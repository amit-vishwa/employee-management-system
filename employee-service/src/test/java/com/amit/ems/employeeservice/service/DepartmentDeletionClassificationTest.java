package com.amit.ems.employeeservice.service;

import com.amit.ems.employeeservice.exception.DepartmentNotEmptyException;
import com.amit.ems.employeeservice.mapper.DepartmentMapper;
import com.amit.ems.employeeservice.repository.DepartmentRepository;
import com.amit.ems.employeeservice.service.impl.DepartmentServiceImpl;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import java.sql.SQLException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentDeletionClassificationTest {
    @Mock DepartmentRepository repository;
    @Mock DepartmentMapper mapper;
    @InjectMocks DepartmentServiceImpl service;

    @ParameterizedTest
    @CsvSource({"fk_employees_department,23000,1451", "PUBLIC.FK_EMPLOYEES_DEPARTMENT: PUBLIC.EMPLOYEES,23503,23503"})
    void employeeReferenceBecomesConflict(String name, String state, int code) {
        when(repository.deleteDepartmentRowById(1L)).thenThrow(failure(name, state, code));
        assertThatThrownBy(() -> service.deleteDepartment(1L)).isInstanceOf(DepartmentNotEmptyException.class)
                .hasMessage("Cannot delete a department that contains employees");
    }

    @ParameterizedTest
    @CsvSource({"other_fk,23000,1451", "fk_employees_department,23000,1452", "fk_employees_department,23505,23505", ",23000,1451"})
    void unrelatedOrUnknownConstraintRemainsServerFailure(String name, String state, int code) {
        var failure = failure(name, state, code);
        when(repository.deleteDepartmentRowById(1L)).thenThrow(failure);
        assertThatThrownBy(() -> service.deleteDepartment(1L)).isSameAs(failure);
    }

    @Test
    void messageAloneDoesNotClassifyFailure() {
        var failure = new DataIntegrityViolationException("fk_employees_department", new SQLException("fixture", "23000", 1451));
        when(repository.deleteDepartmentRowById(1L)).thenThrow(failure);
        assertThatThrownBy(() -> service.deleteDepartment(1L)).isSameAs(failure);
    }

    private static DataIntegrityViolationException failure(String name, String state, int code) {
        return new DataIntegrityViolationException("fixture", new ConstraintViolationException("fixture",
                new SQLException("fixture", state, code), "delete from departments where id = ?", name));
    }
}
