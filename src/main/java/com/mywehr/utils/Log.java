package com.mywehr.utils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Deliberately dependency-free logger.
 *
 * A Selenium suite's console output is read live while tests run, so the format
 * is optimised for scanning: time, level, thread, message. Thread name matters
 * because parallel suites interleave output.
 */
public final class Log {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private Log() {
    }

    public static void info(String message) {
        write("INFO ", message);
    }

    public static void step(String message) {
        write("STEP ", message);
    }

    public static void pass(String message) {
        write("PASS ", message);
    }

    public static void warn(String message) {
        write("WARN ", message);
    }

    public static void error(String message) {
        write("ERROR", message);
    }

    public static void error(String message, Throwable throwable) {
        write("ERROR", message + " -> " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
    }

    private static void write(String level, String message) {
        System.out.printf("%s | %s | %-22s | %s%n",
                LocalTime.now().format(TIME),
                level,
                Thread.currentThread().getName(),
                message);
    }
}
