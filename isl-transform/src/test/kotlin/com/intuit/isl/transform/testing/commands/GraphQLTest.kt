package com.intuit.isl.transform.testing.commands

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Suppress("unused")
class GraphQLTest : BaseTransformTest() {
    companion object {
        @JvmStatic
        fun acceptGraphQL(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("\$page: 10;" +
                        "\$q: `{\n" +
                        "  products(first: \$page) {\n" +
                        "    edges {\n" +
                        "      node {\n" +
                        "        id\n" +
                        "        createdAt\n" +
                        "        title\n" +
                        "        description\n" +
                        "      }\n" +
                        "    }\n" +
                        "  }\n" +
                        "}`;\n" +
                        "r: \$q",
                    """{"r":"{\n  products(first: 10) {\n    edges {\n      node {\n        id\n        createdAt\n        title\n        description\n      }\n    }\n  }\n}"}""", null),
            )
        }
    }

    @ParameterizedTest
    @MethodSource(
        "acceptGraphQL"
    )
    fun runFixtures(script: String, expectedResult: String, map: Map<String, Any?>? = null) {
        run(script, expectedResult, map);
    }
}