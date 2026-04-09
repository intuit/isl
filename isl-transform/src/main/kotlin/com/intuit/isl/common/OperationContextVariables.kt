package com.intuit.isl.common

import com.fasterxml.jackson.databind.JsonNode

/**
 * Variable keys in [IOperationContext.variables] are always lowercased.
 * These helpers skip [String.lowercase] and must only be called with names that are already canonical.
 * Hosts and other external callers should use [IOperationContext.getVariable], [setVariable], etc.
 */

internal fun IOperationContext.getVariableCanonical(name: String): JsonNode? = when (this) {
    is BaseOperationContext -> variables[name]?.value
    is LocalOperationContext -> variables[name]?.value
    is ParallelOperationContext -> variables[name]?.value ?: context.getVariableCanonical(name)
    else -> getVariable(name)
}

internal fun IOperationContext.getTransformVariableCanonical(name: String): TransformVariable? = when (this) {
    is BaseOperationContext -> variables[name]
    is LocalOperationContext -> variables[name]
    is ParallelOperationContext -> variables[name] ?: context.getTransformVariableCanonical(name)
    else -> getTransformVariable(name)
}

internal fun IOperationContext.setVariableCanonical(
    name: String,
    node: JsonNode,
    setIsModified: Boolean? = null
): IOperationContext {
    assert(name.startsWith("$"))
    when (this) {
        is BaseOperationContext -> {
            val existing = variables[name]
            if (existing?.readOnly == true)
                throw Exception("Could not set readonly variable=$name.")
            variables[name] = TransformVariable(node)
            return this
        }
        is LocalOperationContext -> {
            val existing = variables[name]
            if (existing?.readOnly == true)
                throw Exception("Could not set readonly variable=$name.")
            variables[name] = TransformVariable(node)
            return this
        }
        is ParallelOperationContext -> {
            val existing = variables[name]
            if (existing?.readOnly == true)
                throw Exception("Could not set readonly variable=$name.")
            if (context.getVariableCanonical(name) != null)
                throw Exception("Could not set readonly outside scope variable=$name.")
            variables[name] = TransformVariable(node)
            return this
        }
        else -> return setVariable(name, node, setIsModified)
    }
}

internal fun IOperationContext.setTransformVariableCanonical(name: String, variable: TransformVariable): IOperationContext {
    assert(name.startsWith("$"))
    when (this) {
        is BaseOperationContext -> {
            val existing = variables[name]
            if (existing?.readOnly == true)
                throw Exception("Could not set readonly variable=$name.")
            variables[name] = variable
            return this
        }
        is LocalOperationContext -> {
            val existing = variables[name]
            if (existing?.readOnly == true)
                throw Exception("Could not set readonly variable=$name.")
            variables[name] = variable
            return this
        }
        is ParallelOperationContext -> {
            val existing = variables[name]
            if (existing?.readOnly == true)
                throw Exception("Could not set readonly variable=$name.")
            if (context.getVariableCanonical(name) != null)
                throw Exception("Could not set readonly outside scope variable=$name.")
            variables[name] = variable
            return this
        }
        else -> return setVariable(name, variable)
    }
}

internal fun IOperationContext.removeVariableCanonical(name: String) {
    when (this) {
        is BaseOperationContext -> variables.remove(name)
        is LocalOperationContext -> variables.remove(name)
        is ParallelOperationContext -> variables.remove(name)
        else -> removeVariable(name)
    }
}

internal fun IOperationContext.resetVariableCanonical(name: String, node: JsonNode?) {
    if (node == null) removeVariableCanonical(name)
    else setVariableCanonical(name, node, false)
}
