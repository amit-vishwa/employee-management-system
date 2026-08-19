package com.amit.ems.employeeservice.client;

import com.amit.ems.common.web.CorrelationIdFilter;
import com.amit.ems.employeeservice.event.EmployeeCreatedEvent;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest(properties = {
        "resilience4j.circuitbreaker.instances."
                + "notificationService.sliding-window-size=2",
        "resilience4j.circuitbreaker.instances."
                + "notificationService.minimum-number-of-calls=2",
        "resilience4j.circuitbreaker.instances."
                + "notificationService.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances."
                + "notificationService.wait-duration-in-open-state=1m",
        "resilience4j.circuitbreaker.instances."
                + "notificationService."
                + "automatic-transition-from-open-to-half-open-enabled=false"
})
@ActiveProfiles("test")
class NotificationClientCircuitBreakerTest {

    private static final String NOTIFICATION_URL =
            "http://localhost:8083/api/v1/"
                    + "notifications/employee-created";

    @Autowired
    private NotificationClient notificationClient;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private MockRestServiceServer server;
    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        server = MockRestServiceServer
                .bindTo(restTemplate)
                .build();

        circuitBreaker = circuitBreakerRegistry
                .circuitBreaker("notificationService");

        circuitBreaker.reset();
    }

    @AfterEach
    void verifyServerExpectations() {
        server.verify();
    }

    @Test
    void sendEmployeeCreated_shouldSendPayloadAndCorrelationId() {
        EmployeeCreatedEvent event =
                new EmployeeCreatedEvent(
                        "employee@example.com",
                        "Employee Example",
                        "notification-client-test-001"
                );

        server.expect(requestTo(NOTIFICATION_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(
                        CorrelationIdFilter.HEADER_NAME,
                        "notification-client-test-001"
                ))
                .andExpect(content().json(
                        """
                        {
                          "employeeEmail":
                              "employee@example.com",
                          "employeeName":
                              "Employee Example"
                        }
                        """
                ))
                .andRespond(withSuccess());

        notificationClient.sendEmployeeCreated(event);

        assertThat(circuitBreaker.getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void repeatedFailures_shouldOpenCircuitAndStopHttpCalls() {
        EmployeeCreatedEvent event =
                new EmployeeCreatedEvent(
                        "failure@example.com",
                        "Failure Example",
                        "notification-failure-test-001"
                );

        server.expect(
                        times(2),
                        requestTo(NOTIFICATION_URL)
                )
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        notificationClient.sendEmployeeCreated(event);
        notificationClient.sendEmployeeCreated(event);

        assertThat(circuitBreaker.getState())
                .isEqualTo(CircuitBreaker.State.OPEN);

        /*
         * The third call is rejected by the open circuit and handled
         * by NotificationClient's fallback. MockRestServiceServer
         * expects only two HTTP calls, proving that no third network
         * request occurs.
         */
        notificationClient.sendEmployeeCreated(event);

        assertThat(circuitBreaker.getState())
                .isEqualTo(CircuitBreaker.State.OPEN);
    }
}