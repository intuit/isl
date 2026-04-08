package com.intuit.isl.common

import com.fasterxml.jackson.databind.JsonNode
import com.intuit.isl.commands.CommandResult
import com.intuit.isl.commands.modifiers.IConditionalCommand
import java.util.HashMap

// Async callback for host extensions (remains suspend - hosts can still provide suspend functions)
typealias AsyncContextAwareExtensionMethod = suspend (context: FunctionExecuteContext)-> Any?;

// Sync-only callback for internal/hardwired functions
typealias ContextAwareExtensionMethod = (context: FunctionExecuteContext)-> Any?;

// Async callback for annotations (remains suspend)
typealias AsyncExtensionAnnotation = suspend (context: AnnotationExecuteContext)-> Any?;

// Statement execution callback is now sync (runs on virtual thread)
typealias StatementExecution = (executionContext: ExecutionContext) -> CommandResult;

// Statement extensions are now sync-only (nobody uses async anyway)
typealias StatementsExtensionMethod = (context: FunctionExecuteContext, executeStatements: StatementExecution)-> Any?;

// Conditional extensions are now sync-only for simplicity
typealias ConditionalExtension = (command: IConditionalCommand, context: ExecutionContext)-> Any?;

interface IOperationContext {
    fun registerExtensionMethod(fullName: String, callback: AsyncContextAwareExtensionMethod): IOperationContext;

    fun registerConditionalExtensionMethod(fullName: String, extension: ConditionalExtension): IOperationContext;

    /**
     * Register an annotation - Kotlin async
     * */
    fun registerAnnotation(annotationName: String, callback: AsyncExtensionAnnotation): IOperationContext;

    /**
     * Register a method that can execute internal statements (e.g. @.Paginate.List() { } can create an internal loop of statements)
     * Now sync-only since nobody uses async statement extensions anyway
     */
    fun registerStatementMethod(fullName: String, callback: StatementsExtensionMethod): IOperationContext;

    /**
     * Returns an extension method/annotation based on its full name (Case Insensitive)
     * Extensions are now sync - async host extensions are wrapped during registration
     */
    fun getExtension(name: String): ContextAwareExtensionMethod?;
    fun getConditionalExtension(name: String): ConditionalExtension?;
    fun getAnnotation(annotationName: String): AsyncExtensionAnnotation?;
    fun getStatementExtension(name: String): StatementsExtensionMethod?;

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
