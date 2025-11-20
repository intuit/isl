package com.intuit.isl.validation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.intuit.isl.dsl.node
import com.intuit.isl.types.TypedObjectNode
import com.intuit.isl.utils.JsonConvert
import com.networknt.schema.*
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap


// Made this an object in case we want to add properties to it later
data class IslSchema(val contents: String)
typealias AsyncRetrieveSchema = suspend (schemaName: String?, schemaRef: String?) -> IslSchema?;
typealias SchemaKeywordProcessor = (node: JsonNode) -> Pair<JsonNode?, Array<ValidationMessage>?>;

data class IslSchemaProcessingOptions(
    val basicSchemaValidation: Boolean = true,
    val advancedSchemaValidation: Boolean = true,
    val keywordProcessors: Map<String, SchemaKeywordProcessor>? = null
)

class IslSchemaProcessor(
    private val schemaProcessorOptions: IslSchemaProcessingOptions,
    private val retrieveSchema: AsyncRetrieveSchema
) {
    private val schemaCache = ConcurrentHashMap<String, JsonSchema>()

    private val jsonSchemaLoader = lazy {
        // https://github.com/networknt/json-schema-validator
        // val config = SchemaValidatorsConfig.builder().build()
        val metaBuilder = JsonMetaSchema.builder(JsonMetaSchema.getV202012());
        if (schemaProcessorOptions.advancedSchemaValidation)
            metaBuilder.keyword(IslAdvancedValidatorKeyword())
        schemaProcessorOptions.keywordProcessors?.forEach{
            metaBuilder.keyword(IslCustomProcessorKeyword(it.key, it.value))
        }

        // CT: None of our models in idx interfaces has a schema defined
        // Let's default to 2020-12 - IDK why
        JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012) { builder ->
            builder.schemaLoaders {
                it.schemas { schema ->
                    val foundSchema = runBlocking {
                        retrieveSchema(null, schema)
                    };
                    return@schemas foundSchema?.contents;
                }
            }
            builder.metaSchema(metaBuilder.build())
        }
    }


    private suspend fun getSchema(schemaName: String?, schemaRef: String?): JsonSchema? {
        val expectedName = if (!schemaName.isNullOrBlank()) {
            "name:$schemaName"
        } else if (!schemaRef.isNullOrBlank())
            "ref:$schemaRef"
        else
            return null;
        val existingSchema = schemaCache[expectedName];
        if (existingSchema == null) {
            val rawSchema = retrieveSchema(schemaName, schemaRef);
            if (!rawSchema?.contents.isNullOrBlank()) {
                val format = if (rawSchema!!.contents.startsWith("{")) InputFormat.JSON else InputFormat.YAML;
                val newSchema = jsonSchemaLoader.value.getSchema(rawSchema!!.contents, format);
                schemaCache[expectedName] = newSchema;
                return newSchema;
            }
            return null;
        }
        return existingSchema;
    }

    suspend fun processObject(
        value: JsonNode,
        options: JsonNode? = null
    ): JsonNode {
        val result = node {
            put("success", true)
        };

        // we can have two ways of validating
        // value has a type on it already
        // or we've passed a specific type/schema
        val typedValue = value as? TypedObjectNode?;
        val (schemaName, schemaUrl) = if (typedValue?.type?.schemaName != null || typedValue?.type?.schemaUrl != null) {
            listOf(typedValue.type?.schemaName, typedValue.type?.schemaUrl)
        } else if (options?.get("schemaName")?.textValue() != null || options?.get("schemaUrl")?.textValue() != null) {
            listOf(options.get("schemaName")?.textValue(), options.get("schemaUrl")?.textValue())
        } else {
            result.put("warning", "Could not detect expected schema.")
            return result.node;
        }

        val schema = getSchema(schemaName, schemaUrl);
        if (schema == null) {
            result.put("warning", "Could not find schema $schemaName/$schemaUrl.")
            return result.node;
        }
        val results = mutableListOf<IslValidationResult>();

        schemaValidate(value, schema, results);

        if (results.size > 0) {
            result.put("success", false);
            result.array("issues", JsonConvert.convert(results) as ArrayNode)
        }
        return result.node;
    }

    /**
     * This will validate the Type as per the declaration that was done in the ISL
     * $account: idx.banking.account = { id ... }
     */
    suspend fun validateType(
        value: JsonNode,
        options: JsonNode? = null
    ): IslValidationResults {
        // we can have two ways of validating
        // value has a type on it already
        // or we've passed a specific type/schema
        val typedValue = value as? TypedObjectNode?;
        val (schemaName, schemaUrl) = if (typedValue?.type?.schemaName != null || typedValue?.type?.schemaUrl != null) {
            listOf(typedValue.type?.schemaName, typedValue.type?.schemaUrl)
        } else if (options?.get("schemaName")?.textValue() != null || options?.get("schemaUrl")?.textValue() != null) {
            listOf(options.get("schemaName")?.textValue(), options.get("schemaUrl")?.textValue())
        } else {
            return IslValidationResults(IslValidationResultType.CouldNotFindTypeName);
        }

        val schema = getSchema(schemaName, schemaUrl)
            ?: return IslValidationResults(IslValidationResultType.CouldNotFindSchema);

        val results = mutableListOf<IslValidationResult>();
        schemaValidate(value, schema, results);
        if (results.isEmpty())
            return IslValidationResults(IslValidationResultType.Success)
        return IslValidationResults(IslValidationResultType.Issues, results);
    }

    private fun schemaValidate(value: JsonNode, schema: JsonSchema, results: MutableList<IslValidationResult>) {
        val validationResult = schema.validate(value);
        validationResult.forEach {
            println(JsonConvert.convert(it).toPrettyString())
            val error = when (it.type) {
                "required" -> IslValidationResult(IslValidationType.Required, it.jsonPath(), it.property, it.message)
                // odd that the .arguments has the real type as the second param
                "type" -> {
                    val expectedType = it.arguments.getOrNull(1)?.toString() ?: "";
                    IslValidationResult(
                        IslValidationType.Type,
                        it.instanceLocation.toString(),
                        expectedType,
                        it.message
                    )
                }

                "advanced" -> IslValidationResult(
                    IslValidationType.Advanced,
                    it.instanceLocation.toString(),
                    it.details?.get("expected")?.toString() ?: "",
                    it.message
                )

                // everything else is a rule validaton
                else -> IslValidationResult(
                    IslValidationType.Rules,
                    it.instanceLocation.toString(),
                    it.type,    // the required
                    it.message
                )
            }
            results.add(error)
        }
    }

    private fun ValidationMessage.jsonPath(): String {
        return "${this.instanceLocation}.${this.property}";
    }
}

enum class IslValidationType {
    /*
    Required Field Validation
     */
    Required,

    /*
    Data Type Validation
     */
    Type,

    /*
    Schema Rules Validation
     */
    Rules,

//    /*
//    Dependency Field is Missing
//     */
//    Dependency,
//
//    Other,

    /*
    ISL Advanced Validation
     */
    Advanced
}

data class IslValidationResult(
    val type: IslValidationType,
    val location: String,
    val expected: String,
    val message: String
);

enum class IslValidationResultType {
    Success,
    CouldNotFindTypeName,
    CouldNotFindSchema,
    Issues
}

data class IslValidationResults(
    val status: IslValidationResultType,
    val issues: List<IslValidationResult> = listOf()
)

data class IslValidationOptions(
    val todo: Boolean = true,
)