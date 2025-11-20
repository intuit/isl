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
        callback: AsyncStatementsExtensionMethod
    ): IOperationContext {
        throw NotImplementedError("Not supported");
    }

    override fun getExtension(name: String): AsyncContextAwareExtensionMethod? {
        return context.getExtension(name);
    }

    override fun getConditionalExtension(name: String): ConditionalExtension? {
        return context.getConditionalExtension(name);
    }

    override fun getAnnotation(annotationName: String): AsyncExtensionAnnotation? {
        return context.getAnnotation(annotationName);
    }

    override fun getStatementExtension(name: String): AsyncStatementsExtensionMethod? {
        return context.getStatementExtension(name);
    }

    override fun setVariable(name: String, node: JsonNode, setIsModified: Boolean?): IOperationContext {
        // @ is used for now for some internal variables just to keep track of internal stuff
        assert(name.startsWith("$"));

        val lname = name.lowercase();
        // if variable exists and it's readonly then don't set
        val existing = variables[lname];
        if(existing?.readOnly == true)
            throw Exception("Could not set readonly variable=${name}.")

        variables[lname] = TransformVariable(node);

        return this;
    }
    override fun setVariable(name: String, variable: TransformVariable): IOperationContext {
        // @ is used for now for some internal variables just to keep track of internal stuff
        assert(name.startsWith("$"));
        val lname = name.lowercase();
        // if variable exists and it's readonly then don't set
        val existing = variables[lname];
        if(existing?.readOnly == true)
            throw Exception("Could not set readonly variable=${name}.")

        variables[lname] = variable;

        return this;
    }


    override fun getVariable(name: String): JsonNode? {
        val variable = variables[name.lowercase()];
        return variable?.value;
    }

    override fun getTransformVariable(name: String): TransformVariable? {
        return variables[name.lowercase()];
    }
    override fun removeVariable(name: String) {
        variables.remove(name.lowercase());
    }

//    override val interceptor: ICommandInterceptor?
//        get() = context.interceptor;
}