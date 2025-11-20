package com.intuit.isl.validation

import com.fasterxml.jackson.databind.JsonNode
import com.intuit.isl.common.OperationContext
import com.intuit.isl.runtime.ITransformer
import com.intuit.isl.runtime.TransformCompiler
import com.intuit.isl.utils.ConvertUtils
import com.networknt.schema.*
import kotlinx.coroutines.runBlocking

// Custom validator allowing for advanced ISL scripts for schema validation
// https://github.com/networknt/json-schema-validator/blob/master/doc/custom-meta-schema.md

class IslAdvancedValidatorKeyword : Keyword {
    companion object{
        const val NAME = "validate";
    }
    override fun getValue(): String {
        return NAME
    }

    override fun newValidator(
        schemaLocation: SchemaLocation?,
        evaluationPath: JsonNodePath?,
        schemaNode: JsonNode,
        parentSchema: JsonSchema?,
        validationContext: ValidationContext?
    ): JsonValidator {
        val script = schemaNode.textValue();

        val fullScript = "fun run( \$field, \$entity ){\r\n$script\r\n}";

        val compiledValidator = TransformCompiler().compileIsl("Validator:$schemaNode", fullScript);

        return IslAdvancedValidation(
            schemaLocation,
            evaluationPath,
            schemaNode,
            parentSchema,
            this,
            validationContext,
            false,
            compiledValidator
        )
    }
}

class IslAdvancedValidation(
    schemaLocation: SchemaLocation?,
    evaluationPath: JsonNodePath?,
    schemaNode: JsonNode?,
    parentSchema: JsonSchema?,
    keyword: Keyword,
    validationContext: ValidationContext?,
    suppressSubSchemaRetrieval: Boolean,
    private val compiledValidator: ITransformer,
) : BaseJsonValidator(
    schemaLocation,
    evaluationPath,
    schemaNode,
    parentSchema,
    ERROR_MESSAGE_TYPE,
    keyword,
    validationContext,
    suppressSubSchemaRetrieval
) {
    companion object {
        private val ERROR_MESSAGE_TYPE = ErrorMessageType { IslAdvancedValidatorKeyword.NAME }
    }

    override fun validate(
        p0: ExecutionContext?,
        node: JsonNode,
        entity: JsonNode,
        instanceLocation: JsonNodePath?
    ): MutableSet<ValidationMessage> {
        val validationMessages = HashSet<ValidationMessage>();

        val context = OperationContext()
            .registerExtensionMethod("Validation.Error") {
                val code = ConvertUtils.Companion.tryToString(it.firstParameter)
                val message = ConvertUtils.Companion.tryToString(it.secondParameter);
                val expected = ConvertUtils.Companion.tryToString(it.thirdParameter);
                validationMessages.add(ValidationMessage.builder()
                    .type("advanced")
                    .code(code)
                    .message(message)
                    .instanceLocation(instanceLocation)
                    .instanceNode(node)
                    .details(mapOf("expected" to expected))
                    //.property()
                    .build()
                )
                return@registerExtensionMethod "";
            }
            .setVariable("\$field", node)
            .setVariable("\$entity", entity);

        runBlocking {
            compiledValidator.runTransformAsync("run", context)
        }
        return validationMessages;
    }
}