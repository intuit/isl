//package com.intuit.isl.debug
//
//import com.intuit.isl.commands.IIslCommand
//
///**
// * * Details of an operation that can be debugged. Any operation can have a sub-set of Operations.
// * This is nicely recursive so we can have any depth.
// */
//interface IDebuggableOperation{
//    var currentCommand: IIslCommand?;
//
//    /**
//     * Child operations
//     */
//    fun iterator(): Iterator<IDebuggableOperation>;
//
//    fun state(): IOperationState;
//    fun callStack(): OperationCallstack;
//}