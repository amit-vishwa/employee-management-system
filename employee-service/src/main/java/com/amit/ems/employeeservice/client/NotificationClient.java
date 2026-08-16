package com.amit.ems.employeeservice.client;

import com.amit.ems.common.web.CorrelationIdFilter;
import com.amit.ems.employeeservice.event.EmployeeCreatedEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationClient {

    private static final String CIRCUIT_BREAKER =
            "notificationService";

    private final RestTemplate restTemplate;

    @Value("${notification.service-url}")
    private String notificationServiceUrl;

    @CircuitBreaker(
            name = CIRCUIT_BREAKER,
            fallbackMethod = "handleFailure"
    )
    public void sendEmployeeCreated(
            EmployeeCreatedEvent event
    ) {
        HttpHeaders headers = new HttpHeaders();

        if (event.correlationId() != null) {
            headers.set(
                    CorrelationIdFilter.HEADER_NAME,
                    event.correlationId()
            );
        }

        EmployeeCreatedNotificationRequest body =
                new EmployeeCreatedNotificationRequest(
                        event.employeeEmail(),
                        event.employeeName()
                );

        HttpEntity<EmployeeCreatedNotificationRequest> request =
                new HttpEntity<>(body, headers);

        restTemplate.postForEntity(
                notificationServiceUrl,
                request,
                Void.class
        );
    }

    private void handleFailure(
            EmployeeCreatedEvent event,
            Throwable throwable
    ) {
        log.warn(
                "Employee-created notification was not delivered "
                        + "for email {}: {}",
                event.employeeEmail(),
                throwable.getMessage()
        );
    }

    private record EmployeeCreatedNotificationRequest(
            String employeeEmail,
            String employeeName
    ) {
    }
}