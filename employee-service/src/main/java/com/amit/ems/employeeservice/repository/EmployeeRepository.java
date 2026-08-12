package com.amit.ems.employeeservice.repository;

import com.amit.ems.employeeservice.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository
        extends JpaRepository<Employee, Long> {

    List<Employee> findByDesignation(String designation);

    List<Employee> findByDepartmentName(String departmentName);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<Employee> findByAuthUsername(String authUsername);

    boolean existsByAuthUsername(String authUsername);

    boolean existsByAuthUsernameAndIdNot(
            String authUsername,
            Long id
    );
}