package com.intuit.isl.transform.testing.datacontract

import com.fasterxml.jackson.databind.JsonNode

data class YamlTestSuite(var name: String,
                    var tests: List<YamlTestEntry>,
                    var scripts: List<YamlTestScript>? = null)

data class YamlTestScript(var name: String, var value: String)

data class YamlTestEntry(var name: String,
                         var script: String? = null,
                         var expected: JsonNode,
                         var scriptReference: String? = null,
                         var inputs: Map<String, Any>? = null)