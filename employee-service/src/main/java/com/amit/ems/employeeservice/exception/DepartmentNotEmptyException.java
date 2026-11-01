package com.amit.ems.employeeservice.exception;

public class DepartmentNotEmptyException extends RuntimeException {
    public DepartmentNotEmptyException() {
        super("Cannot delete a department that contains employees");
    }
}
