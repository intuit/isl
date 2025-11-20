package com.intuit.isl.runtime

//import com.intuit.isl.debug.TransformDebugBuilder
//import com.intuit.isl.debug.TransformDebugger
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

        val module = ExecutionBuilder(moduleName, moduleToken, null, moduleFinder).build();

        val transformer = Transformer(module);

        return transformer;
    }

    fun compileLocalIsl(
        moduleName: String, script: String,
        localContext: IOperationContext
    ): ILocalTransformer {
        val moduleToken = TransformParser().parseTransform(moduleName, script);

        val module = ExecutionBuilder(moduleName, moduleToken, null, null, localContext).build();

        val transformer = LocalTransformer(module);
        return transformer;
    }


//    fun compileDebug(
//        moduleName: String,
//        script: String,
//        moduleFinder: ((name: String) -> ITransformer?)? = null
//    ): TransformDebugger {
//        val token = TransformParser().parseTransform(moduleName, script);
//
//        val command = ExecutionBuilder(moduleName, token, TransformDebugBuilder(), moduleFinder).build();
//
//        val transformer = Transformer(command);
//
//        return TransformDebugger(transformer);
//    }
}