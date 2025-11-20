//package com.intuit.isl.debug
//
//import com.intuit.isl.commands.FunctionDeclarationCommand
//import com.intuit.isl.commands.IFunctionDeclarationCommand
//import com.intuit.isl.commands.IIslCommand
//import com.intuit.isl.common.AsyncContextAwareExtensionMethod
//import com.intuit.isl.parser.tokens.FunctionDeclarationToken
//class FunctionDeclarationDebugDecorator(command: FunctionDeclarationCommand) : BaseDebuggableOperation(command), IFunctionDeclarationCommand {
//    override val token: FunctionDeclarationToken
//        get() = super.token as FunctionDeclarationToken
//
//    override val statements: IIslCommand
//        get() = command;
//
//    override fun state(): IOperationState {
//        //return addGlobalState(this.stateRead(this.command as T));
//        return OperationState(null, null);    // we should add function name & parameters
//    }
//    override val name: String
//        get() = (command as FunctionDeclarationCommand).name;
//
//    override fun getRunner(): AsyncContextAwareExtensionMethod {
//        TODO("Not yet implemented")
//    }
//}