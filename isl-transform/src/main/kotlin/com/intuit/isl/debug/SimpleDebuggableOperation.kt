//package com.intuit.isl.debug
//
//import com.intuit.isl.commands.builder.ICommandVisitor
//import com.intuit.isl.commands.IIslCommand
//
///**
// * One level deep operation (e.g. value assignment)
// */
//@Suppress("UNCHECKED_CAST")
//class SimpleDebuggableOperation<T : IIslCommand>(command: T, val stateRead: (command:T) -> OperationState)
//    : BaseDebuggableOperation(command) {
//    override fun iterator(): ListIterator<IDebuggableOperation> {
//        return EmptyIterator;
//    }
//
//    override fun state(): IOperationState {
//        return addGlobalState(this.stateRead(this.command as T));
//    }
//
////    override suspend fun executeAsync(context: ExecutionContext): CommandResult {
////        // pause and wait for confirmation we should continue
////        val debugRunner = context.operationContext
////            .getExtension("Debugger")
////            ?.invoke(Array(0){}) as IDebugRunner;
////        debugRunner.onExecuting(this);
////        return command.executeAsync(context);
////    }
//
//    override fun <T> visit(visitor: ICommandVisitor<T>): T {
//        throw NotImplementedError();
//    }
//
//    override fun toString(): String {
//        return "[Simple] $command - ${command.token}";
//    }
//}
//
//
