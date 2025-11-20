package com.intuit.isl.commands.modifiers

import com.fasterxml.jackson.databind.node.ArrayNode
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.getValueOrDefault
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.log
import kotlin.math.pow

/**
 * Math and numeric modifier extensions for ISL.
 * 
 * Provides modifiers for mathematical operations:
 * - negate, absolute, precision, round
 * - min, max, mean, mod, sqrt
 * - Random number generation (RandInt, RandFloat, RandDouble)
 */
object MathModifierExtensions {
    
    fun registerExtensions(context: IOperationContext) {
        // Math modifiers (Modifier.* prefix for pipe usage)
        context.registerExtensionMethod("Modifier.negate", MathModifierExtensions::negate)
        context.registerExtensionMethod("Modifier.precision", MathModifierExtensions::precision)
        context.registerExtensionMethod("Modifier.round.*", MathModifierExtensions::round)
        context.registerExtensionMethod("Modifier.absolute", MathModifierExtensions::absolute)
        
        // Math namespace functions (Math.* prefix for @.Math.function() usage)
        context.registerExtensionMethod("Math.min", MathModifierExtensions::min)
        context.registerExtensionMethod("Math.max", MathModifierExtensions::max)
        context.registerExtensionMethod("Math.mean", MathModifierExtensions::mean)
        context.registerExtensionMethod("Math.mod", MathModifierExtensions::mod)
        context.registerExtensionMethod("Math.sqrt", MathModifierExtensions::sqrt)
        context.registerExtensionMethod("Math.RandInt", MathModifierExtensions::randInt)
        context.registerExtensionMethod("Math.RandFloat", MathModifierExtensions::randFloat)
        context.registerExtensionMethod("Math.RandDouble", MathModifierExtensions::randDouble)
        context.registerExtensionMethod("Math.clamp", MathModifierExtensions::clamp)
        context.registerExtensionMethod("Math.sum", MathModifierExtensions::sum)
        context.registerExtensionMethod("Math.log", MathModifierExtensions::log)
        context.registerExtensionMethod("Math.log10", MathModifierExtensions::log10)
        context.registerExtensionMethod("Math.ln", MathModifierExtensions::ln)
        context.registerExtensionMethod("Math.pow", MathModifierExtensions::pow)
        
        // Also register with Modifier.Math.* prefix for pipe usage
        context.registerExtensionMethod("Modifier.Math.*", MathModifierExtensions::mathPipeDispatcher)
    }
    
    /**
     * Dispatcher for Math.* modifiers when used with pipes
     * When called as: [10, 20, 30] | Math.sum
     * - firstParameter = the array [10, 20, 30]
     * - secondParameter = "sum" (the wildcard part)
     * - remaining parameters shift by one
     */
    private fun mathPipeDispatcher(context: FunctionExecuteContext): Any? {
        val mathFunction = ConvertUtils.tryToString(context.secondParameter) ?: return null
        
        // Create a new context with shifted parameters for the actual math function
        // The math function expects the value as firstParameter, not secondParameter
        val shiftedParams = mutableListOf<Any?>()
        shiftedParams.add(context.firstParameter)
        shiftedParams.addAll(context.parameters.drop(2))
        val shiftedContext = context.copy(parameters = shiftedParams.toTypedArray())
        
        return when {
            mathFunction.equals("min", ignoreCase = true) -> min(shiftedContext)
            mathFunction.equals("max", ignoreCase = true) -> max(shiftedContext)
            mathFunction.equals("mean", ignoreCase = true) -> mean(shiftedContext)
            mathFunction.equals("mod", ignoreCase = true) -> mod(shiftedContext)
            mathFunction.equals("sqrt", ignoreCase = true) -> sqrt(shiftedContext)
            mathFunction.equals("RandInt", ignoreCase = true) -> randInt(shiftedContext)
            mathFunction.equals("RandFloat", ignoreCase = true) -> randFloat(shiftedContext)
            mathFunction.equals("RandDouble", ignoreCase = true) -> randDouble(shiftedContext)
            mathFunction.equals("clamp", ignoreCase = true) -> clamp(shiftedContext)
            mathFunction.equals("sum", ignoreCase = true) -> sum(shiftedContext)
            mathFunction.equals("log", ignoreCase = true) -> log(shiftedContext)
            mathFunction.equals("log10", ignoreCase = true) -> log10(shiftedContext)
            mathFunction.equals("ln", ignoreCase = true) -> ln(shiftedContext)
            mathFunction.equals("pow", ignoreCase = true) -> pow(shiftedContext)
            else -> null
        }
    }
    
