package com.amit.ems.common.web;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Stream;

import static com.amit.ems.common.web.CorrelationIdFilter.HEADER_NAME;
import static com.amit.ems.common.web.CorrelationIdFilter.MDC_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CorrelationIdFilterTest {

    private static final String UNRELATED_MDC_KEY = "testContext";
    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @BeforeEach
    void setUpMdc() {
        MDC.clear();
        MDC.put(UNRELATED_MDC_KEY, "keep-me");
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @ParameterizedTest
    @MethodSource("validCorrelationIds")
    void shouldPreserveValidCorrelationIds(String suppliedId)
            throws ServletException, IOException {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        request.addHeader(HEADER_NAME, suppliedId);

        filter.doFilter(request, response, (req, res) -> {
            assertEquals(suppliedId, response.getHeader(HEADER_NAME));
            assertEquals(suppliedId, MDC.get(MDC_KEY));
        });

        assertEquals(suppliedId, response.getHeader(HEADER_NAME));
        assertCleanedCorrelationContext();
    }

    static Stream<String> validCorrelationIds() {
        return Stream.of("a", "Trace_42.abc-XYZ", "a".repeat(64));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @MethodSource("invalidCorrelationIds")
    void shouldGenerateUuidForMissingOrInvalidCorrelationIds(String suppliedId)
            throws ServletException, IOException {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        if (suppliedId != null) {
            request.addHeader(HEADER_NAME, suppliedId);
        }

        filter.doFilter(request, response, (req, res) -> {
            String chosenId = response.getHeader(HEADER_NAME);
            assertNotNull(chosenId);
            assertEquals(chosenId, MDC.get(MDC_KEY));
        });

        String chosenId = response.getHeader(HEADER_NAME);
        assertNotNull(chosenId);
        assertEquals(chosenId, UUID.fromString(chosenId).toString());
        assertCleanedCorrelationContext();
    }

    static Stream<String> invalidCorrelationIds() {
        return Stream.of("bad@id", "bad id", "a".repeat(65));
    }

    @Test
    void shouldCleanUpMdcAndPropagateDownstreamFailure() {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        request.addHeader(HEADER_NAME, "failure-trace");
        var downstreamFailure = new ServletException("downstream failed");

        ServletException actualFailure = assertThrows(
                ServletException.class,
                () -> filter.doFilter(request, response, (req, res) -> {
                    assertEquals("failure-trace", response.getHeader(HEADER_NAME));
                    assertEquals("failure-trace", MDC.get(MDC_KEY));
                    throw downstreamFailure;
                })
        );

        assertSame(downstreamFailure, actualFailure);
        assertCleanedCorrelationContext();
    }

    private void assertCleanedCorrelationContext() {
        assertNull(MDC.get(MDC_KEY));
        assertEquals("keep-me", MDC.get(UNRELATED_MDC_KEY));
    }
}
