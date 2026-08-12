package com.amit.ems.employeeservice.exception;

public class EmployeeEmailAlreadyExistsException
        extends RuntimeException {

    public EmployeeEmailAlreadyExistsException(String email) {
        super("Employee email already exists: " + email);
    }
}