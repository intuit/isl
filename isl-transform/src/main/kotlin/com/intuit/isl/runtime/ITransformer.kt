package com.intuit.isl.runtime

import com.fasterxml.jackson.databind.JsonNode
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.common.LocalOperationContext

interface ITransformer {
    val module: TransformModule

    /**
     * Run a transformation. Give it a function name (as all transformations are in functions).
     * If there are no functions (e.g. old .xform) would use "run" as that's the default.
     *
     * @param functionName Name of the function to be executed.
     * @param operationContext Full context of the operation including any registered extensions and annotations.
     * You should try to cache this and reuse. If you need to use values from the local context you can use the @param localContext for that.
     */
    suspend fun runTransformAsync(
        functionName: String = "run",
        operationContext: IOperationContext
    ): ITransformResult;

    /**
     * Run a transformation synchronously. This can be used from Java though the preference is to run it
     * async from Kotlin.
     */
    fun runTransformSync(functionName: String = "run", operationContext: IOperationContext): JsonNode?;
}

interface ILocalTransformer {
    /**
     * Run a transformation. Give it a function name (as all transformations are in functions).
     * If there are no functions (e.g. old .xform) would use "run" as that's the default.
     *
     * @param functionName Name of the function to be executed.
     * @param operationContext Full context of the operation including any registered extensions and annotations.
     * You should try to cache this and reuse. If you need to use values from the local context you can use the @param localContext for that.
     * @param localContext A local context specific to the operation.
     */
    suspend fun runTransformAsync(
        functionName: String = "run",
        operationContext: LocalOperationContext,
        localContext: Any? = null
    ): ITransformResult;
}
