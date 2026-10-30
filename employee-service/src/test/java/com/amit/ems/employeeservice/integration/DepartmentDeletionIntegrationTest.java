package com.amit.ems.employeeservice.integration;

import com.amit.ems.employeeservice.dto.DepartmentDto;
import com.amit.ems.employeeservice.service.DepartmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:department_deletion;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DepartmentDeletionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired DepartmentService service;
    @Autowired JdbcTemplate jdbc;

    @Test
    void populatedDepartmentMustPreserveEmployeeRecords() throws Exception {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        long department = service.createDepartment(DepartmentDto.builder().name("Deletion fixture").build()).getId();
        jdbc.update("INSERT INTO employees (first_name, last_name, email, department_id) VALUES (?, ?, ?, ?)",
                "Ada", "Lovelace", "deletion-fixture@example.com", department);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM employees WHERE department_id = ?", Integer.class, department)).isEqualTo(1);
        int response = mvc.perform(delete("/api/v1/departments/" + department).with(user("fixture").roles("ADMIN")))
                .andReturn().getResponse().getStatus();
        int departments = jdbc.queryForObject("SELECT COUNT(*) FROM departments WHERE id = ?", Integer.class, department);
        int employees = jdbc.queryForObject("SELECT COUNT(*) FROM employees WHERE email = ?", Integer.class, "deletion-fixture@example.com");
        assertThat(response).as("HTTP status; committed department rows=%s, employee rows=%s", departments, employees).isEqualTo(409);
        assertThat(departments).isEqualTo(1);
        assertThat(employees).isEqualTo(1);
    }
}
