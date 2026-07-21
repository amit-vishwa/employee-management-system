package com.amit.ems.employeeservice.service;

import com.amit.ems.employeeservice.dto.DepartmentDto;

import java.util.List;

public interface DepartmentService {
    DepartmentDto createDepartment(DepartmentDto dto);
    DepartmentDto getDepartmentById(Long id);
    List<DepartmentDto> getAllDepartments();
    DepartmentDto updateDepartment(Long id, DepartmentDto dto);
    void deleteDepartment(Long id);
}