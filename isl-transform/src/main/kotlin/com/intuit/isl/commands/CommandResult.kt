package com.intuit.isl.commands

import com.fasterxml.jackson.databind.node.MissingNode
import com.fasterxml.jackson.databind.node.NullNode

data class CommandResult(
    private val originalValue: Any?,
    val propertyName: String? = null,
    val append: Boolean? = null,
    val validResult: Boolean? = null
) {
    val value: Any?
        get() {
            if (originalValue is NullNode || originalValue is MissingNode)
                return null;
            return originalValue;
        }

    companion object {
        /** Plain null value; default property/append/valid flags (e.g. no-op, missing path). */
        val NULL = CommandResult(null)

        /**
         * Object-build condition with no else: skip appending the property ([ConditionCommand]).
         * Distinct from [NULL_NOT_VALID] (uses `append = false`, not `validResult`).
         */
        val NULL_APPEND_FALSE = CommandResult(null, null, false)

        /** Statement block ended with no captured result ([StatementsBuildCommand]). */
        val NULL_NOT_VALID = CommandResult(null, validResult = false)
    }
}