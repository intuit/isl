package com.intuit.isl.validation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.runtime.IslException
import com.intuit.isl.utils.Position

class IslValidationException(message: String, val issues: List<IslValidationResult>, override val position: Position?) :
    IslException, Exception(message) {}

/**
 * This will validate an existing ISL entity against its declared schema
 */
class ValidateTypeModifier {

    private lateinit var validator: IslSchemaProcessor;
    fun registerExtensions(
        context: IOperationContext,
        failOnMissingTypeName: Boolean = false,
        retrieveSchema: AsyncRetrieveSchema
    ) {
        this.validator = IslSchemaProcessor(IslSchemaProcessingOptions(), retrieveSchema);

        context.registerExtensionMethod("Modifier.validate.*") { functionContext ->
            val value = functionContext.firstParameter as JsonNode;
            val options = functionContext.secondParameter as? ObjectNode?;

            val result = validator.validateType(value, options);

            // in the modifier we'll automatically fail if anything went wrong
            // todo: Add an override to return the result
            when (result.status) {
                IslValidationResultType.Success -> return@registerExtensionMethod value;
                IslValidationResultType.CouldNotFindTypeName ->
                    if (failOnMissingTypeName)
                        throw IslValidationException(
                            "Could not find TypeName for Object",
                            listOf(),
                            functionContext.command.token.position
                        )
                    else return@registerExtensionMethod value;  // don't fail
                IslValidationResultType.CouldNotFindSchema ->
                    if (failOnMissingTypeName)
                        throw IslValidationException(
                            "Could not find Schema for Object",
                            listOf(),
                            functionContext.command.token.position
                        )
                    else return@registerExtensionMethod value;  // don't fail
                IslValidationResultType.Issues -> {
                    val message = result.issues.map { it.message }
                        .joinToString(",");
                    throw IslValidationException(
                        message,
                        result.issues,
                        functionContext.command.token.position
                    )
                }
            }
        }
    }
}