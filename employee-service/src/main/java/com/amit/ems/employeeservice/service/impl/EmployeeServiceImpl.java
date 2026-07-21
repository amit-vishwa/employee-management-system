package com.amit.ems.employeeservice.service.impl;

import com.amit.ems.common.exception.ResourceNotFoundException;
import com.amit.ems.employeeservice.dto.EmployeeDto;
import com.amit.ems.employeeservice.entity.Department;
import com.amit.ems.employeeservice.entity.Employee;
import com.amit.ems.employeeservice.mapper.EmployeeMapper;
import com.amit.ems.employeeservice.repository.DepartmentRepository;
import com.amit.ems.employeeservice.repository.EmployeeRepository;
import com.amit.ems.employeeservice.service.EmployeeService;
import com.amit.ems.employeeservice.strategy.EmployeeSearchStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeMapper employeeMapper;
    private final Map<String, EmployeeSearchStrategy> searchStrategies;

    @Override
    public EmployeeDto createEmployee(EmployeeDto dto) {
        log.info("Creating employee with email: {}", dto.getEmail());
        Department department = resolveDepartment(dto.getDepartmentId());
        Employee employee = employeeMapper.toEntity(dto, department);
        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toDto(saved);
    }

    @Override
    public EmployeeDto getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return employeeMapper.toDto(employee);
    }

    @Override
    public List<EmployeeDto> getAllEmployees() {
        return employeeRepository.findAll()
                .stream()
                .map(employeeMapper::toDto)
                .toList();
    }

    @Override
    public EmployeeDto updateEmployee(Long id, EmployeeDto dto) {
        Employee existing = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        Department department = resolveDepartment(dto.getDepartmentId());

        existing.setFirstName(dto.getFirstName());
        existing.setLastName(dto.getLastName());
        existing.setEmail(dto.getEmail());
        existing.setDesignation(dto.getDesignation());
        existing.setDateOfJoining(dto.getDateOfJoining());
        existing.setDepartment(department);

        Employee updated = employeeRepository.save(existing);
        return employeeMapper.toDto(updated);
    }

    @Override
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employee not found with id: " + id);
        }
        employeeRepository.deleteById(id);
        log.info("Deleted employee with id: {}", id);
    }

    private Department resolveDepartment(Long departmentId) {
        if (departmentId == null) return null;
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));
    }

    @Override
    public List<EmployeeDto> searchEmployees(String criteria, String value) {
        EmployeeSearchStrategy strategy = searchStrategies.get(criteria.toLowerCase());
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported search criteria: " + criteria);
        }
        return strategy.search(value)
                .stream()
                .map(employeeMapper::toDto)
                .toList();
    }
}