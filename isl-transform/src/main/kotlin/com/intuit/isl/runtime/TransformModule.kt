package com.intuit.isl.runtime

import com.intuit.isl.commands.FunctionDeclarationCommand
import com.intuit.isl.commands.IFunctionDeclarationCommand
import com.intuit.isl.common.AsyncContextAwareExtensionMethod
import com.intuit.isl.parser.tokens.FunctionType
import com.intuit.isl.parser.tokens.ModuleImplementationToken

/**
 * One complete transformation file loaded as a module.
 * It can have one or multiple functions: fun name( parameters ){ ... }
 * Compatible version have no functions defined in which case we have a default "run" function.
 */
class TransformModule(
    val name: String,
    functions: List<IFunctionDeclarationCommand>,
    val token: ModuleImplementationToken
) {
    private val _functions = HashMap<String, IFunctionDeclarationCommand>();
    private val _functionExtensions = HashMap<String, AsyncContextAwareExtensionMethod>();

    val functions
        get() = _functions.values as Collection<IFunctionDeclarationCommand>;
    internal val functionExtensions
        get() = _functionExtensions;

    val imports
        get() = token.imports.toList();

    init {
        functions.forEach {
            this._functions[it.name.lowercase()] = it;
            (it as FunctionDeclarationCommand).module = this;

            val fullName = when (it.token.functionType) {
                FunctionType.Function -> "this.${it.token.functionName.lowercase()}";
                FunctionType.Modifier -> "modifier.${it.token.functionName.lowercase()}";
            };

            val runner = it.getRunner();
            // we need some double registrations for now as we sometimes can call with or without prefix
            this._functionExtensions[it.name.lowercase()] = runner;
            this._functionExtensions[fullName] = runner;
        }
    }

    fun getFunction(name: String): IFunctionDeclarationCommand? {
        return this._functions[name.lowercase()];
    }

    fun getFunctionRunner(name: String): AsyncContextAwareExtensionMethod? {
        return this.functionExtensions[name];
    }
}