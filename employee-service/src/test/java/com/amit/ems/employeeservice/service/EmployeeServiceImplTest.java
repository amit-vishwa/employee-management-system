package com.amit.ems.employeeservice.service;

import com.amit.ems.common.exception.ResourceNotFoundException;
import com.amit.ems.employeeservice.dto.EmployeeDto;
import com.amit.ems.employeeservice.entity.Department;
import com.amit.ems.employeeservice.entity.Employee;
import com.amit.ems.employeeservice.mapper.EmployeeMapper;
import com.amit.ems.employeeservice.repository.DepartmentRepository;
import com.amit.ems.employeeservice.repository.EmployeeRepository;
import com.amit.ems.employeeservice.service.impl.EmployeeServiceImpl;
import com.amit.ems.employeeservice.strategy.EmployeeSearchStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private EmployeeSearchStrategy departmentSearchStrategy;

    @Mock
    private EmployeeSearchStrategy designationSearchStrategy;

    private EmployeeServiceImpl employeeService;

    private Employee employee;
    private EmployeeDto employeeDto;
    private Department department;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id(1L)
                .name("Engineering")
                .build();

        employee = Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .designation("Software Engineer")
                .dateOfJoining(LocalDate.of(2024, 1, 15))
                .department(department)
                .build();

        employeeDto = EmployeeDto.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .designation("Software Engineer")
                .dateOfJoining(LocalDate.of(2024, 1, 15))
                .departmentId(1L)
                .departmentName("Engineering")
                .build();

        Map<String, EmployeeSearchStrategy> strategyMap = new HashMap<>();
        strategyMap.put("department", departmentSearchStrategy);
        strategyMap.put("designation", designationSearchStrategy);

        employeeService = new EmployeeServiceImpl(
                employeeRepository, departmentRepository, employeeMapper, strategyMap);
    }

    @Test
    void createEmployee_shouldReturnSavedEmployeeDto() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeMapper.toEntity(employeeDto, department)).thenReturn(employee);
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(employeeMapper.toDto(employee)).thenReturn(employeeDto);

        EmployeeDto result = employeeService.createEmployee(employeeDto);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        verify(employeeRepository, times(1)).save(employee);
    }

    @Test
    void getEmployeeById_whenExists_shouldReturnEmployeeDto() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeMapper.toDto(employee)).thenReturn(employeeDto);

        EmployeeDto result = employeeService.getEmployeeById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getEmployeeById_whenNotFound_shouldThrowException() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getEmployeeById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found with id: 99");
    }

    @Test
    void getAllEmployees_shouldReturnListOfEmployeeDtos() {
        when(employeeRepository.findAll()).thenReturn(List.of(employee));
        when(employeeMapper.toDto(employee)).thenReturn(employeeDto);

        List<EmployeeDto> result = employeeService.getAllEmployees();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    void deleteEmployee_whenExists_shouldDeleteSuccessfully() {
        when(employeeRepository.existsById(1L)).thenReturn(true);

        employeeService.deleteEmployee(1L);

        verify(employeeRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteEmployee_whenNotFound_shouldThrowException() {
        when(employeeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> employeeService.deleteEmployee(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found with id: 99");

        verify(employeeRepository, never()).deleteById(any());
    }

    @Test
    void searchEmployees_withValidCriteria_shouldReturnResults() {
        when(departmentSearchStrategy.search("Engineering")).thenReturn(List.of(employee));
        when(employeeMapper.toDto(employee)).thenReturn(employeeDto);

        List<EmployeeDto> result = employeeService.searchEmployees("department", "Engineering");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("john.doe@example.com");
        verify(departmentSearchStrategy, times(1)).search("Engineering");
    }

    @Test
    void searchEmployees_withInvalidCriteria_shouldThrowException() {
        assertThatThrownBy(() -> employeeService.searchEmployees("bogus", "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported search criteria: bogus");

        verify(departmentSearchStrategy, never()).search(any());
        verify(designationSearchStrategy, never()).search(any());
    }
}