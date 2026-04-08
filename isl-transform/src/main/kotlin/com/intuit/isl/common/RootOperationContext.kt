package com.intuit.isl.common

import com.intuit.isl.commands.modifiers.*
import com.intuit.isl.utils.CryptoExtensions
import com.intuit.isl.utils.DateExtensions
import com.intuit.isl.utils.SystemExtensions
import com.intuit.isl.utils.zip.ZipExtensions
import com.intuit.isl.utils.annotations.CacheAnnotation
import com.intuit.isl.utils.pagination.CursorPagination
import com.intuit.isl.utils.pagination.DatePagination
import com.intuit.isl.utils.pagination.PagePagination
import com.intuit.isl.commands.modifiers.JsonModifierExtensions
import com.intuit.isl.commands.modifiers.RegexModifierExtensions
import com.intuit.isl.commands.modifiers.TypesModifierExtensions

/**
 * List of all the root ISL Operations
 */
object RootOperationContext {
    private val extensions: HashMap<String, ContextAwareExtensionMethod>;
    private val statementExtensions: HashMap<String, StatementsExtensionMethod>;
    private val annotations: HashMap<String, AsyncExtensionAnnotation>;
    private val conditionalExtensions: HashMap<String, ConditionalExtension>;

    init {
        val defaultContext = RootOperationContextRegistry();

        ModifiersExtensions.registerDefaultExtensions(defaultContext);
        TypesModifierExtensions.registerDefaultExtensions(defaultContext);
        XmlModifierExtensions.registerDefaultExtensions(defaultContext);
        JsonModifierExtensions.registerDefaultExtensions(defaultContext);
        DateExtensions.registerExtensions(defaultContext);
        CryptoExtensions.registerExtensions(defaultContext);
        SystemExtensions.registerExtensions(defaultContext);
        CacheAnnotation.registerInMemoryCacheAnnotation(defaultContext);
        ZipExtensions.registerExtensions(defaultContext);
        RetryModifiers.registerRetry(defaultContext);
        RegexModifierExtensions.registerDefaultExtensions(defaultContext);

        // Custom Pagination Extensions (now sync)
        defaultContext.registerStatementMethod("Pagination.Page", PagePagination::execute);
        defaultContext.registerStatementMethod("Pagination.Cursor", CursorPagination::execute);
        defaultContext.registerStatementMethod("Pagination.Date", DatePagination::execute);

        extensions = defaultContext.extensionsList();
        annotations = defaultContext.annotationsList();
        statementExtensions = defaultContext.statementExtensionsList();
        conditionalExtensions = defaultContext.conditionalExtensionsList();
    }

    fun getExtension(name: String): ContextAwareExtensionMethod? {
        return extensions[name];
    }
    fun getStatementExtension(name: String): StatementsExtensionMethod? {
        return statementExtensions[name];
    }
    fun getAnnotations(name: String): AsyncExtensionAnnotation? {
        return annotations[name];
    }
    fun getConditionalExtension(name: String): ConditionalExtension? {
        return conditionalExtensions[name];
    }

    private class RootOperationContextRegistry : BaseOperationContext() {
        fun extensionsList(): HashMap<String, ContextAwareExtensionMethod> {
            return super.extensions;
        }
        fun conditionalExtensionsList(): HashMap<String, ConditionalExtension> {
            return super.conditionalExtensions;
        }

        fun statementExtensionsList(): HashMap<String, StatementsExtensionMethod> {
            return super.statementExtensions;
        }

        fun annotationsList(): HashMap<String, AsyncExtensionAnnotation> {
            return super.annotations;
        }
    }
}