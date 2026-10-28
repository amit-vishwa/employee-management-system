package com.amit.ems.employeeservice.service.impl;

import com.amit.ems.common.exception.ResourceNotFoundException;
import com.amit.ems.employeeservice.dto.DepartmentDto;
import com.amit.ems.employeeservice.entity.Department;
import com.amit.ems.employeeservice.exception.DepartmentNameAlreadyExistsException;
import com.amit.ems.employeeservice.mapper.DepartmentMapper;
import com.amit.ems.employeeservice.repository.DepartmentRepository;
import com.amit.ems.employeeservice.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.transaction.annotation.Transactional;

import static com.amit.ems.common.logging.LogSanitizer.sanitize;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    @Transactional
    public DepartmentDto createDepartment(DepartmentDto dto) {
        if (dto.getId() != null) {
            throw new IllegalArgumentException(
                    "Department ID must not be supplied when creating a department");
        }

        log.info(
                "Creating department with name: {}",
                sanitize(dto.getName())
        );
        Department department = departmentMapper.toEntity(dto);
        Department saved = saveDepartment(department);
        return departmentMapper.toDto(saved);
    }

    @Override
    public DepartmentDto getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        return departmentMapper.toDto(department);
    }

    @Override
    public List<DepartmentDto> getAllDepartments() {
        return departmentRepository.findAll()
                .stream()
                .map(departmentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public DepartmentDto updateDepartment(Long id, DepartmentDto dto) {
        Department existing = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        existing.setName(dto.getName());
        Department updated = saveDepartment(existing);
        return departmentMapper.toDto(updated);
    }


    private Department saveDepartment(Department department) {
        try {
            // Force the nondeferrable constraint here, including SQL updates deferred until flush.
            return departmentRepository.saveAndFlush(department);
        } catch (DataIntegrityViolationException exception) {
            if (isNameConflict(exception)) {
                throw new DepartmentNameAlreadyExistsException();
            }
            throw exception;
        }
    }

    private static boolean isNameConflict(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                String constraint = violation.getConstraintName();
                var sql = violation.getSQLException();
                if (constraint == null || sql == null) {
                    return false;
                }
                String name = constraint.toUpperCase(Locale.ROOT)
                        .replace("`", "").replace("\"", "").split(" ON ", 2)[0];
                name = name.substring(name.lastIndexOf('.') + 1);
                if ("23505".equals(sql.getSQLState())) {
                    // H2 reports the backing index, optionally followed by its table expression.
                    return name.matches("UK_DEPARTMENTS_NAME(?:_INDEX_[0-9A-Z]+)?");
                }
                return sql.getErrorCode() == 1062 && "23000".equals(sql.getSQLState())
                        && "UK_DEPARTMENTS_NAME".equals(name);
            }
        }
        return false;
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Department not found with id: " + id);
        }
        departmentRepository.deleteById(id);
        log.info("Deleted department with id: {}", id);
    }
}