package com.intuit.isl.commands.modifiers

import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.commands.CommandResult
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.JsonConvert
import kotlinx.coroutines.delay
import kotlin.random.Random

object RetryModifiers {
    fun registerRetry(context: IOperationContext) {

        // retries & conditional retries
        context.registerConditionalExtensionMethod("modifier.retry.*", RetryModifiers::retryWhen)
    }

    // | retry.when( $ condition, { retryCount: 3, backOff: 2 } )
    private suspend fun retryWhen(command: IConditionalCommand, context: ExecutionContext): Any? {
        val options = command.arguments.getOrNull(0)
            ?.executeAsync(context)
            ?.value as? ObjectNode;

        val retryCount = (ConvertUtils.tryParseInt(options?.get("retryCount")) ?: 3);
        val backOff = options?.get("backOff")?.booleanValue() == true;
        var delayTime: Long = (ConvertUtils.tryParseLong(options?.get("delay"))
            ?: Random.nextLong(
                ConvertUtils.tryParseLong(options?.get("delayFrom")) ?: 50,
                ConvertUtils.tryParseLong(options?.get("delayTo")) ?: if(backOff) 100 else 500
            )
        );

        var lastResult: CommandResult?;
        var i = 0;
        do {
            i++

            try {
                val result = command.value.executeAsync(context);
                lastResult = result;
                val value = JsonConvert.convert(result.value);

                context.operationContext.setVariable("$", value);
                if (command.expression.evaluateConditionAsync(context)) {
                    context.operationContext.removeVariable("$");
                    if (delayTime > 0)
                        delay(delayTime)
                    if(backOff) // next time go slower
                        delayTime *= 2;
                    continue;   // try again
                } else {
                    return result.value;
                }
            } finally {
                context.operationContext.removeVariable("$");
            }
        } while (i <= retryCount);

        // retry count failed? what now? fail or return last result?
        return lastResult?.value;
    }
}