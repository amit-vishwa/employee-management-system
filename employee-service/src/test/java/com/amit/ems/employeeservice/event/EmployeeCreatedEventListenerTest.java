package com.amit.ems.employeeservice.event;

import com.amit.ems.employeeservice.client.NotificationClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmployeeCreatedEventListenerTest {

    private static final String EMPLOYEE_EMAIL =
            "john@example.com";

    private static final String EMPLOYEE_NAME =
            "John Doe";

    private static final String CORRELATION_ID =
            "test-correlation-id-001";

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private EmployeeCreatedEventListener listener;

    @Test
    void handle_shouldDelegateCompleteEventToNotificationClient() {
        EmployeeCreatedEvent event =
                new EmployeeCreatedEvent(
                        EMPLOYEE_EMAIL,
                        EMPLOYEE_NAME,
                        CORRELATION_ID
                );

        listener.handle(event);

        verify(notificationClient)
                .sendEmployeeCreated(event);
    }
}