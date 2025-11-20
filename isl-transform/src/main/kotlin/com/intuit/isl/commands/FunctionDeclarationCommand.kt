package com.intuit.isl.commands

import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.common.AsyncContextAwareExtensionMethod
import com.intuit.isl.common.BaseOperationContext
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.common.LocalOperationContext
import com.intuit.isl.parser.tokens.FunctionDeclarationToken
import com.intuit.isl.parser.tokens.FunctionReturnToken
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.runtime.TransformModule
import com.intuit.isl.types.TypedObjectNode
import com.intuit.isl.utils.JsonConvert
import java.util.*
import kotlin.math.min


interface IFunctionDeclarationCommand : IIslCommand {
    val name: String;
    override val token: FunctionDeclarationToken;
    val statements: IIslCommand;

    fun getRunner(): AsyncContextAwareExtensionMethod;
}

class FunctionDeclarationCommand(token: FunctionDeclarationToken, override val statements: IIslCommand) : BaseCommand(token),
    IFunctionDeclarationCommand {

    internal lateinit var module: TransformModule;

    override val name: String
        get() = token.functionName;

    override val token: FunctionDeclarationToken
        get() = super.token as FunctionDeclarationToken;

    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        // execute annotations or function
        return statements.executeAsync(executionContext);
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }

    override fun getRunner(): AsyncContextAwareExtensionMethod {
        val sameModuleRunner: AsyncContextAwareExtensionMethod = declaration@{ functionContext ->
            // we are called from outside (e.g. from another function instead of from the main runtime
            // this means we need to create a true-callstack and the isolation that comes with that
            // clone the context, by keeping only the registered functions
            // TBD: What do we do with cross-module calls - as we might have in the context some @.This left from the previous module
            // this is becoming more complicated when we have two imports that carry each other
            // e.g. import Math from 'math'; import String from 'string';
            // @.String.DoOperation ( .. ) and inside that we call "@.Math" - but Math was never imported in String only in the parent
            val childContext =
                if (functionContext.executionContext.operationContext is BaseOperationContext)
                    functionContext.executionContext.operationContext.createFunctionChildContext(module.functionExtensions);
                else if (functionContext.executionContext.operationContext is LocalOperationContext)
                    LocalOperationContext(functionContext.executionContext.operationContext.context)
                else
                    throw TransformException(
                        "Could not execute ${functionContext.functionName} due to unknown context type=${functionContext.executionContext.operationContext::class.java.name}.",
                        this.token.position
                    )
            //val childContext = existingContext.createFunctionChildContext(module.functionExtensions);

            // push the parameters! We could also add support for default parameter values I guess :)
            val maxArguments = min(functionContext.parameters.size, token.arguments.size) - 1;

            // we can only have as many arguments as the minimum between received and declared
            for (i in 0..maxArguments) {
                val name = token.arguments[i];
                val value = functionContext.parameters[i];
                childContext.setVariable(name.name, JsonConvert.convert(value));
            }

            val childExecutionContext = ExecutionContext(childContext, functionContext.executionContext.localContext);

            val result = executeAsync(childExecutionContext);
            return@declaration result.value;
        }
        return sameModuleRunner;
    }
}

class FunctionReturnCommandHandler(token: FunctionDeclarationToken, val statements: IIslCommand) : BaseCommand(token) {
    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val result = try {
            // Execute statements
            statements.executeAsync(executionContext)
        } catch (e: FunctionReturnCommand.FunctionReturnException) {
            // function returned
            CommandResult(e.returnValue);
        }
        if (token.islType != null) {
            val value = result.value as? ObjectNode?;
            if (value is TypedObjectNode) {
                value.type = token.islType;
                return result;
            } else if(value is ObjectNode){
                // somehow we got back an object but this has a declaration
                // happens when we get results back via ApiCalls for example
                val newResult = TypedObjectNode.tryTypedObject(token.islType, value)
                return CommandResult(newResult, result.propertyName, result.append, result.validResult);
            }
        }
        return result;
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        throw NotImplementedError();
    }
}

/**
 * Note, a function can have multiple return statements - first hit is the lucky winner.
 */
class FunctionReturnCommand(token: FunctionReturnToken, private val returnValue: IIslCommand) : BaseCommand(token) {
    var useReturnValue: Boolean = false;

    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        // Now, there are 100 ways to skin a cat.
        // Normally we should have some pro-active return statement that jumps over all other instructions
        // but that's rather hard in a graph command - so we'll cheat for now (and optimize later) by using
        // a custom exception to jump from here back to the Function declaration

        // just throw this ;) The Function will call us and evaluate us at that point in time
        // Not sure if it's worth evaluating the value of our token here or when we are in the parent function.
        //throw this;

        // I'm walking back on my prev statement - we should evaluate the result value here!
        // that means that if we DO (in the future) support some sort of finally or try/catch
        // we can evaluate and capture the result before we evaluate the try/catch
        val returnedValue = returnValue.executeAsync(executionContext);

        // we need to get out the null returns as an exception to avoid it being caught in a property name build null check
        if (useReturnValue && returnedValue.value != null)
            return returnedValue;
        throw FunctionReturnException(returnedValue.value);
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }

    class FunctionReturnException(val returnValue: Any?) : Exception("FunctionReturning") {
    }
}


/** Used when we find a recursive entry in a function
 * It will resolve the recursion a bit later
 */
class RecursiveFunctionDeclarationCommand(
    token: FunctionDeclarationToken,
    private val compiledFunctions: TreeMap<String, IFunctionDeclarationCommand>
) : BaseCommand(token), IFunctionDeclarationCommand{
    override val name: String
        get() = token.functionName;

    override val token: FunctionDeclarationToken
        get() = super.token as FunctionDeclarationToken;

    override val statements: IIslCommand
        get() = compiledFunctions[name]!!.statements;

    private var recursiveRunner: AsyncContextAwareExtensionMethod? = null;
    override fun getRunner(): AsyncContextAwareExtensionMethod {
        return declaration@{ functionContext ->
            // find the real runner of the real function
            recursiveRunner = recursiveRunner ?: compiledFunctions[name]!!.getRunner();
            recursiveRunner!!.invoke(functionContext);
        }
    }

    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        TODO("Not yet implemented")
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        TODO("Not yet implemented")
    }
}


