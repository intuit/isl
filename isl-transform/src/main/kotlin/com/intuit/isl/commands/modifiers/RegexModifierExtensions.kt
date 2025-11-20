package com.intuit.isl.commands.modifiers

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.utils.ConvertUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

object RegexModifierExtensions {
    // Note - this class is using odd access to the Java Named Groups for RegEx that were only accessible through reflection before J20
    // but now they are accessible directly
    // based on what Java version we run we need to use one or the other API
    private val isJava20OrNewer: Boolean

    private val namedGroupsReader: (pattern: Pattern) -> Map<String, Int>?;

    init {
        val version = Runtime.Version.parse(System.getProperty("java.version"));
        isJava20OrNewer = version.feature() >= 20;
        // put this back if you're trying to cross-compile Java20+ vs older Java.
        // if (isJava20OrNewer) {
        //     println("Loaded ISL Version in Java 20 or newer. This version will use new access pattern for RegEx Named Groups.");
			
        // } else {
        //     println("Loaded ISL Version in Java 19 or older. This version will use old reflective access pattern for RegEx Named Groups.");
        // }

        if (isJava20OrNewer) {
            namedGroupsReader = { pattern -> pattern.namedGroups() }
            // unit tests run in 21 in Jenkins
            //namedGroupsReader = { pattern -> throw NotImplementedError("Named Group not supported in this version of ISL") }
        } else {
            namedGroupsReader = { pattern ->
                val field = pattern.javaClass.getDeclaredField("namedGroups");
                field.trySetAccessible();
                field.get(pattern) as? Map<String, Int>
            }
        }
    }

    fun registerDefaultExtensions(context: IOperationContext) {
        context.registerExtensionMethod("Modifier.regex.*", RegexModifierExtensions::regex);
    }

    // We need a cache here because the Pattern.compile does not cache so it's very expensive to create on every use
    private val cachedPatterns = ConcurrentHashMap<String, Pattern>();

    private fun getPattern(pattern: String, flags: Int): Pattern {
        val id = "$flags@$pattern";
        val existing = cachedPatterns[id];
        if (existing != null)
            return existing;

        // No, we're not bothered to lock here - the write is concurrent
        // there's a small chance we'll compile the same pattern twice but it's not worth locking for that
        val regex = Pattern.compile(pattern, flags);
        cachedPatterns[id] = regex;
        return regex
    }


    //REGEX
    // CT: As of Java21 we removed the reflective access
    private fun regex(context: FunctionExecuteContext): Any? {
        // $$text | regex.find(pattern, options) returns array of matched results
        // $$text | regex.matches(pattern, options) returns true or false
        // $$text | regex.replace(pattern, replacementText, options) returns string with replaced values
        //The available options are ignoreCase, multiLine and comments

        val text = ConvertUtils.tryToString(context.firstParameter) ?: "";
        val method = ConvertUtils.tryToString(context.secondParameter) ?: "";
        val pattern = ConvertUtils.tryToString(context.thirdParameter) ?: "";

        val result = JsonNodeFactory.instance.arrayNode();

        val options = if (method.lowercase() in arrayOf("find", "matches")) {
            context.fourthParameter as? ObjectNode;
        } else {
            context.fifthParameter as? ObjectNode;
        }

        val ignoreCase = (ConvertUtils.tryToString(options?.get("ignoreCase"))).toBoolean();
        val multiLine = (ConvertUtils.tryToString(options?.get("multiLine"))).toBoolean();
        val comments = (ConvertUtils.tryToString(options?.get("comments"))).toBoolean();

        var flags = 0;

        if (ignoreCase) flags = Pattern.CASE_INSENSITIVE or flags;
        if (multiLine) flags = Pattern.MULTILINE or flags;
        if (comments) flags = Pattern.COMMENTS or flags;

        val regex = getPattern(pattern, flags)

        val matcher = regex.matcher(text);

        when (method.lowercase()) {
            "find" -> {

                // If we're on Java20 or newer we need to call this directly - otherwise we'll use reflection
                val namedGroups = namedGroupsReader(regex);

                val c = matcher.groupCount()

                if (namedGroups.isNullOrEmpty()) {
                    if (c > 0) {
                        while (matcher.find()) {
                            var i = 1;
                            while (i <= c) {
                                result.add(matcher.group(i));
                                i += 1;
                            }
                        }
                    } else {
                        while (matcher.find()) result.add(matcher.group(0));
                    }
                } else {
                    while (matcher.find()) {
                        val matchObj = JsonNodeFactory.instance.objectNode();
                        namedGroups.forEach { (k) ->
                            val group = matcher.group(k);
                            if (!group.isNullOrBlank()) {
                                matchObj.put(k, group);
                            }
                        };
                        result.add(matchObj);
                    }
                }
                return result;
            };
            "matches" -> {
                return matcher.find();
            };
            "replace" -> {
                val replacementText = ConvertUtils.tryToString(context.fourthParameter) ?: ""

                return matcher.replaceAll(replacementText)
            }

            "replacefirst" -> {
                val replacementText = ConvertUtils.tryToString(context.fourthParameter) ?: ""

                return matcher.replaceFirst(replacementText)
            }
        }

        return "Unknown method regex.$method";
    }

}