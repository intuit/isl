//package com.intuit.isl.transform.testing.debugging
//
//import com.intuit.isl.common.OperationContext
////import com.intuit.isl.debug.TransformDebugger
//import com.intuit.isl.utils.JsonConvert
//import com.intuit.isl.runtime.TransformCompiler
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.runBlocking
//import org.junit.jupiter.api.Test
//
//class DebuggingTest {
//
//    //@Test // disabled for now until we get the SelfServe debugger plugged in.
//    fun debuggerFixture1() {
//        debug(
//            "description: 123", null, arrayOf(
//                "stepInto", // function declaration
//                "stepInto", // object declaration
//                "stepInto",  // assign
//                "stepInto",  // value
//            )
//        );
//    }
//
//    //@Test
//    fun debuggerFixture2() {
//        debug("description: \$var", mapOf("var" to 123), arrayOf("stepInto", "stepInto", "stepInto"));
//    }
//
//
//    fun debug(script: String, map: Map<String, Any?>?,  operations: Array<String>) {
//        println("Loading:\n$script\nDebug:${operations.joinToString(",")}");
//
//        val context = OperationContext();
//        map?.forEach {
//            context.setVariable(it.key, JsonConvert.convert(it.value));
//        }
//
//        runBlocking {
//            val t = TransformCompiler().compileDebug("test", script);
//            val debugger = t.start(context);
//            println("Debugger Started");
//
//            // we can now start a separate thread and fire back to this transformer all our commands
//            // we'll do it every few seconds to see how it replies
//            val operations = launch {
//                runOperations(operations, debugger)
//            }
//
//            println("Unit Test Waiting for Debugger To Finish");
//            val result = debugger.sessionResult?.await();
//
//            println("Done $result");
//
//            operations.cancel();
//            operations.join();
//        }
//    }
//    suspend fun runOperations(operations: Array<String>, debugger: TransformDebugger) {
//        operations.forEach {
//            println("\n\n\nGetting ready for > $it");
//            delay(1000);
//
//            println("Start State:\n${debugger.userGetState()}");
//            println("Call Stack :\n${debugger.userGetCallstack()}");
//            when(it){
//                "stepInto" -> debugger.userStepInto();
//                "state" -> {};
//                else -> println("Unknown $it");
//            }
//            println("End State:\n${debugger.userGetState()}");
//        }
//        println("No more operations");
//    }
//}