    private fun negate(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val value = ConvertUtils.tryParseDecimal(first)
        
        if (value.getValueOrDefault() == BigDecimal.ZERO)
            return value
        
        return value?.negate()
    }
    
    private fun absolute(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val value = ConvertUtils.tryParseDecimal(first)
        
        if (value.getValueOrDefault() == BigDecimal.ZERO)
            return value
        
        return value?.abs()
    }
    
    private fun precision(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val value = ConvertUtils.tryParseDecimal(first) ?: BigDecimal.ZERO
        var precision = ConvertUtils.tryParseInt(context.secondParameter) ?: 2
        if (precision < 0)
            precision = 0
        
        // roundMode - does not seem to be used anywhere - we should avoid that in preference to | round:up or | round:down
        val newValue = value.setScale(precision, RoundingMode.HALF_EVEN)
        return newValue
    }
    
    private fun round(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val value = ConvertUtils.tryParseDecimal(first) ?: BigDecimal.ZERO
        
        val roundMethod = context.secondParameter?.toString() ?: "down"
        
        var precision = ConvertUtils.tryParseInt(context.thirdParameter) ?: 2
        if (precision < 0)
            precision = 0
        
        return when (roundMethod) {
            "up" -> value.setScale(precision, RoundingMode.UP)
            "down" -> value.setScale(precision, RoundingMode.DOWN)
            "ceiling" -> value.setScale(precision, RoundingMode.CEILING)
            "floor" -> value.setScale(precision, RoundingMode.FLOOR)
            else -> value.setScale(precision, RoundingMode.DOWN)
        }
    }
    
    private fun min(context: FunctionExecuteContext): Any? {
        val inputs: List<BigDecimal>? = tryConvertContextParamsToBigDecimalList(context.parameters)
        return inputs?.minByOrNull { it.toDouble() }
    }
    
    private fun max(context: FunctionExecuteContext): Any? {
        val inputs: List<BigDecimal>? = tryConvertContextParamsToBigDecimalList(context.parameters)
        return inputs?.maxByOrNull { it.toDouble() }
    }
    
    private fun mean(context: FunctionExecuteContext): Any? {
        val inputs: List<BigDecimal>? = tryConvertContextParamsToBigDecimalList(context.parameters)
        val partial: Double? = inputs?.map { it.toDouble() }?.average()
        return BigDecimal(partial.toString()).setScale(2, RoundingMode.DOWN)
    }
    
    private fun mod(context: FunctionExecuteContext): Any? {
        val first = ConvertUtils.tryParseLong(context.firstParameter) ?: 0
        val second = ConvertUtils.tryParseLong(context.parameters[1] ?: 0) ?: 0
        
        val m = first.mod(second)
        return m
    }
    
    private fun sqrt(context: FunctionExecuteContext): Any {
        val first = (ConvertUtils.tryParseDecimal(context.firstParameter) ?: 0).toDouble()
        if (first <= 0)
            return 0
        return kotlin.math.sqrt(first)
    }
    
    private fun randInt(context: FunctionExecuteContext): Any {
        var from = ConvertUtils.tryParseInt(context.firstParameter)
        val until = ConvertUtils.tryParseInt(context.secondParameter)
        
        var result = 0
        if (from == null)
            result = kotlin.random.Random.nextInt()
        else if (until != null) {
            result = if (from == until)
                kotlin.random.Random.nextInt(from)
            else if (from > until)
                kotlin.random.Random.nextInt(until, from)
            else
                kotlin.random.Random.nextInt(from, until)
        } else {
            if (from < 0) {
                from = kotlin.math.abs(from)
                result = -1 * kotlin.random.Random.nextInt(from)
            } else if (from != 0)
                result = kotlin.random.Random.nextInt(from)
        }
        
        return result
    }
    
    @Suppress("UNUSED_PARAMETER")
    private fun randFloat(context: FunctionExecuteContext): Any {
        return kotlin.random.Random.nextFloat()
    }
    
