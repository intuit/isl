package com.intuit.isl.playground.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.intuit.isl.common.OperationContext
import com.intuit.isl.playground.model.*
import com.intuit.isl.runtime.TransformCompilationException
import com.intuit.isl.runtime.TransformCompiler
import com.intuit.isl.runtime.TransformException
import org.springframework.stereotype.Service

@Service
class IslService {

    private val jsonMapper = ObjectMapper().registerKotlinModule()
    private val yamlMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    private fun parseInput(input: String, format: String): JsonNode {
        val normalized = (format ?: "json").lowercase()
        return when (normalized) {
            "yaml" -> yamlMapper.readTree(input)
            else -> jsonMapper.readTree(input)
        }
    }

    private fun writeOutput(result: Any?, format: String): String {
        val normalized = (format ?: "json").lowercase()
        val value = result ?: jsonMapper.createObjectNode()
        return when (normalized) {
            "yaml" -> yamlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value)
            else -> jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value)
        }
    }
    
    fun transform(request: TransformRequest): TransformResponse {
        return try {
            val inputFmt = request.inputFormat?.lowercase() ?: "json"
            val outputFmt = request.outputFormat?.lowercase() ?: "json"

            // Parse the input (JSON or YAML) to JsonNode
            val inputJson = parseInput(request.input, inputFmt)
            
            // Compile the ISL script
            val compiler = TransformCompiler()
            val transformer = compiler.compileIsl("playground", request.isl)
            
            // Create operation context with input
            val context = OperationContext()
            context.setVariable("\$input", inputJson)
            
            // Try to find an available function to execute
            // Priority: run > main > transform > first available function
            val functionNames = listOf("run", "main", "transform")
            val availableFunctions = transformer.module.functions.map { it.name.lowercase() }
            
            val functionToRun = functionNames.firstOrNull { it in availableFunctions }
                ?: availableFunctions.firstOrNull()
                ?: "run" // fallback to "run" for compatibility

            println( "Script has functions=" + availableFunctions + " ISL will run on=" + functionToRun );

            // Execute the transformation
            val result = transformer.runTransformSync(functionToRun, context)
            
            // Convert result to requested output format (JSON or YAML)
            val output = writeOutput(result, outputFmt)
            
            TransformResponse(
                success = true,
                output = output
            )
        } catch (e: TransformCompilationException) {
            TransformResponse(
                success = false,
                error = ErrorDetail(
                    message = e.message ?: "Compilation error",
                    line = e.position?.line,
                    column = e.position?.column,
                    type = "COMPILATION_ERROR"
                )
            )
        } catch (e: TransformException) {
            TransformResponse(
                success = false,
                error = ErrorDetail(
                    message = e.message ?: "Transformation error",
                    line = e.position?.line,
                    column = e.position?.column,
                    type = "TRANSFORM_ERROR"
                )
            )
        } catch (e: Exception) {
            TransformResponse(
                success = false,
                error = ErrorDetail(
                    message = e.message ?: "Runtime error: ${e.javaClass.simpleName}",
                    type = "RUNTIME_ERROR"
                )
            )
        }
    }
    
    fun validate(request: ValidationRequest): ValidationResponse {
        return try {
            // Try to compile the ISL script
            val compiler = TransformCompiler()
            compiler.compileIsl("validation", request.isl)
            
            ValidationResponse(valid = true)
        } catch (e: TransformCompilationException) {
            ValidationResponse(
                valid = false,
                errors = listOf(
                    ErrorDetail(
                        message = e.message ?: "Compilation error",
                        line = e.position?.line,
                        column = e.position?.column,
                        type = "COMPILATION_ERROR"
                    )
                )
            )
        } catch (e: Exception) {
            ValidationResponse(
                valid = false,
                errors = listOf(
                    ErrorDetail(
                        message = e.message ?: "Validation error",
                        type = "ERROR"
                    )
                )
            )
        }
    }
    
    fun getExamples(): List<ExampleResponse> {
        return listOf(
            ExampleResponse(
                name = "Simple Field Mapping",
                description = "Copy and rename fields from input to output",
                isl = """
                    fun run( ${'$'}input ){
                      return {
                        first_name: ${'$'}input.firstName,
                        last_name: ${'$'}input.lastName,
                        age: ${'$'}input.age
                      }
                    }
                """.trimIndent(),
                input = """
                    {
                      "firstName": "John",
                      "lastName": "Doe",
                      "age": 30
                    }
                """.trimIndent(),
                expectedOutput = """
                    {
                      "first_name": "John",
                      "last_name": "Doe",
                      "age": 30
                    }
                """.trimIndent()
            ),
            ExampleResponse(
                name = "Array Transformation",
                description = "Transform each item in an array",
                isl = """
                    fun run( ${'$'}input ){
                      return {
                        products: foreach ${'$'}item in ${'$'}input.items
                          {
                            title: ${'$'}item.name,
                            cost: ${'$'}item.price,
                            currency: "USD"
                          }
                        endfor
                      }
                    }
                """.trimIndent(),
                input = """
                    {
                      "items": [
                        {"name": "Product A", "price": 10},
                        {"name": "Product B", "price": 20}
                      ]
                    }
                """.trimIndent(),
                expectedOutput = """
                    {
                      "products": [
                        {"title": "Product A", "cost": 10, "currency": "USD"},
                        {"title": "Product B", "cost": 20, "currency": "USD"}
                      ]
                    }
                """.trimIndent()
            ),
            ExampleResponse(
                name = "Conditional Transformation",
                description = "Include fields based on conditions",
                isl = """
                    fun run( ${'$'}input ) {
                      return {
                        username: ${'$'}input.user,
                        age: ${'$'}input.age,
                        membership: if (${'$'}input.premium) "premium" else "standard" endif,
                        discount_percent: if (${'$'}input.premium) ${'$'}input.discount endif
                      }
                    }
                """.trimIndent(),
                input = """
                    {
                      "user": "john",
                      "age": 25,
                      "premium": true,
                      "discount": 10
                    }
                """.trimIndent(),
                expectedOutput = """
                    {
                      "username": "john",
                      "age": 25,
                      "membership": "premium",
                      "discount_percent": 10
                    }
                """.trimIndent()
            ),
            ExampleResponse(
                name = "Default Values",
                description = "Provide default values using coalesce operator",
                isl = """
                    fun run( ${'$'}input ) {
                      return {
                        name: ${'$'}input.name,
                        price: ${'$'}input.price,
                        quantity: ${'$'}input.quantity ?? 1,
                        available: ${'$'}input.available ?? true,
                        category: ${'$'}input.category ?? "uncategorized"
                      }
                    }
                """.trimIndent(),
                input = """
                    {
                      "name": "Product",
                      "price": 50
                    }
                """.trimIndent(),
                expectedOutput = """
                    {
                      "name": "Product",
                      "price": 50,
                      "quantity": 1,
                      "available": true,
                      "category": "uncategorized"
                    }
                """.trimIndent()
            )
        )
    }
}
