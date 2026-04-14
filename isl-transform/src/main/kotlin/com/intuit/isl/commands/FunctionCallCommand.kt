package com.intuit.isl.commands

import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.common.ContextAwareExtensionMethod
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

    internal val callArguments: List<IIslCommand> get() = arguments
    override val token: FunctionCallToken
        get() = super.token as FunctionCallToken;

    override fun execute(executionContext: ExecutionContext): CommandResult {
        var function = executionContext.operationContext.getExtension(token.name);

        if (function == null) { // search for the fallback
            function = executionContext.operationContext.getExtension(Const.FallbackMethodName);
        }

        val args = arguments.map { it.execute(executionContext).value }.toTypedArray();
        if (function == null) {
            val error = "Could not Execute '@.${token.name}'. Error='Unknown Function: ${token.name}'";
            throw TransformException(error, token.position);
        } else {
            val functionContext = FunctionExecuteContext(token.name, this, executionContext, args);
            // Direct sync call - no bridge needed!
            // All extensions are now sync (async ones were wrapped during registration)
            val result = safeRunFunction(token.name, this) { 
                function(functionContext) 
            };
            return CommandResult(result);
        }
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }

    companion object {
        fun safeRunFunction(
            name: String,
            command: IIslCommand,
            func: () -> Any?
        ): Any? {
            try {
                return func();
            } catch (e: TransformException) {
                val thisError = "Could not Execute '@.$name' at ${command.token.position}.\n${e.message}";
                throw TransformException(thisError, command.token.position, e);
            } catch (e: Exception) {
                // e.message is null for NullPointerException; include the top stack frame so the crash site is visible.
                val stackTop = e.stackTrace.firstOrNull()
                    ?.let { " at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }
                    ?: ""
                val error = "${e.javaClass.simpleName}: ${e.message ?: "no message"}$stackTop";
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
 * Now fully sync - all hardwired functions are internal ISL functions
 */
class HardwiredFunctionCallCommand(
    token: FunctionCallToken,
    arguments: List<IIslCommand>,
    initialCallback: ContextAwareExtensionMethod?,
) : FunctionCallCommand(token, arguments) {
    /** Resolved after loading a pre-compiled package via [com.intuit.isl.runtime.TransformPackageBuilder.loadCompiled]. */
    internal var linkedCallback: ContextAwareExtensionMethod? = initialCallback

    override fun execute(executionContext: ExecutionContext): CommandResult {
        val callback = linkedCallback
            ?: throw TransformException(
                "Hardwired function '@.${token.name}' is not linked (pre-compiled load/link step missing).",
                token.position
            )
        val args = arguments.map { it.execute(executionContext).value }.toTypedArray();
        val functionContext = FunctionExecuteContext(token.name, this, executionContext, args);
        val result = safeRunFunction(token.name, this) {
            // Direct sync call - no bridge needed!
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

    internal val statementArguments: List<IIslCommand> get() = arguments
    internal val statementBody: IIslCommand get() = statements
    override val token: FunctionCallToken
        get() = super.token as FunctionCallToken;

    override fun execute(executionContext: ExecutionContext): CommandResult {
        val function = executionContext.operationContext.getStatementExtension(token.name);
        val args = arguments.map { it.execute(executionContext).value }.toTypedArray();

        if (function == null) {
            val error = "Could not Execute '@.${token.name}'. Error='Unknown Function: ${token.name}'";
            throw TransformException(error, token.position);
        } else {
            val functionExecutionContext = FunctionExecuteContext(token.name, this, executionContext, args);
            // Statement extensions are now sync-only
            val result = function(functionExecutionContext) { _ ->
                try {
                    return@function this.statements.execute(executionContext);
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
