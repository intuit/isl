package com.intuit.isl.common

import com.fasterxml.jackson.databind.JsonNode

/**
 * Internal ISL Transform Variable:
 * @property readOnly variable can't be modified - this is a bit subject as ISL can't guarantee this
 * @property global variable is global and will be passed along from function to function
 */
data class TransformVariable(var value: JsonNode?, val readOnly: Boolean = false, val global: Boolean = false);