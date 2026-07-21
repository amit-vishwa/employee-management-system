package com.amit.ems.employeeservice.repository;

import com.amit.ems.employeeservice.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
}