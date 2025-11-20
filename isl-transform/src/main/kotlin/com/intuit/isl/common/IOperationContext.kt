package com.intuit.isl.common

import com.fasterxml.jackson.databind.JsonNode
import com.intuit.isl.commands.CommandResult
import com.intuit.isl.commands.modifiers.IConditionalCommand
import java.util.HashMap

// Async callback (we'll convert sync ones to async in case anyone wants simple sync callbacks)
typealias AsyncContextAwareExtensionMethod = suspend (context: FunctionExecuteContext)-> Any?;

typealias AsyncExtensionAnnotation = suspend (context: AnnotationExecuteContext)-> Any?;

// Callback to execute an internal block of code or statements
typealias StatementExecution = suspend (executionContext: ExecutionContext) -> CommandResult;
typealias AsyncStatementsExtensionMethod = suspend (context: FunctionExecuteContext, executeStatements: StatementExecution)-> Any?;

typealias ConditionalExtension = suspend (command: IConditionalCommand, context: ExecutionContext)-> Any?;

interface IOperationContext {
    fun registerExtensionMethod(fullName: String, callback: AsyncContextAwareExtensionMethod): IOperationContext;

    fun registerConditionalExtensionMethod(fullName: String, extension: ConditionalExtension): IOperationContext;

    /**
     * Register an annotation - Kotlin async
     * */
    fun registerAnnotation(annotationName: String, callback: AsyncExtensionAnnotation): IOperationContext;

    /**
     * Register a method that can execute internal statements (e.g. @.Paginate.List() { } can create an internal loop of statements)
     */
    fun registerStatementMethod(fullName: String, callback: AsyncStatementsExtensionMethod): IOperationContext;

    /**
     * Returns an extension method/annotation based on its full name (Case Insensitive)
     */
    fun getExtension(name: String): AsyncContextAwareExtensionMethod?;
    fun getConditionalExtension(name: String): ConditionalExtension?;
    fun getAnnotation(annotationName: String): AsyncExtensionAnnotation?;
    fun getStatementExtension(name: String): AsyncStatementsExtensionMethod?;

    /**
     * Add @param name Variable. Name will be visible as `$$name` in the transform code.
     */
    fun setVariable(name: String, node: JsonNode, setIsModified: Boolean? = null): IOperationContext;
    fun setVariable(name: String, variable: TransformVariable): IOperationContext;
    fun getVariable(name: String): JsonNode?;
    fun getTransformVariable(name: String): TransformVariable?;
    fun removeVariable(name: String);
    val variables: HashMap<String, TransformVariable>;
//    val interceptor: ICommandInterceptor?;
}

fun IOperationContext.resetVariable(name: String, node: JsonNode?){
    if (node == null)
        this.removeVariable(name);
    else
        this.setVariable(name, node, false);
}
