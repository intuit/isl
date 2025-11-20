package com.intuit.isl.transform.testing.commands

import com.fasterxml.jackson.databind.JsonNode
import com.intuit.isl.commands.modifiers.IConditionalCommand
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.OperationContext
import com.intuit.isl.runtime.Transformer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.run

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Suppress("UNUSED_PARAMETER")
class ForLoopTest2 : YamlTransformTest("forloop") {

    private fun parallelFor(): Stream<Arguments> {
        return createTests("parallel-for")
    }

    private fun parallelSingleThreadFor(): Stream<Arguments> {
        return createTests("parallel-for-single-thread")
    }

    @ParameterizedTest(name = "{index}. {0}")
    @MethodSource(
        "parallelFor"
    )
    fun runFixtures(testName: String, script: String, expectedResult: JsonNode, map: Map<String, Any?>? = null) {
        Transformer.maxParallelWorkers = 6
        run(script, expectedResult.toPrettyString(), map)
    }

    @ParameterizedTest(name = "{index}. {0}")
    @MethodSource(
        "parallelSingleThreadFor"
    )
    fun runFixturesSingleThread(
        testName: String,
        script: String,
        expectedResult: JsonNode,
        map: Map<String, Any?>? = null
    ) {
        Transformer.maxParallelWorkers = 1;
        try {
            run(script, expectedResult.toPrettyString(), map)
        } finally {
            Transformer.maxParallelWorkers = 6;
        }
    }

    override fun onRegisterExtensions(context: OperationContext) {
        val mutex = Mutex();
        val threadIds = mutableMapOf<String, Int>();

        // to ISL we want to return 1, 2, 3, .. as consistent values not the read thread id
        context.registerExtensionMethod("Thread.Id") {
            return@registerExtensionMethod mutex.withLock {
                val id = Thread.currentThread().name;
                if (threadIds.containsKey(id)) {
                    return@withLock threadIds[id];
                }
                val newId = threadIds.count() + 1;
                threadIds[id] = newId;
                return@withLock newId;
            }
        }

        super.onRegisterExtensions(context)
    }
}

