package com.amit.ems.common.logging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LogSanitizerTest {

    @Test
    void shouldReturnNullWhenValueIsNull() {
        assertNull(LogSanitizer.sanitize(null));
    }

    @Test
    void shouldReturnUnchangedValueWithoutLineBreaks() {
        assertEquals(
                "employee@example.com",
                LogSanitizer.sanitize("employee@example.com")
        );
    }

    @Test
    void shouldReplaceCarriageReturnsAndLineFeeds() {
        assertEquals(
                "attacker__FORGED_LOG_ENTRY",
                LogSanitizer.sanitize("attacker\r\nFORGED_LOG_ENTRY")
        );
    }
}