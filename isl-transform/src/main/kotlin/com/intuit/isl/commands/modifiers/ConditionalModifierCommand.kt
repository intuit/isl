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
    override fun execute(executionContext: ExecutionContext): CommandResult {
        val hook = executionContext.executionHook
        hook?.onBeforeExecute(this, executionContext)
        val standardModifier = executionContext.operationContext.getExtension("modifier.${modifierName}")
        val result =
            if (standardModifier != null) {
                val prevValue = value.execute(executionContext)
                internalRunModifier(this, executionContext, prevValue, super.modifierArguments, standardModifier)
            } else {
                val extension =
                    executionContext.operationContext.getConditionalExtension("modifier.${modifierName}")
                        ?: executionContext.operationContext.getConditionalExtension("modifier.${name.lowercase()}")
                if (extension == null) {
                    CommandResult("Unknown Extension: ${name}")
                } else {
                    // Direct sync call - conditional extensions are now sync-only
                    CommandResult(extension.invoke(this, executionContext))
                }
            }
        hook?.onAfterExecute(this, executionContext, result)
        return result
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

    override fun execute(executionContext: ExecutionContext): CommandResult {
        val hook = executionContext.executionHook
        hook?.onBeforeExecute(this, executionContext)
        val extension =
            executionContext.operationContext.getConditionalExtension("modifier.$modifierName")
                ?: executionContext.operationContext.getConditionalExtension("modifier.$name")
        val result =
            if (extension == null) {
                CommandResult("Unknown Extension: ${name}")
            } else {
                // Direct sync call - conditional extensions are now sync-only
                CommandResult(extension.invoke(this, executionContext))
            }
        hook?.onAfterExecute(this, executionContext, result)
        return result
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}