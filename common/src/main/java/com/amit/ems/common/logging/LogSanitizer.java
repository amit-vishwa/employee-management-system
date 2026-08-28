package com.amit.ems.common.logging;

public final class LogSanitizer {

    private LogSanitizer() {
    }

    public static String sanitize(String value) {
        if (value == null) {
            return null;
        }

        return value
                .replace('\r', '_')
                .replace('\n', '_');
    }
}