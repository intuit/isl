package com.intuit.isl.runtime

import com.fasterxml.jackson.databind.JsonNode

/**
 * Result of a transformation. For now we just carry the actual payload but most probably we'll want more stuff in the future.
 */
interface ITransformResult {
    val result: JsonNode?;
}

class TransformResult(override val result: JsonNode?) : ITransformResult {
}
