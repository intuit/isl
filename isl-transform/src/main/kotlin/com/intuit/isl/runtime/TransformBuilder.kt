//package com.intuit.isl.runtime
//
//import com.intuit.isl.debug.TransformDebugBuilder
//import com.intuit.isl.debug.TransformDebugger
//import com.intuit.isl.commands.builder.ExecutionBuilder
//import com.intuit.isl.parser.IslScriptVisitor
//
//class TransformBuilder {
//    fun compile(moduleName: String, script: String): ITransformer {
//        val visitor = IslScriptVisitor(moduleName, script, System.out);
//
//        val module = visitor.parseXForm();
//
//        val command = ExecutionBuilder(moduleName, module, null).build();
//
//        val transformer = IslTransformer(command);
//
//        return transformer;
//    }
//
//    fun compileDebug(moduleName: String, script: String): TransformDebugger {
//        val visitor = IslScriptVisitor(moduleName, script, System.out);
//
//        val module = visitor.parseXForm();
//
//        val command = ExecutionBuilder(moduleName, module, TransformDebugBuilder()).build();
//
//        val transformer = IslTransformer(command);
//
//        return TransformDebugger(transformer);
//    }
//}