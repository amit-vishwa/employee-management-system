package com.amit.ems.employeeservice.strategy;

import com.amit.ems.employeeservice.entity.Employee;

import java.util.List;

public interface EmployeeSearchStrategy {
    List<Employee> search(String value);
}