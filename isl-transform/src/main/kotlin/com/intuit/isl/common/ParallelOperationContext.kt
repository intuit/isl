package com.intuit.isl.common

import com.fasterxml.jackson.databind.JsonNode

/**
 * Specialized Parallel OperationContext that can reuse an existing OperationContext
 * But safely protect all variables inside it.
 * This will READ from the original context but not WRITE in the original context
 */
class ParallelOperationContext(
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
        return setVariableCanonical(name.lowercase(), node, setIsModified);
    }

    override fun setVariable(name: String, variable: TransformVariable): IOperationContext {
//        // @ is used for now for some internal variables just to keep track of internal stuff
//        assert(name.startsWith("$"));
//        val lname = name.lowercase();
//        // if variable exists and it's readonly then don't set
//        val existing = variables[lname];
//        if(existing?.readOnly == true)
//            throw Exception("Could not set readonly variable=${name}.")
//
//        variables[lname] = variable;
//
//        return this;
        throw NotImplementedError("Not supported");
    }


    override fun getVariable(name: String): JsonNode? {
        val lname = name.lowercase();
        return variables[lname]?.value ?: context.getVariableCanonical(lname);
    }

    override fun getTransformVariable(name: String): TransformVariable? {
        val lname = name.lowercase();
        return variables[lname] ?: context.getTransformVariableCanonical(lname);
    }

    override fun removeVariable(name: String) {
        removeVariableCanonical(name.lowercase());
    }

//    override val interceptor: ICommandInterceptor?
//        get() = context.interceptor;
}