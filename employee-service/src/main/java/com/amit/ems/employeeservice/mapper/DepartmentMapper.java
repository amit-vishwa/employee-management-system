package com.amit.ems.employeeservice.mapper;

import com.amit.ems.employeeservice.dto.DepartmentDto;
import com.amit.ems.employeeservice.entity.Department;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {

    public DepartmentDto toDto(Department department) {
        return DepartmentDto.builder()
                .id(department.getId())
                .name(department.getName())
                .build();
    }

    public Department toEntity(DepartmentDto dto) {
        return Department.builder()
                .id(dto.getId())
                .name(dto.getName())
                .build();
    }
}