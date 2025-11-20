package com.intuit.isl.commands.modifiers

import com.intuit.isl.commands.BaseCommand
import com.intuit.isl.commands.CommandResult
import com.intuit.isl.commands.IEvaluableConditionCommand
import com.intuit.isl.commands.IIslCommand
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.parser.tokens.GenericConditionalModifierValueToken

interface IConditionalCommand {
    val name: String;
    val value: IIslCommand;
    val expression: IEvaluableConditionCommand;
    val arguments: List<IIslCommand>;
}

/**
 * We can have these potential conditional command when we don't know if the firstParameter is a condition or an argument
 */
class PotentialGenericConditionalModifierCommand(
    override val name: String,
    token: GenericConditionalModifierValueToken,
    value: IIslCommand,
    // conditional modifier | dostuff ( condition, arguments )
    override val expression: IEvaluableConditionCommand,
    override val arguments: List<IIslCommand>,

    // standard modifier | dostuff ( allArguments )
    allArguments: List<IIslCommand>
) : IConditionalCommand, ModifierValueCommand(token, name, value, allArguments) {
    override val token: GenericConditionalModifierValueToken
        get() = super.token as GenericConditionalModifierValueToken;

    // We hit a modifier that has a first param that could be either a condition
    // or an argument - we'll have to wait until execution to see what this is
//    init {
//        println(">>>>>>>>>>>>>>>>>>> got confused by ${name}")
//    }
    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        // WARNING WARNING WARNING: Generic Conditional CAN accidentally pickup normal modifiers
        // so we don't know which type of modifier we picked up
        val standardModifier = executionContext.operationContext.getExtension("modifier.${modifierName}");
        if (standardModifier != null) {
            val prevValue = value.executeAsync(executionContext);
            // standard modifier, where first param is the value
            return internalRunModifier(this, executionContext, prevValue, super.modifierArguments, standardModifier)
        }

        // condition modifier!
        val extension =
            executionContext.operationContext.getConditionalExtension("modifier.${modifierName}")
                ?: executionContext.operationContext.getConditionalExtension("modifier.${name.lowercase()}");
        if (extension == null) {
            // maybe it's a standard modifier
            val error = "Unknown Extension: ${name}";
            return CommandResult(error);
        } else {
            val result = extension.invoke(this, executionContext)
            return CommandResult(result);
        }
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}

class GenericConditionalModifierCommand(
    override val name: String,
    token: GenericConditionalModifierValueToken,
    override val value: IIslCommand,
    override val expression: IEvaluableConditionCommand,
    override val arguments: List<IIslCommand>,
) : IConditionalCommand, BaseCommand(token) {

    override val token: GenericConditionalModifierValueToken
        get() = super.token as GenericConditionalModifierValueToken;


    private val modifierName: String;
    private val modifierSelector: String?;

    init {
        // wildcard separator
        if (name.contains(".")) {
            modifierSelector = name.substringAfter(".");
            modifierName = name.substringBefore(".") + ".*";
        } else {
            modifierName = name;
            modifierSelector = null;
        }
    }

    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val extension =
            executionContext.operationContext.getConditionalExtension("modifier.$modifierName") ?:  // do.*
            executionContext.operationContext.getConditionalExtension("modifier.$name");    // do.stuff
        if (extension == null) {
            // maybe it's a standard modifier
            val error = "Unknown Extension: ${name}";
            return CommandResult(error);
        } else {
            val result = extension.invoke(this, executionContext)
            return CommandResult(result);
        }
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}