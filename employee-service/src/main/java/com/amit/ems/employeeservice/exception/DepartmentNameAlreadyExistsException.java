package com.amit.ems.employeeservice.exception;

public class DepartmentNameAlreadyExistsException extends RuntimeException {
    public DepartmentNameAlreadyExistsException() {
        super("A department with that name already exists");
    }
}
