package com.amit.ems.employeeservice.event;

import com.amit.ems.employeeservice.client.NotificationClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EmployeeCreatedEventListener {

    private final NotificationClient notificationClient;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(EmployeeCreatedEvent event) {
        notificationClient.sendEmployeeCreated(event);
    }
}