//package com.intuit.isl.validation
//
//import com.fasterxml.jackson.databind.ObjectMapper
//import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
//
//object SchemaExtensions {
//    private val yamlMapper = ObjectMapper(YAMLFactory());
//
//    /**
//     * Most of our schemas are actually in OpenAPI specifications
//     * openapi: 3.0.0
//     * so we need to remove the first part of them and keep just the schema for the validation
//     */
//    fun removeOpenApiSpecFromSchema(schema: String): String {
//        val result = yamlMapper.readTree(schema)
//
//        if(result.has("openapi")){
//            // this needs clearing
//            // components.schemas.someType
//            val realSchema = result.get("components")?.get("schemas")?.elements()?.next();
//            return realSchema?.toString() ?: "";
//        } else
//            return schema;
//    }
//}