package com.intuit.isl.test

import com.intuit.isl.commands.CommandResult
import com.intuit.isl.common.*
import com.intuit.isl.test.mocks.MockContext

class TestOperationMockExtensions {
    val mockExtensions = HashMap<String, MockContext<AsyncContextAwareExtensionMethod>>()
    val mockAnnotations = HashMap<String, MockContext<AsyncExtensionAnnotation>>()
    val mockStatementExtensions = HashMap<String, MockContext<AsyncStatementsExtensionMethod>>()
}