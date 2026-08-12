package com.amit.ems.employeeservice.service;

import com.amit.ems.common.exception.ResourceNotFoundException;
import com.amit.ems.employeeservice.dto.EmployeeDto;
import com.amit.ems.employeeservice.entity.Department;
import com.amit.ems.employeeservice.entity.Employee;
import com.amit.ems.employeeservice.exception.EmployeeEmailAlreadyExistsException;
import com.amit.ems.employeeservice.exception.EmployeeOwnershipAlreadyExistsException;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    private static final Long EMPLOYEE_ID = 1L;
    private static final Long DEPARTMENT_ID = 1L;

    private static final String EMPLOYEE_EMAIL =
            "john.doe@example.com";

    private static final String AUTH_USERNAME = "john";

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

    @Mock
    private RestTemplate restTemplate;

    private EmployeeServiceImpl employeeService;

    private Employee employee;
    private EmployeeDto employeeDto;
    private Department department;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id(DEPARTMENT_ID)
                .name("Engineering")
                .build();

        employee = Employee.builder()
                .id(EMPLOYEE_ID)
                .firstName("John")
                .lastName("Doe")
                .email(EMPLOYEE_EMAIL)
                .authUsername(AUTH_USERNAME)
                .designation("Software Engineer")
                .dateOfJoining(LocalDate.of(2024, 1, 15))
                .department(department)
                .build();

        employeeDto = EmployeeDto.builder()
                .id(EMPLOYEE_ID)
                .firstName("John")
                .lastName("Doe")
                .email(EMPLOYEE_EMAIL)
                .authUsername(AUTH_USERNAME)
                .designation("Software Engineer")
                .dateOfJoining(LocalDate.of(2024, 1, 15))
                .departmentId(DEPARTMENT_ID)
                .departmentName("Engineering")
                .build();

        Map<String, EmployeeSearchStrategy> strategyMap =
                new HashMap<>();

        strategyMap.put(
                "department",
                departmentSearchStrategy
        );
        strategyMap.put(
                "designation",
                designationSearchStrategy
        );

        employeeService = new EmployeeServiceImpl(
                employeeRepository,
                departmentRepository,
                employeeMapper,
                strategyMap,
                restTemplate
        );

        ReflectionTestUtils.setField(
                employeeService,
                "notificationServiceUrl",
                "http://localhost:8083/api/v1/"
                        + "notifications/employee-created"
        );
    }

    @Test
    void createEmployee_shouldReturnSavedEmployeeDto() {
        when(employeeRepository.existsByEmail(EMPLOYEE_EMAIL))
                .thenReturn(false);
        when(employeeRepository.existsByAuthUsername(
                AUTH_USERNAME
        )).thenReturn(false);
        when(departmentRepository.findById(DEPARTMENT_ID))
                .thenReturn(Optional.of(department));
        when(employeeMapper.toEntity(employeeDto, department))
                .thenReturn(employee);
        when(employeeRepository.save(employee))
                .thenReturn(employee);
        when(employeeMapper.toDto(employee))
                .thenReturn(employeeDto);

        EmployeeDto result =
                employeeService.createEmployee(employeeDto);

        assertThat(result).isSameAs(employeeDto);
        assertThat(result.getEmail())
                .isEqualTo(EMPLOYEE_EMAIL);
        assertThat(result.getAuthUsername())
                .isEqualTo(AUTH_USERNAME);

        verify(employeeRepository)
                .existsByEmail(EMPLOYEE_EMAIL);
        verify(employeeRepository)
                .existsByAuthUsername(AUTH_USERNAME);
        verify(employeeRepository).save(employee);
        verify(restTemplate).postForEntity(
                anyString(),
                any(),
                eq(Void.class)
        );
    }

    @Test
    void createEmployee_whenNotificationServiceFails_shouldStillReturnSavedEmployeeDto() {
        when(employeeRepository.existsByEmail(EMPLOYEE_EMAIL))
                .thenReturn(false);
        when(employeeRepository.existsByAuthUsername(
                AUTH_USERNAME
        )).thenReturn(false);
        when(departmentRepository.findById(DEPARTMENT_ID))
                .thenReturn(Optional.of(department));
        when(employeeMapper.toEntity(employeeDto, department))
                .thenReturn(employee);
        when(employeeRepository.save(employee))
                .thenReturn(employee);
        when(employeeMapper.toDto(employee))
                .thenReturn(employeeDto);
        when(restTemplate.postForEntity(
                anyString(),
                any(),
                eq(Void.class)
        )).thenThrow(
                new RuntimeException(
                        "notification-service unreachable"
                )
        );

        EmployeeDto result =
                employeeService.createEmployee(employeeDto);

        assertThat(result).isSameAs(employeeDto);
        assertThat(result.getEmail())
                .isEqualTo(EMPLOYEE_EMAIL);

        verify(employeeRepository).save(employee);
        verify(employeeMapper).toDto(employee);
    }

    @Test
    void createEmployee_whenEmailExists_shouldThrowConflict() {
        when(employeeRepository.existsByEmail(EMPLOYEE_EMAIL))
                .thenReturn(true);

        assertThatThrownBy(
                () -> employeeService.createEmployee(employeeDto)
        )
                .isInstanceOf(
                        EmployeeEmailAlreadyExistsException.class
                )
                .hasMessage(
                        "Employee email already exists: "
                                + EMPLOYEE_EMAIL
                );

        verify(employeeRepository, never()).save(any());
        verify(employeeRepository, never())
                .existsByAuthUsername(anyString());
        verifyNoInteractions(
                departmentRepository,
                employeeMapper,
                restTemplate
        );
    }

    @Test
    void createEmployee_whenDatabaseRaceOccurs_shouldThrowConflict() {
        when(employeeRepository.existsByEmail(EMPLOYEE_EMAIL))
                .thenReturn(false);
        when(employeeRepository.existsByAuthUsername(
                AUTH_USERNAME
        )).thenReturn(false);
        when(departmentRepository.findById(DEPARTMENT_ID))
                .thenReturn(Optional.of(department));
        when(employeeMapper.toEntity(employeeDto, department))
                .thenReturn(employee);
        when(employeeRepository.save(employee))
                .thenThrow(
                        new DataIntegrityViolationException(
                                "duplicate employee constraint"
                        )
                );

        assertThatThrownBy(
                () -> employeeService.createEmployee(employeeDto)
        )
                .isInstanceOf(
                        EmployeeEmailAlreadyExistsException.class
                )
                .hasMessage(
                        "Employee email already exists: "
                                + EMPLOYEE_EMAIL
                );

        verify(employeeRepository).save(employee);
        verify(restTemplate, never()).postForEntity(
                anyString(),
                any(),
                eq(Void.class)
        );
        verify(employeeMapper, never()).toDto(any());
    }

    @Test
    void createEmployee_whenAuthUsernameExists_shouldThrowConflict() {
        when(employeeRepository.existsByEmail(EMPLOYEE_EMAIL))
                .thenReturn(false);
        when(employeeRepository.existsByAuthUsername(
                AUTH_USERNAME
        )).thenReturn(true);

        assertThatThrownBy(
                () -> employeeService.createEmployee(employeeDto)
        )
                .isInstanceOf(
                        EmployeeOwnershipAlreadyExistsException.class
                )
                .hasMessage(
                        "Employee record already linked to username: "
                                + AUTH_USERNAME
                );

        verify(employeeRepository, never()).save(any());
        verifyNoInteractions(
                departmentRepository,
                employeeMapper,
                restTemplate
        );
    }

    @Test
    void createEmployee_withoutAuthUsername_shouldAllowUnlinkedEmployee() {
        employeeDto.setAuthUsername(null);
        employee.setAuthUsername(null);

        when(employeeRepository.existsByEmail(EMPLOYEE_EMAIL))
                .thenReturn(false);
        when(departmentRepository.findById(DEPARTMENT_ID))
                .thenReturn(Optional.of(department));
        when(employeeMapper.toEntity(employeeDto, department))
                .thenReturn(employee);
        when(employeeRepository.save(employee))
                .thenReturn(employee);
        when(employeeMapper.toDto(employee))
                .thenReturn(employeeDto);

        EmployeeDto result =
                employeeService.createEmployee(employeeDto);

        assertThat(result).isSameAs(employeeDto);
        assertThat(result.getAuthUsername()).isNull();

        verify(employeeRepository, never())
                .existsByAuthUsername(anyString());
        verify(employeeRepository).save(employee);
    }

    @Test
    void updateEmployee_whenEmailBelongsToAnotherEmployee_shouldThrowConflict() {
        when(employeeRepository.findById(EMPLOYEE_ID))
                .thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmailAndIdNot(
                EMPLOYEE_EMAIL,
                EMPLOYEE_ID
        )).thenReturn(true);

        assertThatThrownBy(
                () -> employeeService.updateEmployee(
                        EMPLOYEE_ID,
                        employeeDto
                )
        )
                .isInstanceOf(
                        EmployeeEmailAlreadyExistsException.class
                )
                .hasMessage(
                        "Employee email already exists: "
                                + EMPLOYEE_EMAIL
                );

        verify(employeeRepository, never()).save(any());
        verify(employeeRepository, never())
                .existsByAuthUsernameAndIdNot(
                        anyString(),
                        any()
                );
        verifyNoInteractions(departmentRepository);
    }

    @Test
    void updateEmployee_whenAuthUsernameBelongsToAnotherEmployee_shouldThrowConflict() {
        when(employeeRepository.findById(EMPLOYEE_ID))
                .thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmailAndIdNot(
                EMPLOYEE_EMAIL,
                EMPLOYEE_ID
        )).thenReturn(false);
        when(employeeRepository.existsByAuthUsernameAndIdNot(
                AUTH_USERNAME,
                EMPLOYEE_ID
        )).thenReturn(true);

        assertThatThrownBy(
                () -> employeeService.updateEmployee(
                        EMPLOYEE_ID,
                        employeeDto
                )
        )
                .isInstanceOf(
                        EmployeeOwnershipAlreadyExistsException.class
                )
                .hasMessage(
                        "Employee record already linked to username: "
                                + AUTH_USERNAME
                );

        verify(employeeRepository, never()).save(any());
        verifyNoInteractions(departmentRepository);
    }

    @Test
    void updateEmployee_whenEmailAndOwnershipAreAvailable_shouldUpdateEmployee() {
        EmployeeDto updatedDto = EmployeeDto.builder()
                .id(EMPLOYEE_ID)
                .firstName("John")
                .lastName("Doe")
                .email(EMPLOYEE_EMAIL)
                .authUsername(AUTH_USERNAME)
                .designation("Lead Engineer")
                .dateOfJoining(LocalDate.of(2024, 1, 15))
                .departmentId(DEPARTMENT_ID)
                .departmentName("Engineering")
                .build();

        when(employeeRepository.findById(EMPLOYEE_ID))
                .thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmailAndIdNot(
                EMPLOYEE_EMAIL,
                EMPLOYEE_ID
        )).thenReturn(false);
        when(employeeRepository.existsByAuthUsernameAndIdNot(
                AUTH_USERNAME,
                EMPLOYEE_ID
        )).thenReturn(false);
        when(departmentRepository.findById(DEPARTMENT_ID))
                .thenReturn(Optional.of(department));
        when(employeeRepository.save(employee))
                .thenReturn(employee);
        when(employeeMapper.toDto(employee))
                .thenReturn(updatedDto);

        EmployeeDto result = employeeService.updateEmployee(
                EMPLOYEE_ID,
                updatedDto
        );

        assertThat(result).isSameAs(updatedDto);
        assertThat(employee.getDesignation())
                .isEqualTo("Lead Engineer");
        assertThat(employee.getEmail())
                .isEqualTo(EMPLOYEE_EMAIL);
        assertThat(employee.getAuthUsername())
                .isEqualTo(AUTH_USERNAME);
        assertThat(employee.getDepartment())
                .isSameAs(department);

        verify(employeeRepository)
                .existsByEmailAndIdNot(
                        EMPLOYEE_EMAIL,
                        EMPLOYEE_ID
                );
        verify(employeeRepository)
                .existsByAuthUsernameAndIdNot(
                        AUTH_USERNAME,
                        EMPLOYEE_ID
                );
        verify(employeeRepository).save(employee);
        verify(employeeMapper).toDto(employee);
    }

    @Test
    void getEmployeeByAuthUsername_whenLinked_shouldReturnEmployee() {
        when(employeeRepository.findByAuthUsername(
                AUTH_USERNAME
        )).thenReturn(Optional.of(employee));
        when(employeeMapper.toDto(employee))
                .thenReturn(employeeDto);

        EmployeeDto result =
                employeeService.getEmployeeByAuthUsername(
                        AUTH_USERNAME
                );

        assertThat(result).isSameAs(employeeDto);
        assertThat(result.getAuthUsername())
                .isEqualTo(AUTH_USERNAME);

        verify(employeeRepository)
                .findByAuthUsername(AUTH_USERNAME);
        verify(employeeMapper).toDto(employee);
    }

    @Test
    void getEmployeeByAuthUsername_whenNotLinked_shouldThrowNotFound() {
        when(employeeRepository.findByAuthUsername(
                AUTH_USERNAME
        )).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> employeeService.getEmployeeByAuthUsername(
                        AUTH_USERNAME
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "Employee not found for authenticated user: "
                                + AUTH_USERNAME
                );

        verify(employeeMapper, never()).toDto(any());
    }

    @Test
    void getEmployeeById_whenExists_shouldReturnEmployeeDto() {
        when(employeeRepository.findById(EMPLOYEE_ID))
                .thenReturn(Optional.of(employee));
        when(employeeMapper.toDto(employee))
                .thenReturn(employeeDto);

        EmployeeDto result =
                employeeService.getEmployeeById(EMPLOYEE_ID);

        assertThat(result).isSameAs(employeeDto);
        assertThat(result.getId())
                .isEqualTo(EMPLOYEE_ID);
    }

    @Test
    void getEmployeeById_whenNotFound_shouldThrowException() {
        when(employeeRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> employeeService.getEmployeeById(99L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "Employee not found with id: 99"
                );
    }

    @Test
    void getAllEmployees_shouldReturnListOfEmployeeDtos() {
        when(employeeRepository.findAll())
                .thenReturn(List.of(employee));
        when(employeeMapper.toDto(employee))
                .thenReturn(employeeDto);

        List<EmployeeDto> result =
                employeeService.getAllEmployees();

        assertThat(result).containsExactly(employeeDto);
        assertThat(result.get(0).getEmail())
                .isEqualTo(EMPLOYEE_EMAIL);
    }

    @Test
    void deleteEmployee_whenExists_shouldDeleteSuccessfully() {
        when(employeeRepository.existsById(EMPLOYEE_ID))
                .thenReturn(true);

        employeeService.deleteEmployee(EMPLOYEE_ID);

        verify(employeeRepository)
                .deleteById(EMPLOYEE_ID);
    }

    @Test
    void deleteEmployee_whenNotFound_shouldThrowException() {
        when(employeeRepository.existsById(99L))
                .thenReturn(false);

        assertThatThrownBy(
                () -> employeeService.deleteEmployee(99L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "Employee not found with id: 99"
                );

        verify(employeeRepository, never())
                .deleteById(any());
    }

    @Test
    void searchEmployees_withValidCriteria_shouldReturnResults() {
        when(departmentSearchStrategy.search("Engineering"))
                .thenReturn(List.of(employee));
        when(employeeMapper.toDto(employee))
                .thenReturn(employeeDto);

        List<EmployeeDto> result =
                employeeService.searchEmployees(
                        "department",
                        "Engineering"
                );

        assertThat(result).containsExactly(employeeDto);

        verify(departmentSearchStrategy)
                .search("Engineering");
        verify(designationSearchStrategy, never())
                .search(any());
    }

    @Test
    void searchEmployees_withInvalidCriteria_shouldThrowException() {
        assertThatThrownBy(
                () -> employeeService.searchEmployees(
                        "bogus",
                        "x"
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Unsupported search criteria: bogus"
                );

        verify(departmentSearchStrategy, never())
                .search(any());
        verify(designationSearchStrategy, never())
                .search(any());
    }
}