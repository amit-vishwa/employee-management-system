package com.amit.ems.employeeservice.service;

import com.amit.ems.common.exception.ResourceNotFoundException;
import com.amit.ems.employeeservice.dto.DepartmentDto;
import com.amit.ems.employeeservice.entity.Department;
import com.amit.ems.employeeservice.mapper.DepartmentMapper;
import com.amit.ems.employeeservice.repository.DepartmentRepository;
import com.amit.ems.employeeservice.service.impl.DepartmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    private DepartmentDto departmentDto;
    private Department department;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id(1L)
                .name("Engineering")
                .build();

        departmentDto = DepartmentDto.builder().id(1L).name("Engineering").build();
    }

    @Test
    void createDepartment_shouldReturnSavedDepartmentDto() {
        when(departmentMapper.toEntity(departmentDto)).thenReturn(department);
        when(departmentRepository.save(department)).thenReturn(department);
        when(departmentMapper.toDto(department)).thenReturn(departmentDto);

        DepartmentDto result = departmentService.createDepartment(departmentDto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Engineering");
        verify(departmentRepository, times(1)).save(department);
    }

    @Test
    void getDepartmentById_whenExists_shouldReturnDepartmentDto() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentMapper.toDto(department)).thenReturn(departmentDto);

        DepartmentDto result = departmentService.getDepartmentById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getDepartmentById_whenNotFound_shouldThrowException() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.getDepartmentById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Department not found with id: 99");
    }

    @Test
    void getAllDepartments_shouldReturnListOfDepartmentDtos() {
        when(departmentRepository.findAll()).thenReturn(List.of(department));
        when(departmentMapper.toDto(department)).thenReturn(departmentDto);

        List<DepartmentDto> result = departmentService.getAllDepartments();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Engineering");
    }

    @Test
    void updateDepartment_whenExists_shouldReturnUpdatedDepartmentDto() {
        DepartmentDto updateRequest = DepartmentDto.builder().name("Engineering & Platform").build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentRepository.save(department)).thenReturn(department);
        when(departmentMapper.toDto(department)).thenReturn(
                DepartmentDto.builder().id(1L).name("Engineering & Platform").build()
        );

        DepartmentDto result = departmentService.updateDepartment(1L, updateRequest);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Engineering & Platform");
        verify(departmentRepository, times(1)).save(department);
    }

    @Test
    void updateDepartment_whenNotFound_shouldThrowException() {
        DepartmentDto updateRequest = DepartmentDto.builder().name("Nonexistent").build();

        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.updateDepartment(99L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Department not found with id: 99");

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void deleteDepartment_whenExists_shouldDeleteSuccessfully() {
        when(departmentRepository.existsById(1L)).thenReturn(true);

        departmentService.deleteDepartment(1L);

        verify(departmentRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteDepartment_whenNotFound_shouldThrowException() {
        when(departmentRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> departmentService.deleteDepartment(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Department not found with id: 99");

        verify(departmentRepository, never()).deleteById(any());
    }
}