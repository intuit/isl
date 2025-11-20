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
}