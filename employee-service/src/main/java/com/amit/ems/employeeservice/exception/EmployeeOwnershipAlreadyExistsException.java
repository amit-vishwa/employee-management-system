package com.amit.ems.employeeservice.exception;

public class EmployeeOwnershipAlreadyExistsException
        extends RuntimeException {

    public EmployeeOwnershipAlreadyExistsException(
            String authUsername
    ) {
        super(
                "Employee record already linked to username: "
                        + authUsername
        );
    }
}