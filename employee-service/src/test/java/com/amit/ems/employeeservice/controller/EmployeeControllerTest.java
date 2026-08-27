package com.amit.ems.employeeservice.controller;

import com.amit.ems.common.exception.ResourceNotFoundException;
import com.amit.ems.employeeservice.dto.EmployeeDto;
import com.amit.ems.employeeservice.exception.EmployeeEmailAlreadyExistsException;
import com.amit.ems.employeeservice.security.JwtAuthFilter;
import com.amit.ems.employeeservice.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void createEmployee_shouldReturn201() throws Exception {
        EmployeeDto employeeDto = createEmployeeDto();

        when(employeeService.createEmployee(
                any(EmployeeDto.class)
        )).thenReturn(employeeDto);

        mockMvc.perform(
                        post("/api/v1/employees")
                                .contentType("application/json")
                                .content(
                                        objectMapper.writeValueAsString(
                                                employeeDto
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email")
                        .value("jane.smith@example.com"))
                .andExpect(jsonPath("$.authUsername")
                        .value("jane"));
    }

    @Test
    void createEmployee_withInvalidEmail_shouldReturn400()
            throws Exception {

        EmployeeDto invalidDto = EmployeeDto.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("not-an-email")
                .authUsername("jane")
                .build();

        mockMvc.perform(
                        post("/api/v1/employees")
                                .contentType("application/json")
                                .content(
                                        objectMapper.writeValueAsString(
                                                invalidDto
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/employees"))
                .andExpect(jsonPath("$.message")
                        .isNotEmpty())
                .andExpect(jsonPath("$.timestamp")
                        .exists());
    }

    @Test
    void createEmployee_whenEmailExists_shouldReturn409()
            throws Exception {

        EmployeeDto employeeDto = createEmployeeDto();

        when(employeeService.createEmployee(
                any(EmployeeDto.class)
        )).thenThrow(
                new EmployeeEmailAlreadyExistsException(
                        "jane.smith@example.com"
                )
        );

        mockMvc.perform(
                        post("/api/v1/employees")
                                .contentType("application/json")
                                .content(
                                        objectMapper.writeValueAsString(
                                                employeeDto
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        "Employee email already exists: "
                                + "jane.smith@example.com"
                ))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/employees"))
                .andExpect(jsonPath("$.timestamp")
                        .exists());
    }

    @Test
    void getCurrentEmployee_shouldUseAuthenticatedUsername()
            throws Exception {

        EmployeeDto employeeDto = createEmployeeDto();

        when(employeeService.getEmployeeByAuthUsername("jane"))
                .thenReturn(employeeDto);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "jane",
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_EMPLOYEE"
                                )
                        )
                );

        mockMvc.perform(
                        get("/api/v1/employees/me")
                                .principal(authentication)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.authUsername")
                        .value("jane"))
                .andExpect(jsonPath("$.email")
                        .value("jane.smith@example.com"));

        verify(employeeService)
                .getEmployeeByAuthUsername("jane");
    }

    @Test
    void getCurrentEmployee_whenNoRecordIsLinked_shouldReturn404()
            throws Exception {

        when(employeeService.getEmployeeByAuthUsername("jane"))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Employee not found for "
                                        + "authenticated user: jane"
                        )
                );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "jane",
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_EMPLOYEE"
                                )
                        )
                );

        mockMvc.perform(
                        get("/api/v1/employees/me")
                                .principal(authentication)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(
                        "Employee not found for "
                                + "authenticated user: jane"
                ))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/employees/me"))
                .andExpect(jsonPath("$.timestamp")
                        .exists());
    }

    @Test
    void getEmployee_whenNotFound_shouldReturn404()
            throws Exception {

        when(employeeService.getEmployeeById(99L))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Employee not found with id: 99"
                        )
                );

        mockMvc.perform(get("/api/v1/employees/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(
                        "Employee not found with id: 99"
                ))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/employees/99"))
                .andExpect(jsonPath("$.timestamp")
                        .exists());
    }

    @Test
    void getAllEmployees_shouldReturn200()
            throws Exception {

        EmployeeDto employeeDto = createEmployeeDto();

        when(employeeService.getAllEmployees())
                .thenReturn(List.of(employeeDto));

        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].authUsername")
                        .value("jane"));
    }

    private EmployeeDto createEmployeeDto() {
        return EmployeeDto.builder()
                .id(1L)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .authUsername("jane")
                .designation("QA Engineer")
                .dateOfJoining(LocalDate.of(2024, 3, 1))
                .departmentId(1L)
                .departmentName("Quality")
                .build();
    }
}