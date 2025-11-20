package com.intuit.isl.commands

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.parser.tokens.WhileToken
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.JsonConvert

class WhileCommand(
    token: WhileToken,
    private val expression: IEvaluableConditionCommand,
    private val maxLoops: IIslCommand?,
    val statements: IIslCommand
) : BaseCommand(token) {
    companion object {
        const val MAX_LOOPS = 5000;
        const val DEFAULT_LOOPS = 50;
    }

    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        var loops = DEFAULT_LOOPS;
        if (maxLoops != null) {
            val res = maxLoops.executeAsync(executionContext).value as? ObjectNode;
            loops = ConvertUtils.tryParseInt(res?.get("maxLoops"), DEFAULT_LOOPS)!!;
            if (loops > MAX_LOOPS)
                loops = MAX_LOOPS;
        }

        val result = JsonNodeFactory.instance.arrayNode(10);
        while (expression.evaluateConditionAsync(executionContext) && loops > 0) {
            val statementsRes = statements.executeAsync(executionContext)

            if (statementsRes.validResult != false)
                result.add(JsonConvert.convert(statementsRes.value));

            loops -= 1;
        }

        return CommandResult(result, null, true);
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}