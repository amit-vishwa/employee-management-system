package com.amit.ems.employeeservice.event;

public record EmployeeCreatedEvent(
        String employeeEmail,
        String employeeName,
        String correlationId
) {
}