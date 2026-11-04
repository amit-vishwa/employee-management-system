package com.amit.ems.employeeservice.controller;

import com.amit.ems.employeeservice.dto.DepartmentDto;
import com.amit.ems.employeeservice.dto.EmployeeDto;
import com.amit.ems.employeeservice.security.JwtAuthFilter;
import com.amit.ems.employeeservice.service.DepartmentService;
import com.amit.ems.employeeservice.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest({EmployeeController.class, DepartmentController.class})
@AutoConfigureMockMvc(addFilters = false)
class RequestLengthValidationTest {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper mapper;
    @MockitoBean EmployeeService employees;
    @MockitoBean DepartmentService departments;
    @MockitoBean JwtAuthFilter jwt;

    static Stream<Arguments> oversized() {
        return Stream.of("name", "firstName", "lastName", "email", "authUsername", "designation")
                .flatMap(field -> Stream.of(false, true).map(update -> Arguments.of(field, update)));
    }

    @ParameterizedTest
    @MethodSource("oversized")
    void rejectsOversizedInputBeforeCallingEitherService(String field, boolean update) throws Exception {
        var body = employee();
        boolean department = "name".equals(field);
        if (department) { body.clear(); }
        body.put(field, "email".equals(field)
                ? "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(63)
                : "x".repeat(256));
        String api = "/api/v1/" + (department ? "departments" : "employees");
        var request = update ? put(api + "/1") : post(api);
        mvc.perform(request.contentType("application/json").content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(field)))
                .andExpect(jsonPath("$.message", containsString("255")));
        verifyNoInteractions(employees, departments);
    }

    @Test
    void optionalFieldsStillAllowNullAndEmptyOnBothRoutes() throws Exception {
        when(employees.createEmployee(any())).thenAnswer(call -> call.getArgument(0));
        when(employees.updateEmployee(eq(1L), any())).thenAnswer(call -> call.getArgument(1));
        for (String optional : new String[]{null, ""}) {
            var body = employee();
            body.put("authUsername", optional);
            body.put("designation", optional);
            mvc.perform(post("/api/v1/employees").contentType("application/json").content(mapper.writeValueAsString(body)))
                    .andExpect(status().isCreated());
            mvc.perform(put("/api/v1/employees/1").contentType("application/json").content(mapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void sizeCountsUtf16CodeUnitsForSupplementaryUnicode() throws Exception {
        String emoji = new String(Character.toChars(0x1F600));
        mvc.perform(post("/api/v1/departments").contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of("name", emoji.repeat(128)))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(departments);
        when(departments.createDepartment(any())).thenReturn(DepartmentDto.builder().name("fixture").build());
        mvc.perform(post("/api/v1/departments").contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of("name", emoji.repeat(127) + "x"))))
                .andExpect(status().isCreated());
    }

    @Test
    void requiredAndEmailValidationStillRejectsInvalidInputs() throws Exception {
        for (String field : new String[]{"firstName", "lastName", "email"}) {
            var body = employee(); body.put(field, " ");
            mvc.perform(post("/api/v1/employees").contentType("application/json").content(mapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }
        var body = employee(); body.put("email", "invalid-email");
        mvc.perform(put("/api/v1/employees/1").contentType("application/json").content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/departments/1").contentType("application/json").content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(employees, departments);
    }

    private static Map<String, Object> employee() {
        return new LinkedHashMap<>(Map.of("firstName", "Ada", "lastName", "Lovelace", "email", "ada@example.com"));
    }
}
