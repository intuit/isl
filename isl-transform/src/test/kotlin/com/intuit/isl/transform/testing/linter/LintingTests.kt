//package com.intuit.isl.transform.testing.linter
//
//import com.fasterxml.jackson.databind.JsonNode
//import com.fasterxml.jackson.databind.node.ObjectNode
//import com.intuit.isl.linter.IslLintBuilder
//import com.intuit.isl.parser.TransformParser
//import com.intuit.isl.transform.testing.commands.AssertEqualityType
//import com.intuit.isl.transform.testing.commands.YamlTransformTest
//import com.intuit.isl.utils.JsonConvert
//import kotlinx.coroutines.runBlocking
//import org.junit.jupiter.api.Disabled
//import org.junit.jupiter.api.TestInstance
//import org.junit.jupiter.params.ParameterizedTest
//import org.junit.jupiter.params.provider.Arguments
//import org.junit.jupiter.params.provider.MethodSource
//import java.util.stream.Stream
//
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@Suppress("unused", "UNUSED_PARAMETER")
//class LintingTests : YamlTransformTest("linter") {
//
//    private fun callapiTests(): Stream<Arguments> {
//        return createTests("callapi")
//    }
//
//    private fun conditions(): Stream<Arguments> {
//        return createTests("conditions")
//    }
//
//    private fun interpolate(): Stream<Arguments> {
//        return createTests("interpolate")
//    }
//
//    private fun concatToAppend(): Stream<Arguments> {
//        return createTests("concat-to-append")
//    }
//
//    private fun simplerToString(): Stream<Arguments> {
//        return createTests("tostring")
//    }
//
//    @Disabled("Temp")
//    @ParameterizedTest
//    @MethodSource(
//        "callapiTests",
//        "conditions",
//        "interpolate",
//        "concatToAppend",
//        "simplerToString"
//    )
//    fun runFixtures(testName: String, script: String, expectedResult: JsonNode, map: Map<String, Any?>? = null) {
//        val moduleToken = TransformParser().parseTransform("test", script)
//        val lintingCode = readResource("tests/linter/linter.isl");
//
//        // as a safety we also want to run the code
//        // both as original and the recommendation so make sure our recommendation is not breaking it
//
//        runBlocking {
//            val linter = IslLintBuilder().buildLinter(lintingCode);
//            val issues = linter.lintCode(script, moduleToken);
//
//            println("Detected Issues:")
//            println(issues);
//
//            val jsonIssues = JsonConvert.convert(issues);
//            jsonIssues.forEach {
//                (it as ObjectNode).remove("type");
//                it.remove("token");
//                it.remove("id");
//                it.remove("message");
//                it.remove("help");
//            }
//            println()
//
//            println("Expected Issues:")
//            val expectedIssues = expectedResult["issues"];
//            println(expectedIssues)
//            compareJsonResults(expectedIssues?.toPrettyString() ?: "[]", jsonIssues, AssertEqualityType.Json)
//
//            if (expectedResult["result"] != null) {
//                println();
//                println("Running Original Script:")
//                println(script);
//                // we have an expected result - let's run it
//                val originalResult = run(script, expectedResult["result"]?.toPrettyString() ?: "", emptyMap())
//                println("Original Result:")
//                println(originalResult.second.result)
//
//                // if we have an updated script let's run that as well
//                val updatedScript = expectedResult["updatedScript"]?.textValue();
//                if (updatedScript != null) {
//                    println();
//                    println("Running updatedScript:")
//                    println(updatedScript);
//                    run(updatedScript, expectedResult["result"]?.toPrettyString() ?: "", emptyMap())
//                }
//            }
//        }
//    }
//
//    private fun renderWarning(script: String, warning: JsonNode) {
//        val loc = warning.get("loc")?.textValue();
//        if (loc.isNullOrEmpty())
//            return;
//        // "L1:C12/L1:C13"
//        val parts = loc.split("/");
//        val l = parts[0].substringBefore(":").toInt() - 1;
//        val c = parts[0].substringAfter(":").toInt();
//        val el = parts[1].substringBefore(":").toInt() - 1;
//        val ec = parts[1].substringAfter(":").toInt();
//
//        // first start line
////        val lines = script.split("\n");
////        if (l < lines.size) {
////            if (l == el) {
////                println("Warning: ${warning["message"].textValue()} at: [${l + 1}:$c]-${el}")
////                println(lines[l])
////                val chars = "~".repeat(ec - c);
////                val start = " ".repeat(c);
////                println(start + chars);
////                println("Replace with: ${warning["recommendation"]}");
////            } else {
////                // single line error
////                println("Warning: ${warning["message"].textValue()} at: [${l + 1}:$c]-[${el + 1}:$ec]")
////                for (lineIndex in l..el) {
////                    println(lines[lineIndex])
////                }
////                println("Replace with: ${warning["recommendation"]}");
////            }
////        } else {
////            println("Warning: ${warning["message"].textValue()} at: [${l + 1}:$c]")
////            println("Replace with: ${warning["recommendation"]}");
////        }
//    }
//}