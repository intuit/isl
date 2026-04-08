package com.intuit.isl.commands

import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.parser.tokens.IIslToken
import com.intuit.isl.utils.ConvertUtils
import java.util.HashMap

/**
 * O(1) string-key dispatch for switches where every non-else arm is `==` against a string literal.
 * Semantics match [SwitchCaseCommand] for that pattern: [ConvertUtils.tryToString] on the switch
 * value is used as the lookup key (same string-coercion path used by [ConditionEvaluator] for `==`).
 */
class HashDispatchSwitchCommand(
    token: IIslToken,
    val value: IIslCommand,
    private val armsByKey: HashMap<String, IIslCommand>,
    val defaultArm: IIslCommand?
) : BaseCommand(token) {

    internal fun forEachArmCommand(action: (IIslCommand) -> Unit) {
        armsByKey.values.forEach(action)
        defaultArm?.let(action)
    }

    override fun execute(executionContext: ExecutionContext): CommandResult {
        val leftResult = value.execute(executionContext)
        val key = ConvertUtils.tryToString(leftResult.value)
        if (key != null) {
            armsByKey[key]?.let { return it.execute(executionContext) }
        }
        defaultArm?.let { return it.execute(executionContext) }
        return CommandResult(null)
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this)
    }
}
