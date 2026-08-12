package com.amit.ems.employeeservice.mapper;

import com.amit.ems.employeeservice.dto.EmployeeDto;
import com.amit.ems.employeeservice.entity.Department;
import com.amit.ems.employeeservice.entity.Employee;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public EmployeeDto toDto(Employee employee) {
        return EmployeeDto.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .authUsername(employee.getAuthUsername())
                .designation(employee.getDesignation())
                .dateOfJoining(employee.getDateOfJoining())
                .departmentId(employee.getDepartment() != null ? employee.getDepartment().getId() : null)
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .build();
    }

    public Employee toEntity(EmployeeDto dto, Department department) {
        return Employee.builder()
                .id(dto.getId())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .authUsername(dto.getAuthUsername())
                .designation(dto.getDesignation())
                .dateOfJoining(dto.getDateOfJoining())
                .department(department)
                .build();
    }
}