package com.amit.ems.employeeservice.service;

import com.amit.ems.employeeservice.dto.EmployeeDto;

import java.util.List;

public interface EmployeeService {
    EmployeeDto createEmployee(EmployeeDto dto);
    EmployeeDto getEmployeeById(Long id);
    List<EmployeeDto> getAllEmployees();
    EmployeeDto updateEmployee(Long id, EmployeeDto dto);
    void deleteEmployee(Long id);
    List<EmployeeDto> searchEmployees(String criteria, String value);
}