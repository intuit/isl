package com.intuit.isl.commands

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.common.ParallelOperationContext
import com.intuit.isl.parser.tokens.ParallelForEachToken
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.runtime.Transformer
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.IIslIterable
import com.intuit.isl.utils.JsonConvert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import java.util.concurrent.Executors
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

/**
 * For each that can run in parallel.
 */
class ParallelForEachCommand(
    token: ParallelForEachToken,
    private val options: IIslCommand?,
    private val source: IIslCommand,
    statements: IIslCommand
) :
    ForEachCommand(token, source, statements) {
    override val token: ParallelForEachToken
        get() = super.token as ParallelForEachToken;

    companion object {
        private val localDispatcher = Executors.newFixedThreadPool(20).asCoroutineDispatcher()
    }

    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val sourceCollection = source.executeAsync(executionContext).value;
        // Options can have for now just `worders: X`
        val localOptions = options?.executeAsync(executionContext)?.value as? ObjectNode?;

        val workers = Math.clamp(
            ConvertUtils.tryParseLong(localOptions?.get("workers"), 6) ?: 6,
            1, Transformer.maxParallelWorkers
        );

        // we'll run as a normal foreach
        if(workers == 1) {
            return super.executeAsync(executionContext);
        }

        val source = when (sourceCollection) {
            is IIslIterable -> sourceCollection.getInnerIterator();
            is Iterable<Any?> -> sourceCollection
            else -> null;
        };

        val supervisorJob = SupervisorJob()
        val limitedDispatcher = localDispatcher.limitedParallelism(workers)

        val waits = source?.mapIndexed { i, it ->
            CoroutineScope(limitedDispatcher + coroutineContext + supervisorJob).async {
                val localOperationContext = ParallelOperationContext(executionContext.operationContext);
                val localExecutionContext = ExecutionContext(localOperationContext, executionContext.localContext);
                localOperationContext.setVariable(token.iterator, JsonConvert.convert(it));
                localOperationContext.setVariable(token.iterator + "index", JsonConvert.convert(i));

                val itValue = statements.executeAsync(localExecutionContext);
                return@async itValue;
            }
        };
        waits?.joinAll();

        val result = JsonNodeFactory.instance.arrayNode();
        waits?.forEach {
            try {
                val r = it.await();
                if (r.validResult == false)
                    return@forEach; // ignore

                result.add(JsonConvert.convert(r.value));
            } catch (t: TransformException) {
                throw t;
            } catch (e: Exception) {
                throw TransformException(e.message + " at ${token.position}", token.position, e);
            }
        }

        return CommandResult(result, null, true);
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}