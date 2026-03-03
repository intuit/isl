package com.intuit.isl.test.annotations

import com.intuit.isl.common.AnnotationExecuteContext

object SetupAnnotation {
    const val annotationName = "setup"

    suspend fun runAnnotationFunction(context: AnnotationExecuteContext) : Any? {
        return context.runNextCommand()
    }
}

