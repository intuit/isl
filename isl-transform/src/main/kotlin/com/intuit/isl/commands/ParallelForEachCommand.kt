package com.intuit.isl.commands

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.common.ParallelOperationContext
import com.intuit.isl.common.setVariableCanonical
import com.intuit.isl.parser.tokens.ParallelForEachToken
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.runtime.Transformer
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.IIslIterable
import com.intuit.isl.utils.JsonConvert
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.ConcurrentHashMap

/**
 * For each that can run in parallel using virtual threads.
 * Uses a semaphore to limit parallelism while maintaining virtual thread benefits.
 */
class ParallelForEachCommand(
    token: ParallelForEachToken,
    private val options: IIslCommand?,
    private val source: IIslCommand,
    statements: IIslCommand
) :
    ForEachCommand(token, source, statements) {

    internal val parallelOptions: IIslCommand? get() = options
    override val token: ParallelForEachToken
        get() = super.token as ParallelForEachToken;

    override fun execute(executionContext: ExecutionContext): CommandResult {
        val sourceCollection = source.execute(executionContext).value;
        // Options can have for now just `workers: X`
        val localOptions = options?.execute(executionContext)?.value as? ObjectNode?;

        val workers = Math.clamp(
            ConvertUtils.tryParseLong(localOptions?.get("workers"), 6) ?: 6,
            1, Transformer.maxParallelWorkers
        ).toInt();

        // we'll run as a normal foreach
        if(workers == 1) {
            return super.execute(executionContext);
        }

        val source = when (sourceCollection) {
            is IIslIterable -> sourceCollection.getInnerIterator();
            is Iterable<Any?> -> sourceCollection
            else -> null;
        };

        val sourceList = source?.toList() ?: emptyList()
        val results = ConcurrentHashMap<Int, CommandResult>()
        val errors = ConcurrentHashMap<Int, Exception>()
        val semaphore = Semaphore(workers)

        // Use virtual thread executor
        val executor = Executors.newVirtualThreadPerTaskExecutor()
        
        try {
            val futures = sourceList.mapIndexed { i, it ->
                executor.submit {
                    semaphore.acquire()
                    try {
                        val localOperationContext = ParallelOperationContext(executionContext.operationContext)
                        val localExecutionContext = ExecutionContext(
                            localOperationContext,
                            executionContext.localContext,
                            executionContext.executionHook,
                            executionContext.coroutineContext
                        )
                        localOperationContext.setVariableCanonical(foreachIteratorKey, JsonConvert.convert(it))
                        localOperationContext.setVariableCanonical(foreachIteratorIndexKey, JsonConvert.convert(i))

                        val itValue = statements.execute(localExecutionContext)
                        results[i] = itValue
                    } catch (e: Exception) {
                        // Collect errors but don't stop other iterations (supervisor behavior)
                        errors[i] = e
                    } finally {
                        semaphore.release()
                    }
                }
            }
            
            // Wait for all futures to complete
            futures.forEach { it.get() }
        } finally {
            executor.shutdown()
        }

        // Build result array from collected results, maintaining order
        val result = JsonNodeFactory.instance.arrayNode()
        
        for (i in sourceList.indices) {
            val r = results[i]
            if (r != null) {
                try {
                    if (r.validResult == false)
                        continue // ignore

                    result.add(JsonConvert.convert(r.value))
                } catch (t: TransformException) {
                    throw t
                } catch (e: Exception) {
                    throw TransformException(e.message + " at ${token.position}", token.position, e)
                }
            } else {
                // Check if there was an error for this index
                val error = errors[i]
                if (error != null) {
                    when (error) {
                        is TransformException -> throw error
                        else -> throw TransformException(
                            error.message + " at ${token.position}",
                            token.position,
                            error
                        )
                    }
                }
            }
        }

        return CommandResult(result, null, true)
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}