    private fun randDouble(context: FunctionExecuteContext): Any {
        var from = ConvertUtils.tryParseDecimal(context.firstParameter)?.toDouble()
        val until = ConvertUtils.tryParseDecimal(context.secondParameter)?.toDouble()
        
        var result = 0.0
        if (from == null)
            result = kotlin.random.Random.nextDouble()
        else if (until != null) {
            result = if (from == until)
                kotlin.random.Random.nextDouble(from)
            else if (from > until)
                kotlin.random.Random.nextDouble(until, from)
            else
                kotlin.random.Random.nextDouble(from, until)
        } else {
            if (from < 0) {
                from = kotlin.math.abs(from)
                result = -1.0 * kotlin.random.Random.nextDouble(from)
            } else if (from != 0.0)
                result = kotlin.random.Random.nextDouble(from)
        }
        
        return result
    }
    
    /**
     * Clamp a number between min and max values
     */
    private fun clamp(context: FunctionExecuteContext): Any? {
        val value = ConvertUtils.tryParseDecimal(context.firstParameter) ?: return null
        val min = ConvertUtils.tryParseDecimal(context.secondParameter) ?: return value
        val max = ConvertUtils.tryParseDecimal(context.thirdParameter) ?: return value
        
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }
    
    /**
     * Sum an array of numbers, with optional initial value
     * Usage: $array | Math.sum() or $array | Math.sum(initialValue)
     */
    private fun sum(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        
        // If first parameter is an array, sum its elements
        if (first is ArrayNode) {
            val inputs = ConvertUtils.tryToNumbersList(first)
            if (inputs.isNullOrEmpty()) {
                // Return the initial value if provided, otherwise 0
                val initialValue = context.secondParameter?.let { ConvertUtils.tryParseDecimal(it) }
                return initialValue ?: BigDecimal.ZERO
            }
            
            val initialValue = context.secondParameter?.let { ConvertUtils.tryParseDecimal(it) } ?: BigDecimal.ZERO
            return inputs.fold(initialValue) { acc, num -> acc + num }
        }
        
        // Otherwise, try to convert all parameters to numbers and sum them
        val inputs: List<BigDecimal>? = tryConvertContextParamsToBigDecimalList(context.parameters)
        if (inputs.isNullOrEmpty()) return BigDecimal.ZERO
        
        return inputs.reduce { acc, num -> acc + num }
    }
    
    /**
     * Natural logarithm (base e)
     */
    private fun ln(context: FunctionExecuteContext): Any {
        val value = (ConvertUtils.tryParseDecimal(context.firstParameter) ?: BigDecimal.ZERO).toDouble()
        if (value <= 0) return Double.NaN
        return ln(value)
    }
    
    /**
     * Logarithm base 10
     */
    private fun log10(context: FunctionExecuteContext): Any {
        val value = (ConvertUtils.tryParseDecimal(context.firstParameter) ?: BigDecimal.ZERO).toDouble()
        if (value <= 0) return Double.NaN
        return log10(value)
    }
    
    /**
     * Logarithm with custom base (defaults to base e if not specified)
     */
    private fun log(context: FunctionExecuteContext): Any {
        val value = (ConvertUtils.tryParseDecimal(context.firstParameter) ?: BigDecimal.ZERO).toDouble()
        if (value <= 0) return Double.NaN
        
        val base = context.secondParameter?.let { 
            (ConvertUtils.tryParseDecimal(it) ?: BigDecimal.ZERO).toDouble()
        }
        
        return if (base != null && base > 0 && base != 1.0) {
            log(value, base)
        } else {
            ln(value)
        }
    }
    
    /**
     * Power function: value^exponent
     */
    private fun pow(context: FunctionExecuteContext): Any {
        val base = (ConvertUtils.tryParseDecimal(context.firstParameter) ?: BigDecimal.ZERO).toDouble()
        val exponent = (ConvertUtils.tryParseDecimal(context.secondParameter) ?: BigDecimal.ONE).toDouble()
        
        return base.pow(exponent)
    }
    
    /**
     * Helper function to convert context parameters to a list of BigDecimal values.
     */
    private fun tryConvertContextParamsToBigDecimalList(parameters: Array<*>): List<BigDecimal>? {
        val first = parameters.elementAtOrNull(0)
        var inputs: List<BigDecimal>? = null
        if (first is com.fasterxml.jackson.databind.node.ArrayNode) {
            inputs = ConvertUtils.tryToNumbersList(first)
        } else {
            val value = ConvertUtils.tryParseDecimal(first)
            if (value is BigDecimal)
                inputs = parameters.map { (BigDecimal(it.toString())) }
        }
        return inputs
    }
}

