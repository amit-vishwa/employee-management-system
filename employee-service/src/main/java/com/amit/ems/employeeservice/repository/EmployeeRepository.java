package com.amit.ems.employeeservice.repository;

import com.amit.ems.employeeservice.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByDesignation(String designation);
    List<Employee> findByDepartmentName(String departmentName);
}