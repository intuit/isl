package com.intuit.isl.validation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.networknt.schema.*

// This is a custom processor to allow us to add post processing based on schema properties
class IslCustomProcessorKeyword(
    private val keyword: String,
    private val processor: SchemaKeywordProcessor
) : Keyword {
    override fun getValue(): String {
        return keyword
    }

    override fun newValidator(
        schemaLocation: SchemaLocation?,
        evaluationPath: JsonNodePath?,
        schemaNode: JsonNode,
        parentSchema: JsonSchema?,
        validationContext: ValidationContext?
    ): JsonValidator {
        return IslCustomProcessor(
            schemaLocation,
            evaluationPath,
            schemaNode,
            parentSchema,
            this,
            validationContext,
            false,
            processor
        )
    }
}

class IslCustomProcessor(
    schemaLocation: SchemaLocation?,
    evaluationPath: JsonNodePath?,
    schemaNode: JsonNode?,
    parentSchema: JsonSchema?,
    keyword: Keyword,
    validationContext: ValidationContext?,
    suppressSubSchemaRetrieval: Boolean,
    private val processor: SchemaKeywordProcessor,
) : BaseJsonValidator(
    schemaLocation,
    evaluationPath,
    schemaNode,
    parentSchema,
    ErrorMessageType { keyword.value },
    keyword,
    validationContext,
    suppressSubSchemaRetrieval
) {
    override fun validate(
        p0: ExecutionContext?,
        node: JsonNode,
        entity: JsonNode,
        instanceLocation: JsonNodePath?
    ): MutableSet<ValidationMessage> {
        val result = processor(node);
        if (!result.second.isNullOrEmpty()) {
            return hashSetOf(*result.second!!)
        } else if (result.first != null && result.first != node) {
            // Processor is trying to replace the current node :)
            if (entity is ObjectNode) {
                entity.fields().forEach { it ->
                    if (it.value == node) {
                        entity.replace(it.key, result.first)
                    }
                }
            }// we don't support array value replacements yet
        }
        return HashSet()
    }
}