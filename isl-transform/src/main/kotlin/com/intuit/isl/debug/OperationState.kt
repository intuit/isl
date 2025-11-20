//package com.intuit.isl.debug
//
//import com.intuit.isl.commands.IIslCommand
//import com.intuit.isl.parser.tokens.IIslToken
//
//class OperationState(override val command: IIslCommand?, override val token: IIslToken?) : IOperationState {
//    override val globals = HashMap<String, Any?>();
//    override val locals = HashMap<String, Any?>();
//
//    fun addLocal(name: String, value: Any?): OperationState{
//        locals[name] = value;
//        return this;
//    }
//    fun addGlobal(name: String, value: Any?): OperationState{
//        globals[name] = value;
//        return this;
//    }
//
//    override fun toString(): String {
//        val sb = StringBuilder("Current: $token\n");
//
//        locals.forEach{
//            sb.appendLine("\tLocal : ${it.key} = ${it.value}");
//        }
//        globals.forEach{
//            sb.appendLine("\tGlobal: ${it.key} = ${it.value}");
//        }
//
//        return sb.toString();
//    }
//}