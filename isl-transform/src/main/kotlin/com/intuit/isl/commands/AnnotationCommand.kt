package com.intuit.isl.commands

import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.common.AnnotationExecuteContext
import com.intuit.isl.parser.tokens.AnnotationDeclarationToken
import com.intuit.isl.parser.tokens.FunctionDeclarationToken
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.utils.ConvertUtils


class AnnotationCommand(
    token: AnnotationDeclarationToken,
    val arguments: List<IIslCommand>,
    val nextCommand: IIslCommand,
    val function: FunctionDeclarationToken
) :
    BaseCommand(token) {
    override val token: AnnotationDeclarationToken
        get() = super.token as AnnotationDeclarationToken;

    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val annotationCallback = executionContext.operationContext.getAnnotation(token.annotationName);
        val args =
            arguments.map { ConvertUtils.extractFromNode(it.executeAsync(executionContext).value) }.toTypedArray();

        if (annotationCallback == null) {
            val error = "Unknown Annotation: ${token.annotationName}";
//            executionContext.operationContext.interceptor?.onIssue(this, executionContext, error, args);
            return CommandResult(error);
        }

        val annotationContext = AnnotationExecuteContext(
            this,
            executionContext,
            args
        );

        val result = safeRunAnnotation(token.annotationName, executionContext, this) {
            annotationCallback.invoke(annotationContext)
        };
        return CommandResult(result);
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }

    companion object {
        suspend fun safeRunAnnotation(
            name: String,
            context: ExecutionContext,
            command: IIslCommand,
            func: suspend () -> Any?
        ): Any? {
            try {
                return func();
            } catch (e: TransformException) {
                val thisError = "Could not Execute '@$name' at ${command.token.position}.\n${e.message}";
                throw TransformException(thisError, command.token.position, e.cause);
            } catch (e: Exception) {
                val error = "${e.javaClass.simpleName}: ${e.message}";

//                try {
//                    context.operationContext.interceptor?.onIssue(
//                        command,
//                        context,
//                        "Could not Execute @$name: ${error}.",
//                        arrayOf<Any>()
//                    );
//                } catch (e: Exception) {
//                }

                throw TransformException(
                    "Could not Execute '@$name'. Error='${error}' at ${command.token.position}.",
                    command.token.position,
                    e
                );
            }
        }
    }
}