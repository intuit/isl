//package com.intuit.isl.commands
//
//import com.fasterxml.jackson.databind.node.JsonNodeFactory
//import com.fasterxml.jackson.databind.node.ObjectNode
//import com.intuit.isl.commands.builder.ICommandVisitor
//import com.intuit.isl.common.ExecutionContext
//import com.intuit.isl.parser.tokens.ForEachToken
//import com.intuit.isl.parser.tokens.WithLockToken
//import com.intuit.isl.utils.ConvertUtils
//import com.intuit.isl.utils.IIslIterable
//import com.intuit.isl.utils.JsonConvert
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.async
//import kotlinx.coroutines.sync.Mutex
//import kotlinx.coroutines.sync.withLock
//
///**
// * Sometimes we have to lock.
// */
//class WithLockCommand(
//    token: WithLockToken,
//    val statements: IIslCommand
//) :
//    BaseCommand(token) {
//    override val token: ForEachToken
//        get() = super.token as ForEachToken;
//
//    private val mutex = Mutex()
//
//    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
//        mutex.withLock {
//            val statementsRes = statements.executeAsync(executionContext)
//            return statementsRes;
//        }
//    }
//
//    override fun <T> visit(visitor: ICommandVisitor<T>): T {
//        return visitor.visit(this);
//    }
//}