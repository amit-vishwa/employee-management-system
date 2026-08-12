package com.amit.ems.notificationservice.controller;

import com.amit.ems.notificationservice.dto.EmployeeCreatedEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@Slf4j
@Tag(
        name = "Notifications",
        description = "Internal employee-notification operations"
)
public class NotificationController {

    @PostMapping("/employee-created")
    @Operation(
            summary = "Process an employee-created notification",
            description = "Current implementation writes intended email activity to logs"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Notification processed successfully"
            )
    })
    public ResponseEntity<Void> notifyEmployeeCreated(@RequestBody EmployeeCreatedEvent event) {
        log.info("Notification: Welcome email would be sent to {} ({})",
                event.getEmployeeName(), event.getEmployeeEmail());
        return ResponseEntity.ok().build();
    }
}