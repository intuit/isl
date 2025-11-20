package com.intuit.isl.transform.testing.java;

import com.intuit.isl.common.ExecutionContext;
import com.intuit.isl.common.ICommandInterceptor;
import com.intuit.isl.commands.IIslCommand;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Example of how to write an interceptor that can log various calls.
 * You can even modify the context or the arguments to the call if you are so inclined
 */
public class LogCommandInterceptor implements ICommandInterceptor {
    @Override
    public void logInfo(@NotNull IIslCommand command, @NotNull ExecutionContext context, @NotNull Function0<String> details) {
        System.out.println("LogInfo " + command + " Message: " + details.invoke());
    }

    @Override
    public void logError(@NotNull IIslCommand command, @NotNull ExecutionContext context, @NotNull Function0<String> details) {
        System.out.println("LogError " + command + " Message: " + details.invoke());
    }

    @Override
    public void onExecuting(@NotNull IIslCommand command, @NotNull ExecutionContext context, @NotNull Object[] arguments) {
        System.out.println("Executing " + command + " args: " + Arrays.toString(arguments));
    }

    @Override
    public void onIssue(@NotNull IIslCommand command, @NotNull ExecutionContext context, @NotNull String message, @NotNull Object[] arguments) {
        System.out.println("Issue " + command + " args: " + Arrays.toString(arguments) + " : " + message);
    }
}
