package com.intuit.isl.runtime

import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.common.LocalOperationContext
import com.intuit.isl.parser.tokens.IIslToken
import com.intuit.isl.utils.JsonConvert

class LocalTransformer(val module: TransformModule) : ILocalTransformer {
    val token: IIslToken
        get() = module.token;

    /**
     * Run a specific function in a pre-compiled transformation.
     * You can pass through the @param operationContext
     * and a unique @param carryContext that will be carried across the executions
     * Using a carryContext allows you to pre-register all methods in the operationContext and use
     * the carryContext
     */
    override suspend fun runTransformAsync(
        functionName: String,
        operationContext: LocalOperationContext,
        localContext: Any?
    ): ITransformResult {
        val function = module.getFunction(functionName);

        if (function != null) {
            val context = ExecutionContext(operationContext, localContext);

            // Add Default Variables
            operationContext.setVariable("\$isl", Transformer.getIslInfo());

            val result = function.executeAsync(context);
            return TransformResult(JsonConvert.convert(result.value));
        }

        throw TransformException("Unknown Function @.${module.name}.$functionName", module.token.position);
    }
}