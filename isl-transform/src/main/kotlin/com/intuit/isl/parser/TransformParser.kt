package com.intuit.isl.parser

import com.intuit.isl.parser.tokens.ModuleImplementationToken

class TransformParser {
    fun parseTransform(moduleName: String, contents: String): ModuleImplementationToken {
        val visitor = IslScriptVisitor(moduleName, contents, System.out);

        val module = visitor.parseIsl();

        // TODO: Analyze the module see if there are any extra warnings we could generate to help the user

        return module;
    }
}