package com.intuit.isl.commands.modifiers

import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.commands.BaseCommand
import com.intuit.isl.commands.CommandResult
import com.intuit.isl.commands.FunctionCallCommand
import com.intuit.isl.commands.IIslCommand
import com.intuit.isl.common.AsyncContextAwareExtensionMethod
import com.intuit.isl.parser.tokens.ModifierValueToken

open class ModifierValueCommand(
    token: ModifierValueToken,
    realModifierName: String,
    val value: IIslCommand,
    protected val modifierArguments: List<IIslCommand>
) : BaseCommand(token) {
    override val token: ModifierValueToken
        get() = super.token as ModifierValueToken;

    protected val modifierName: String;
    protected val modifierSelector: String?;

    companion object{
        @JvmStatic
        protected suspend fun internalRunModifier(
            command: ModifierValueCommand,
            executionContext: ExecutionContext,
            prevValue: CommandResult,
            arguments: List<IIslCommand>,
            modifier: AsyncContextAwareExtensionMethod
        ): CommandResult {
            val args = mutableListOf(prevValue.value);
            if (command.modifierSelector != null)
                args.add(command.modifierSelector);
            arguments.mapTo(args) { it.executeAsync(executionContext).value };

            val functionContext = FunctionExecuteContext(command.token.name, command, executionContext, args.toTypedArray());

            val result = FunctionCallCommand.safeRunFunction(command.token.name, command) {
                modifier(functionContext);
            }

            return CommandResult(result);
        }
    }

    init {
        // wildcard separator
        // Performance: pre-lowercase the modifier name here to avoid allocation in hot path
        if (realModifierName.contains(".")) {
            modifierSelector = realModifierName.substringAfter(".");
            modifierName = (realModifierName.substringBefore(".") + ".*").lowercase();
        } else{
            modifierName = realModifierName.lowercase();
            modifierSelector = null;
        }
    }

    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val prevValue = value.executeAsync(executionContext);

        return internalExecuteAsync(prevValue, executionContext);
    }

    protected open suspend fun internalExecuteAsync(
        prevValue: CommandResult,
        executionContext: ExecutionContext
    ): CommandResult {
        // Apply our modifier
        val modifier = executionContext.operationContext.getExtension("modifier.${modifierName}");

        if (modifier == null) {
            val error = "Unknown Modifier: ${token.name}";
            return CommandResult(error);
        } else {
            return internalRunModifier(this, executionContext, prevValue, modifierArguments, modifier)
        }
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}

class HardwiredModifierValueCommand(
    token: ModifierValueToken, realModifierName: String, value: IIslCommand, arguments: List<IIslCommand>,
    private val callback: AsyncContextAwareExtensionMethod
) : ModifierValueCommand(token, realModifierName, value, arguments) {
    override suspend fun internalExecuteAsync(
        prevValue: CommandResult,
        executionContext: ExecutionContext
    ): CommandResult {
        return ModifierValueCommand.internalRunModifier(this, executionContext, prevValue, modifierArguments, callback);
    }
}
