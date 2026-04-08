package com.intuit.isl.common

import com.fasterxml.jackson.databind.JsonNode

/**
 * Specialized LocalOperationContext that can reuse an existing OperationContext
 * In order to reduce the need for expensive OperationContext creation.
 */
class LocalOperationContext(
    val context: IOperationContext
) : IOperationContext {
    override val variables: HashMap<String, TransformVariable> = HashMap()

    override fun registerExtensionMethod(
        fullName: String,
        callback: AsyncContextAwareExtensionMethod
    ): IOperationContext {
        throw NotImplementedError("Not supported");
    }

    override fun registerSyncExtensionMethod(
        fullName: String,
        callback: ContextAwareExtensionMethod
    ): IOperationContext {
        throw NotImplementedError("Not supported")
    }

    override fun registerConditionalExtensionMethod(
        fullName: String,
        extension: ConditionalExtension
    ): IOperationContext {
        throw NotImplementedError("Not supported");
    }

    override fun registerAnnotation(annotationName: String, callback: AsyncExtensionAnnotation): IOperationContext {
        throw NotImplementedError("Not supported");
    }

    override fun registerStatementMethod(
        fullName: String,
        callback: StatementsExtensionMethod
    ): IOperationContext {
        throw NotImplementedError("Not supported");
    }

    override fun getExtension(name: String): ContextAwareExtensionMethod? {
        return context.getExtension(name);
    }

    override fun getConditionalExtension(name: String): ConditionalExtension? {
        return context.getConditionalExtension(name);
    }

    override fun getAnnotation(annotationName: String): AsyncExtensionAnnotation? {
        return context.getAnnotation(annotationName);
    }

    override fun getStatementExtension(name: String): StatementsExtensionMethod? {
        return context.getStatementExtension(name);
    }

    override fun setVariable(name: String, node: JsonNode, setIsModified: Boolean?): IOperationContext {
        assert(name.startsWith("$"));
        setVariableCanonical(name.lowercase(), node, setIsModified);
        return this;
    }
    override fun setVariable(name: String, variable: TransformVariable): IOperationContext {
        assert(name.startsWith("$"));
        setTransformVariableCanonical(name.lowercase(), variable);
        return this;
    }


    override fun getVariable(name: String): JsonNode? {
        return getVariableCanonical(name.lowercase());
    }

    override fun getTransformVariable(name: String): TransformVariable? {
        return getTransformVariableCanonical(name.lowercase());
    }
    override fun removeVariable(name: String) {
        removeVariableCanonical(name.lowercase());
    }

//    override val interceptor: ICommandInterceptor?
//        get() = context.interceptor;
}