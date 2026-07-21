package com.amit.ems.employeeservice.strategy;

import com.amit.ems.employeeservice.entity.Employee;
import com.amit.ems.employeeservice.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("designation")
@RequiredArgsConstructor
public class DesignationSearchStrategy implements EmployeeSearchStrategy {

    private final EmployeeRepository employeeRepository;

    @Override
    public List<Employee> search(String value) {
        return employeeRepository.findByDesignation(value);
    }
}