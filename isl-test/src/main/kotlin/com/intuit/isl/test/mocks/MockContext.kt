package com.intuit.isl.test.mocks

class MockContext<T>(mappingFunc : (mockObject : MockObject) -> T) {
    val func : T
    val mockObject = MockObject()

    init {
        func = mappingFunc(mockObject)
    }
}