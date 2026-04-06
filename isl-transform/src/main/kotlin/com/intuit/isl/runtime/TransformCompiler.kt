package com.intuit.isl.runtime

import com.intuit.isl.commands.builder.ExecutionBuilder
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.parser.TransformParser

class TransformCompiler {
    /**
     * Compile a full ISL Script (includes functions, modifiers, imports)
     */
    fun compileIsl(
        moduleName: String, script: String,
        moduleFinder: ((name: String) -> ITransformer?)? = null
    ): ITransformer {
        val moduleToken = TransformParser().parseTransform(moduleName, script);

        val module = ExecutionBuilder(moduleName, moduleToken, moduleFinder).build();

        val transformer = Transformer(module);

        return transformer;
    }

    fun compileLocalIsl(
        moduleName: String, script: String,
        localContext: IOperationContext
    ): ILocalTransformer {
        val moduleToken = TransformParser().parseTransform(moduleName, script);

        val module = ExecutionBuilder(moduleName, moduleToken, null, localContext).build();

        val transformer = LocalTransformer(module);
        return transformer;
    }


}