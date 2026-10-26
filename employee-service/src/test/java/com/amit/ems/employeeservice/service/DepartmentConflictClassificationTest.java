package com.amit.ems.employeeservice.service;

import com.amit.ems.employeeservice.dto.DepartmentDto;
import com.amit.ems.employeeservice.entity.Department;
import com.amit.ems.employeeservice.exception.DepartmentNameAlreadyExistsException;
import com.amit.ems.employeeservice.mapper.DepartmentMapper;
import com.amit.ems.employeeservice.repository.DepartmentRepository;
import com.amit.ems.employeeservice.service.impl.DepartmentServiceImpl;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentConflictClassificationTest {
    @Mock DepartmentRepository repository;
    @Mock DepartmentMapper mapper;
    @InjectMocks DepartmentServiceImpl service;

    @ParameterizedTest
    @MethodSource("namedDuplicates")
    void recognizedNameDuplicateBecomesDomainConflict(String name, String state, int code) {
        var request = DepartmentDto.builder().name("Engineering").build();
        var department = Department.builder().name("Engineering").build();
        when(mapper.toEntity(request)).thenReturn(department);
        when(repository.saveAndFlush(department)).thenThrow(failure(name, state, code));
        assertThatThrownBy(() -> service.createDepartment(request))
                .isInstanceOf(DepartmentNameAlreadyExistsException.class)
                .hasMessage("A department with that name already exists");
    }

    @ParameterizedTest
    @MethodSource("otherFailures")
    void unrelatedOrUnidentifiedViolationIsRethrown(String name, String state, int code) {
        var request = DepartmentDto.builder().name("Engineering").build();
        var department = Department.builder().name("Engineering").build();
        var failure = failure(name, state, code);
        when(mapper.toEntity(request)).thenReturn(department);
        when(repository.saveAndFlush(department)).thenThrow(failure);
        assertThatThrownBy(() -> service.createDepartment(request)).isSameAs(failure);
    }

    @Test
    void messageMentioningNameWithoutConstraintMetadataIsNotEnough() {
        var request = DepartmentDto.builder().name("Engineering").build();
        var department = Department.builder().name("Engineering").build();
        var failure = new DataIntegrityViolationException("uk_departments_name duplicate",
                new SQLException("duplicate uk_departments_name", "23000", 1062));
        when(mapper.toEntity(request)).thenReturn(department);
        when(repository.saveAndFlush(department)).thenThrow(failure);
        assertThatThrownBy(() -> service.createDepartment(request)).isSameAs(failure);
    }

    static Stream<Arguments> namedDuplicates() {
        return Stream.of(
                Arguments.of("uk_departments_name", "23000", 1062),
                Arguments.of("departments.uk_departments_name", "23000", 1062),
                Arguments.of("PUBLIC.UK_DEPARTMENTS_NAME_INDEX_4 ON PUBLIC.DEPARTMENTS(NAME NULLS FIRST)",
                        "23505", 23505));
    }

    static Stream<Arguments> otherFailures() {
        return Stream.of(
                Arguments.of("uk_other_name", "23000", 1062),
                Arguments.of("UK_OTHER_NAME_INDEX_4", "23505", 23505),
                Arguments.of(null, "23000", 1062),
                Arguments.of("uk_departments_name", "23000", 1452),
                Arguments.of("uk_departments_name", "23502", 23502));
    }

    private static DataIntegrityViolationException failure(String name, String state, int code) {
        return new DataIntegrityViolationException("controlled fixture violation",
                new ConstraintViolationException("fixture", new SQLException("fixture", state, code), name));
    }
}
