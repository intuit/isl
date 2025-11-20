//package com.intuit.isl.debug
//
//import com.intuit.isl.common.ExecutionContext
//import com.intuit.isl.common.FunctionExecuteContext
//import com.intuit.isl.common.OperationContext
//import com.intuit.isl.commands.builder.ICommandVisitor
//import com.intuit.isl.commands.CommandResult
//import com.intuit.isl.commands.IIslCommand
//import com.intuit.isl.parser.tokens.IIslToken
//
//abstract class BaseDebuggableOperation(val command: IIslCommand) : IDebuggableOperation, IIslCommand {
//    override fun iterator(): ListIterator<IDebuggableOperation> {
//        return EmptyIterator;
//    }
//
//    override val token: IIslToken
//        get() = this.command.token;
//
//    private var _parent: IIslCommand? = null;
//
//    // debuggable operations don't have parent, only their commands have
//    override var parent: IIslCommand?
//        get() = this._parent;
//        set(value) {
//            this._parent = value;
//            this.command.parent = this;
//        }
//
//    override fun callStack(): OperationCallstack {
//        val callstack = ArrayList<StackFrame>();
//        var c = currentCommand;
//        while (c != null) {
//            callstack.add(StackFrame(callstack.size, c.toString(), c.token));
//
//            c = c.parent;
//            // jump over debuggable parents
////            if(c is IDebuggableOperation)
////                c = c.parent;
////            if(c.parent == null && c is IDebuggableOperation){
////                c = c.currentCommand?.parent;
////            } else {
////                c = c.parent;
////            }
//        }
//        return OperationCallstack(callstack.toTypedArray());
//    }
//
//    override var currentCommand: IIslCommand? = command;
//    private var currentContext: ExecutionContext? = null;
//    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
//        // pause and wait for confirmation we should continue
//
//        currentContext = executionContext;
//        val debugRunner = executionContext.operationContext
//            .getExtension("Debugger")
//            ?.invoke(FunctionExecuteContext("Debugger", currentCommand!!, executionContext, emptyArray<Any?>()))
//            as IDebugRunner;
//
//        debugRunner.onExecuting(this);
//        return command.executeAsync(executionContext);
//    }
//
//    protected fun addGlobalState(state: OperationState): IOperationState {
//        (currentContext?.operationContext as OperationContext?)
//            ?.variables
//            ?.forEach {
//                state.addGlobal(it.key, it.value.value);
//            };
//        return state;
//    }
//
//
//    override fun <T> visit(visitor: ICommandVisitor<T>): T {
//        throw NotImplementedError();
//    }
//
//    internal object EmptyIterator : ListIterator<Nothing> {
//        override fun hasNext(): Boolean = false
//        override fun hasPrevious(): Boolean = false
//        override fun nextIndex(): Int = 0
//        override fun previousIndex(): Int = -1
//        override fun next(): Nothing = throw NoSuchElementException()
//        override fun previous(): Nothing = throw NoSuchElementException()
//    }
//}