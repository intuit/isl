package com.intuit.isl.commands

import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.common.AsyncContextAwareExtensionMethod
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.parser.tokens.FunctionCallToken
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.utils.Const

/**
 * Implementation of a function call @.Service.Call( parameters )
 * This works for any internal and external function call.
 * There is a Hardwired version of this that already resolved the target function in a target module.
 */
open class FunctionCallCommand(token: FunctionCallToken, protected val arguments: List<IIslCommand>) :
    BaseCommand(token) {
    override val token: FunctionCallToken
        get() = super.token as FunctionCallToken;

    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        var function = executionContext.operationContext.getExtension(token.name);

        if (function == null) { // search for the fallback
            function = executionContext.operationContext.getExtension(Const.FallbackMethodName);
        }

        val args = arguments.map { it.executeAsync(executionContext).value }.toTypedArray();
        if (function == null) {
            val error = "Could not Execute '@.${token.name}'. Error='Unknown Function: ${token.name}'";
            throw TransformException(error, token.position);
        } else {
            val functionContext = FunctionExecuteContext(token.name, this, executionContext, args);
            val result = safeRunFunction(token.name, this) { function(functionContext) };
            return CommandResult(result);
        }
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }

    companion object {
        suspend fun safeRunFunction(
            name: String,
            command: IIslCommand,
            func: suspend () -> Any?
        ): Any? {
            try {
                return func();
            } catch (e: TransformException) {
                val thisError = "Could not Execute '@.$name' at ${command.token.position}.\n${e.message}";
                throw TransformException(thisError, command.token.position, e.cause);
            } catch (e: Exception) {
                val error = "${e.javaClass.simpleName}: ${e.message}";
                throw TransformException(
                    "Could not Execute '@.$name'. Error='${error}' at ${command.token.position}.",
                    command.token.position,
                    e
                );
            }
        }
    }
}

/**
 * Hardwired function call (e.g. calls across modules can be hardwired)
 */
class HardwiredFunctionCallCommand(
    token: FunctionCallToken,
    arguments: List<IIslCommand>,
    private val callback: AsyncContextAwareExtensionMethod
) : FunctionCallCommand(token, arguments) {
    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val args = arguments.map { it.executeAsync(executionContext).value }.toTypedArray();
        val functionContext = FunctionExecuteContext(token.name, this, executionContext, args);
        val result = safeRunFunction(token.name, this) {
            callback(functionContext)
        };
        return CommandResult(result);
    }
}

open class StatementFunctionCallCommand(
    token: FunctionCallToken,
    protected val arguments: List<IIslCommand>,
    protected val statements: IIslCommand
) : BaseCommand(token) {
    override val token: FunctionCallToken
        get() = super.token as FunctionCallToken;

    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val function = executionContext.operationContext.getStatementExtension(token.name);
        val args = arguments.map { it.executeAsync(executionContext).value }.toTypedArray();

        if (function == null) {
            val error = "Could not Execute '@.${token.name}'. Error='Unknown Function: ${token.name}'";
            throw TransformException(error, token.position);
        } else {
            val functionExecutionContext = FunctionExecuteContext(token.name, this, executionContext, args);
            // note - that function calls could have internal statements - than will be executed on demand
            // e.g. @.Pagination..() { } can execute an internal statement
            val result = function(functionExecutionContext) { _ ->
                try {
                    return@function this.statements.executeAsync(executionContext);
                } catch (e: FunctionReturnCommand.FunctionReturnException) {
                    // function returned
                    CommandResult(e.returnValue);
                }
            };
            return CommandResult(result);
        }
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}
