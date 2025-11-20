//package com.intuit.isl.debug
//
//import com.intuit.isl.common.OperationContext
//import com.intuit.isl.utils.JsonConvert
//import com.intuit.isl.runtime.TransformCompiler
//import com.intuit.isl.parser.tokens.IIslToken
//
//class DebugSession(val script: String, var context: OperationContext? = null) {
//    private var debugger: TransformDebugger;
//
//    init {
//        val t = TransformCompiler().compileDebug("debug", script, null);
//        context = context ?: OperationContext();
//
//        debugger = t.start(context!!);
//    }
//
//    val token: IIslToken
//        get() {
//            return debugger.transformer.token;
//        }
//
//    suspend fun userGetState(): IOperationState?{
//        return debugger.userGetState();
//    }
//    suspend fun userGetCallstack(): OperationCallstack? {
//        return debugger.userGetCallstack();
//    }
//    suspend fun userStepInto(){
//        debugger.userStepInto();
//    }
//    suspend fun userStepOver(){
//        debugger.userStepOver();
//    }
//
//
//
//    /**
//     * Stop the current debugger
//     */
//    suspend fun stop(){
//        // TODO:
//    }
//}