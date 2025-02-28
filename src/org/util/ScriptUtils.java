package org.util;

import java.util.function.Consumer;

public class ScriptUtils {
    private static Consumer<String> logger = System.out::println; // Default to System.out

    public static void setLogger(Consumer<String> logFunction) {
        logger = logFunction;
    }

    public static void log(String message) {
        logger.accept(message);
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            logger.accept("Sleep interrupted: " + e.getMessage());
        }
    }

    public static void sleep(int min, int max) {
        try {
            Thread.sleep(min + (int) (Math.random() * (max - min)));
        } catch (InterruptedException e) {
            logger.accept("Sleep interrupted: " + e.getMessage());
        }
    }
}
