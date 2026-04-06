package com.intuit.isl.commands

import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.commands.modifiers.GenericConditionalModifierCommand
import com.intuit.isl.commands.modifiers.ModifierValueCommand
import com.intuit.isl.parser.tokens.IIslToken

// TODO: Maybe split this into two classes, one for left conditions only and one for right conditions
class ConditionCommand(
    token: IIslToken,
    private val expression: IEvaluableConditionCommand,
    private val trueResult: IIslCommand,
    private val falseResult: IIslCommand?
) : BaseCommand(token) {

    internal val branchCondition: IEvaluableConditionCommand get() = expression
    internal val trueBranch: IIslCommand get() = trueResult
    internal val falseBranch: IIslCommand? get() = falseResult
    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        executionContext.executionHook?.onBeforeExecute(this, executionContext)
        if (expression.evaluateConditionAsync(executionContext)) {
            hookBranchForCoverage(executionContext, trueResult)
            val result = trueResult.executeAsync(executionContext)
            return CommandResult(result.value, null, true);
        } else {
            if (falseResult == null) {
                // there is no else branch! don't attempt to append the property at all!
                return CommandResult(null, null, false);
            } else {
                hookBranchForCoverage(executionContext, falseResult)
                val result = falseResult.executeAsync(executionContext)
                return CommandResult(result.value, null, true);
            }
        }
    }

    /**
     * Object/statement builders call the hook on each child; literals and similar leaves do not.
     * Record coverage for the chosen branch root. Skip modifier/condition commands that already invoke the hook
     * at the start of their own executeAsync so hits are not double-counted.
     */
    private suspend fun hookBranchForCoverage(executionContext: ExecutionContext, branch: IIslCommand) {
        val hook = executionContext.executionHook ?: return
        when (branch) {
            is ModifierValueCommand, is ConditionCommand, is GenericConditionalModifierCommand -> Unit
            else -> hook.onBeforeExecute(branch, executionContext)
        }
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }


}