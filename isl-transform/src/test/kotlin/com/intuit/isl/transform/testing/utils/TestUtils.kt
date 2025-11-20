package com.intuit.isl.transform.testing.utils

import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

suspend fun <T> runLoop(runCount: Int, name: String, callback: suspend () -> T): T {
    val result = callback();

    println("Starting $name");

    if (runCount > 1) {
        var totalTime: Long = 0;
        val startTime = Instant.now();
        for (i in 1..runCount) {
            callback();

            if(i % 5000 == 0)
                println("Run=${i}");
        }
        val time = Duration.between(startTime, Instant.now());
        totalTime += time.toNanos();

        val avgNs = totalTime.toDouble() / runCount.toDouble();
        println(
            "Total Time to run $name x $runCount = ${TimeUnit.NANOSECONDS.toMillis(totalTime)}ms Avg/Run=${avgNs}ns or ${
                String.format("%.2f", (avgNs / 1_000_000))
            }ms"
        );
    }

    return result;
}