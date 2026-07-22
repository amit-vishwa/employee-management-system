package com.amit.ems.notificationservice.controller;

import com.amit.ems.notificationservice.dto.EmployeeCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@Slf4j
public class NotificationController {

    @PostMapping("/employee-created")
    public ResponseEntity<Void> notifyEmployeeCreated(@RequestBody EmployeeCreatedEvent event) {
        log.info("Notification: Welcome email would be sent to {} ({})",
                event.getEmployeeName(), event.getEmployeeEmail());
        return ResponseEntity.ok().build();
    }
}