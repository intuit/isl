package com.intuit.isl.common

import com.fasterxml.jackson.databind.JsonNode
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.utils.Position
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap

/**
 * Context specific to one operation.
 * This is state-full and represents the current state of the operation and transformation.
 */
open class BaseOperationContext : IOperationContext {
    override val variables = HashMap<String, TransformVariable>();

    // We carry two sets of extensions ! which have various priorities
    // `extensions` are the publicly registered set of extensions and modifiers that were done from code
    // generally by the host of the engine
    // these are carried across as we create child contexts or call functions either in the same module or other modules
    protected val extensions: HashMap<String, AsyncContextAwareExtensionMethod>;
    protected val annotations: HashMap<String, AsyncExtensionAnnotation>;
    protected val statementExtensions: HashMap<String, AsyncStatementsExtensionMethod>;
    protected val conditionalExtensions: HashMap<String, ConditionalExtension>;

    // internal extensions are the ones that are registered through code (e.g automatically via @.This or dynamically via import statements)
    // These are not carried in child contexts
    protected var internalExtensions: HashMap<String, AsyncContextAwareExtensionMethod>;
    
    // Performance optimization: Cache extension lookups to avoid repeated HashMap traversals
    private val extensionCache = ConcurrentHashMap<String, AsyncContextAwareExtensionMethod?>()

//    override val interceptor: ICommandInterceptor?;

    constructor() {
        extensions = HashMap();
        annotations = HashMap()
        statementExtensions = HashMap();

        internalExtensions = HashMap();
        conditionalExtensions = HashMap();
//        interceptor = null;
    }

    constructor(
        extensions: HashMap<String, AsyncContextAwareExtensionMethod>,
        annotations: HashMap<String, AsyncExtensionAnnotation>,
        statementExtensions: HashMap<String, AsyncStatementsExtensionMethod>,

        internalExtensions: HashMap<String, AsyncContextAwareExtensionMethod>,
        conditionalExtensions: HashMap<String, ConditionalExtension>,
//        interceptor: ICommandInterceptor?
    ) {
        this.extensions = extensions;
        this.annotations = annotations;
        this.statementExtensions = statementExtensions;

        this.internalExtensions = internalExtensions;
        this.conditionalExtensions = conditionalExtensions;
//        this.interceptor = interceptor;
        // Note: extensionCache is automatically initialized per instance
    }

    /**
     * Same module child context is used when calling from function to function in the same module
     * We keep the same extensions and internalExtensions but drop in new variables
     */
    internal open fun createFunctionChildContext(newInternals: HashMap<String, AsyncContextAwareExtensionMethod>): IOperationContext {
        return clone(newInternals);
    }

    open fun clone(newInternals: HashMap<String, AsyncContextAwareExtensionMethod>): IOperationContext {
        // we also want to copy across all global variables
        val newContext = BaseOperationContext(
            this.extensions,
            this.annotations,
            this.statementExtensions,
            newInternals,
            this.conditionalExtensions,
//            this.interceptor
        );

        this.variables.filter { it.value.global }
            .forEach { newContext.setVariable(it.key, it.value) }

        return newContext;
    }

    fun useModuleFunctions(functionExtensions: HashMap<String, AsyncContextAwareExtensionMethod>): IOperationContext {
        this.internalExtensions = functionExtensions;
        // Clear cache when internal extensions change since cached lookups may now be invalid
        extensionCache.clear()
        return this;
    }

    /**
     * Register an annotation
     * */
    override fun registerAnnotation(
        annotationName: String,
        callback: AsyncExtensionAnnotation
    ): BaseOperationContext {
        annotations[annotationName.lowercase()] = { context -> callback(context); };
        return this;
    }

    override fun registerExtensionMethod(
        fullName: String,
        callback: AsyncContextAwareExtensionMethod
    ): BaseOperationContext {
        extensions[fullName.lowercase()] = callback;
        return this;
    }

    override fun registerConditionalExtensionMethod(
        fullName: String,
        extension: ConditionalExtension
    ): IOperationContext {
        conditionalExtensions[fullName.lowercase()] = extension;
        return this;
    }

    override fun registerStatementMethod(
        fullName: String,
        callback: AsyncStatementsExtensionMethod
    ): BaseOperationContext {
        statementExtensions[fullName.lowercase()] = callback;
        return this;
    }

    /**
     * Returns an extension method based on its full name (Case Insensitive)
     * Performance: Uses cache to avoid repeated HashMap traversals
     * Note: Only caches non-null results to allow dynamic extension registration
     */
    override fun getExtension(name: String): AsyncContextAwareExtensionMethod? {
        // Check cache first
        val cached = extensionCache[name]
        if (cached != null) return cached
        
        // Lookup in extension maps
        val result = internalExtensions[name] ?: extensions[name] ?: RootOperationContext.getExtension(name)
        
        // Only cache non-null results to allow for dynamic extension registration
        if (result != null) {
            extensionCache[name] = result
        }
        
        return result
    }

    override fun getConditionalExtension(name: String): ConditionalExtension? {
        // Performance optimization: assume name is already lowercase
        return conditionalExtensions[name] ?: RootOperationContext.getConditionalExtension(name);
    }

    override fun getAnnotation(annotationName: String): AsyncExtensionAnnotation? {
        // Performance optimization: assume name is already lowercase
        return annotations[annotationName] ?: RootOperationContext.getAnnotations(annotationName);
    }

    override fun getStatementExtension(name: String): AsyncStatementsExtensionMethod? {
        // Performance optimization: assume name is already lowercase
        return statementExtensions[name] ?: RootOperationContext.getStatementExtension(name);
    }

    /**
     * Add @param name Variable. Name will be visible as `$name` in the transform code.
     * Name needs to start with a single `$`
     */
    override fun setVariable(name: String, node: JsonNode, setIsModified: Boolean?): BaseOperationContext {
        // @ is used for now for some internal variables just to keep track of internal stuff
        assert(name.startsWith("$"));

        val lname = name.lowercase();
        // if variable exists and it's readonly then don't set
        val existing = variables[lname];
        if (existing?.readOnly == true)
            throw Exception("Could not set readonly variable=${name}.")

        variables[lname] = TransformVariable(node);

        return this;
    }

    override fun setVariable(name: String, variable: TransformVariable): BaseOperationContext {
        // @ is used for now for some internal variables just to keep track of internal stuff
        assert(name.startsWith("$"));
        val lname = name.lowercase();
        // if variable exists and it's readonly then don't set
        val existing = variables[lname];
        if (existing?.readOnly == true)
            throw Exception("Could not set readonly variable=${name}.")
        variables[lname] = variable;

        return this;
    }

    override fun getVariable(name: String): JsonNode? {
        return variables[name.lowercase()]?.value;
    }

    override fun getTransformVariable(name: String): TransformVariable? {
        return variables[name.lowercase()];
    }

    override fun removeVariable(name: String) {
        variables.remove(name.lowercase());
    }
}

