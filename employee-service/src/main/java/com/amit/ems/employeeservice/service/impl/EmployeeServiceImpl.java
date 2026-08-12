package com.amit.ems.employeeservice.service.impl;

import com.amit.ems.common.exception.ResourceNotFoundException;
import com.amit.ems.employeeservice.dto.EmployeeDto;
import com.amit.ems.employeeservice.entity.Department;
import com.amit.ems.employeeservice.entity.Employee;
import com.amit.ems.employeeservice.exception.EmployeeEmailAlreadyExistsException;
import com.amit.ems.employeeservice.exception.EmployeeOwnershipAlreadyExistsException;
import com.amit.ems.employeeservice.mapper.EmployeeMapper;
import com.amit.ems.employeeservice.repository.DepartmentRepository;
import com.amit.ems.employeeservice.repository.EmployeeRepository;
import com.amit.ems.employeeservice.service.EmployeeService;
import com.amit.ems.employeeservice.strategy.EmployeeSearchStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
    private final RestTemplate restTemplate;

    @Value("${notification.service-url}")
    private String notificationServiceUrl;

    @Override
    public EmployeeDto createEmployee(EmployeeDto dto) {
        log.info("Creating employee with email: {}", dto.getEmail());

        ensureEmailAvailableForCreate(dto.getEmail());
        ensureOwnershipAvailableForCreate(dto.getAuthUsername());

        Department department = resolveDepartment(dto.getDepartmentId());
        Employee employee = employeeMapper.toEntity(dto, department);

        Employee saved;

        try {
            saved = employeeRepository.save(employee);
        } catch (DataIntegrityViolationException exception) {
            log.warn(
                    "Employee email conflict during create: {}",
                    dto.getEmail()
            );
            throw new EmployeeEmailAlreadyExistsException(dto.getEmail());
        }

        notifyEmployeeCreated(saved);
        return employeeMapper.toDto(saved);
    }

    private void notifyEmployeeCreated(Employee employee) {
        try {
            var event = Map.of(
                    "employeeEmail", employee.getEmail(),
                    "employeeName", employee.getFirstName() + " " + employee.getLastName()
            );
            restTemplate.postForEntity(notificationServiceUrl, event, Void.class);
        } catch (Exception e) {
            log.warn("Failed to notify notification-service: {}", e.getMessage());
        }
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
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + id
                ));

        ensureEmailAvailableForUpdate(dto.getEmail(), id);
        ensureOwnershipAvailableForUpdate(dto.getAuthUsername(), id);

        Department department = resolveDepartment(dto.getDepartmentId());

        existing.setFirstName(dto.getFirstName());
        existing.setLastName(dto.getLastName());
        existing.setEmail(dto.getEmail());
        existing.setAuthUsername(dto.getAuthUsername());
        existing.setDesignation(dto.getDesignation());
        existing.setDateOfJoining(dto.getDateOfJoining());
        existing.setDepartment(department);

        try {
            Employee updated = employeeRepository.save(existing);
            return employeeMapper.toDto(updated);
        } catch (DataIntegrityViolationException exception) {
            log.warn(
                    "Employee email conflict during update: {}",
                    dto.getEmail()
            );
            throw new EmployeeEmailAlreadyExistsException(dto.getEmail());
        }
    }

    private void ensureEmailAvailableForCreate(String email) {
        if (employeeRepository.existsByEmail(email)) {
            throw new EmployeeEmailAlreadyExistsException(email);
        }
    }

    private void ensureEmailAvailableForUpdate(
            String email,
            Long employeeId
    ) {
        if (employeeRepository.existsByEmailAndIdNot(
                email,
                employeeId
        )) {
            throw new EmployeeEmailAlreadyExistsException(email);
        }
    }

    private void ensureOwnershipAvailableForCreate(
            String authUsername
    ) {
        if (authUsername == null || authUsername.isBlank()) {
            return;
        }

        if (employeeRepository.existsByAuthUsername(
                authUsername
        )) {
            throw new EmployeeOwnershipAlreadyExistsException(
                    authUsername
            );
        }
    }

    private void ensureOwnershipAvailableForUpdate(
            String authUsername,
            Long employeeId
    ) {
        if (authUsername == null || authUsername.isBlank()) {
            return;
        }

        if (employeeRepository.existsByAuthUsernameAndIdNot(
                authUsername,
                employeeId
        )) {
            throw new EmployeeOwnershipAlreadyExistsException(
                    authUsername
            );
        }
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

    @Override
    public EmployeeDto getEmployeeByAuthUsername(
            String authUsername
    ) {
        Employee employee = employeeRepository
                .findByAuthUsername(authUsername)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found for authenticated user: "
                                + authUsername
                ));

        return employeeMapper.toDto(employee);
    }
}