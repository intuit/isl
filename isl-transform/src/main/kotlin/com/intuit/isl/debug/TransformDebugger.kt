//package com.intuit.isl.debug
//
//import com.fasterxml.jackson.databind.JsonNode
//import com.intuit.isl.common.IOperationContext
//import com.intuit.isl.runtime.Transformer
//import kotlinx.coroutines.CompletableDeferred
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.channels.Channel
//import kotlinx.coroutines.channels.SendChannel
//import kotlinx.coroutines.channels.actor
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.runBlocking
//import kotlin.concurrent.thread
//
//class TransformDebugger(val transformer: Transformer) : IDebugRunner {
//    var debugControlActor: SendChannel<DebugMessage>? = null;
//    //var runningControlActor: SendChannel<RunMessage>? = null;
//    var operationContext: IOperationContext? = null;
//
//    fun start(operationContext: IOperationContext): TransformDebugger {
//        // we start a background thread that will run the actual transformation and will wait for us to give it commands
//        // let's play with actors - https://kotlinlang.org/docs/shared-mutable-state-and-concurrency.html#actors
//        println(">>>> Starting Debugger");
//
//        this.operationContext = operationContext;
//        sessionResult = CompletableDeferred();
//
//        thread ( start = true ){
//            startDebugger();
//        }
//
//        println(">>>> Ready to Debug");
//        return this;
//    }
//
//    private fun startDebugger(){
//        runBlocking {
//            launch {
//                debugControlActor = debuggingControlActor();
//
//                // register ourselves
//                operationContext?.registerExtensionMethod("Debugger") {
//                    return@registerExtensionMethod this@TransformDebugger;
//                };
//            }
//
//            launch{
//                startTransform();
//            }
//        }
//    }
//
//
//    /**
//     * Receive commands from the user
//     */
//    suspend fun userStepInto(){
//        this.lastUserInput.send(UserStepInto);
//        // TODO: wait for a confirmation from the other thread that it managed to step in
//    }
//    suspend fun userStepOver(){
//        this.lastUserInput.send(UserStepInto);
//        // TODO: wait for a confirmation from the other thread that it managed to step in
//    }
//
//    /**
//     * Current state (variables, locals)
//     */
//    suspend fun userGetState() : IOperationState? {
//        return currentOperation?.state();
//    }
//    suspend fun userGetCallstack(): OperationCallstack?{
//        return currentOperation?.callStack();
//    }
//
//    var sessionResult: CompletableDeferred<JsonNode?>? = null;
//
//    private suspend fun startTransform(){
//        println(">>>> Started Runner");
//
//        val result = transformer.runTransformAsync("run", operationContext!!);
//
//        println(">>>> Finished Runner");
//        sessionResult?.complete(result.result);
//        debugControlActor?.close();
//    }
//
//    // this interfaces with the runner
//    private var currentOperation: IDebuggableOperation? = null;
//    override suspend fun onExecuting(operation: IDebuggableOperation) {
//        currentOperation = operation;
//        // wait here
//        if (debugControlActor != null) {
//            val response = CompletableDeferred<DebugCommand>();
//            println("> Executing $operation");
//            debugControlActor?.send(GetNextCommand(response));
//            val command = response.await();
//            println("> Executing Response: ${command.javaClass.simpleName}");
//            // execute the command
//        }
//    }
//
//    private sealed class UserInputCommand;
//    private object UserStepInto: UserInputCommand();
//    private val lastUserInput = Channel<UserInputCommand> ();
//
//
//    sealed class DebugCommand;
//    sealed class DebugMessage;
//    object DebuggerStepOver : DebugCommand();
//    class GetNextCommand(val nextCommand: CompletableDeferred<DebugCommand>): DebugMessage();
//
//    fun CoroutineScope.debuggingControlActor() = actor<DebugMessage>{
//        println(">>> Started Debugger");
//        //var current: IDebuggableOperation = getDebuggingOperation();
//
//        for(msg in channel){
//            when(msg){
//                is GetNextCommand -> {
//                    println("Runner asks about the next command. We'll wait for user input.");
//                    val lastInput = lastUserInput.receive();  // here we can add any user timeout if we want
//                    msg.nextCommand.complete(convertLastUserInput(lastInput));
//                }
//            }
//        }
//        println(">>> Finished Debugger");
//    }
//
//    private fun convertLastUserInput(lastInput: UserInputCommand): DebugCommand {
//        return when(lastInput){
//            is UserStepInto -> DebuggerStepOver;
//            else -> throw NotImplementedError("Unknown user command: $lastInput");
//        }
//    }
//}