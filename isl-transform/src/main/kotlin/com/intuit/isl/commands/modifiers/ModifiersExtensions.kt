package com.intuit.isl.commands.modifiers

import com.intuit.isl.common.IOperationContext

/**
 * Central registration point for all default ISL modifiers.
 * 
 * This object delegates to specialized extension modules organized by type:
 * - Math operations (MathModifierExtensions)
 * - String operations (StringModifierExtensions)
 * - Type conversions (ConversionModifierExtensions)
 * - Encoding/decoding (EncodingModifierExtensions)
 * - Object operations (ObjectModifierExtensions)
 * - Array operations (ArrayModifierExtensions)
 * - Compression (CompressionModifierExtensions)
 * - JSON operations (JsonModifierExtensions)
 * - XML operations (XmlModifierExtensions)
 * - Regex operations (RegexModifierExtensions)
 * - Type checking (TypesModifierExtensions)
 * - Conditional modifiers (ConditionalModifierCommand, FilterModifierValueCommand, ReduceModifierValueCommand)
 * - Retry modifiers (RetryModifiers)
 */
@Suppress("MoveVariableDeclarationIntoWhen")
object ModifiersExtensions {
    
    /**
     * Register all default ISL modifiers with the given operation context.
     */
    fun registerDefaultExtensions(context: IOperationContext) {
        // Register modifiers from specialized extension modules
        MathModifierExtensions.registerExtensions(context)
        StringModifierExtensions.registerExtensions(context)
        ConversionModifierExtensions.registerExtensions(context)
        EncodingModifierExtensions.registerExtensions(context)
        ObjectModifierExtensions.registerExtensions(context)
        ArrayModifierExtensions.registerExtensions(context)
        CompressionModifierExtensions.registerExtensions(context)       
        JsonModifierExtensions.registerDefaultExtensions(context)
        XmlModifierExtensions.registerDefaultExtensions(context)
        RegexModifierExtensions.registerDefaultExtensions(context)
        TypesModifierExtensions.registerDefaultExtensions(context)
        
        // Note: |filter, |reduce, |map are implemented in dedicated command classes:
        // - FilterModifierValueCommand
        // - ReduceModifierValueCommand  
        // - MapModifierValueCommand
    }
}
