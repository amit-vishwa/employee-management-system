package com.amit.ems.notificationservice.dto;

import lombok.Data;

@Data
public class EmployeeCreatedEvent {
    private String employeeEmail;
    private String employeeName;
}