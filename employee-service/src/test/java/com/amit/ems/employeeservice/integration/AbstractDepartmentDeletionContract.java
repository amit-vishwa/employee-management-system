package com.amit.ems.employeeservice.integration;

import com.amit.ems.employeeservice.dto.DepartmentDto;
import com.amit.ems.employeeservice.service.DepartmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.springframework.dao.DataIntegrityViolationException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

abstract class AbstractDepartmentDeletionContract {
    @Autowired MockMvc mvc;
    @Autowired DepartmentService service;
    @Autowired JdbcTemplate jdbc;

    @Autowired PlatformTransactionManager transactions;

    @BeforeEach
    void clearOwnedDatabase() {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        jdbc.update("DELETE FROM employees");
        jdbc.update("DELETE FROM departments");
    }

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

    @Test
    void emptyDepartmentReturns204AndCommits() throws Exception {
        long id = create();
        mvc.perform(delete("/api/v1/departments/" + id).with(user("fixture").roles("ADMIN")))
                .andExpect(status().isNoContent());
        assertThat(departmentCount(id)).isZero();
    }

    @Test
    void missingDepartmentReturns404() throws Exception {
        mvc.perform(delete("/api/v1/departments/9223372036854775807").with(user("fixture").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void committedConcurrentAssignmentPreventsDeletion() throws Exception {
        long id = create();
        var inserted = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var deleting = new CountDownLatch(1);
        var workers = Executors.newFixedThreadPool(2);
        try {
            var assignment = workers.submit(() -> new TransactionTemplate(transactions).execute(ignored -> {
                insertEmployee(id);
                inserted.countDown();
                await(release);
                return true;
            }));
            assertThat(inserted.await(10, TimeUnit.SECONDS)).isTrue();
            var deletion = workers.submit(() -> {
                deleting.countDown();
                return mvc.perform(delete("/api/v1/departments/" + id).with(user("fixture").roles("ADMIN")))
                        .andReturn().getResponse().getStatus();
            });
            assertThat(deleting.await(10, TimeUnit.SECONDS)).isTrue();
            release.countDown();
            assertThat(assignment.get(20, TimeUnit.SECONDS)).isTrue();
            assertThat(deletion.get(20, TimeUnit.SECONDS)).isEqualTo(409);
            assertThat(departmentCount(id)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM employees WHERE department_id = ?", Integer.class, id)).isEqualTo(1);
        } finally {
            release.countDown();
            workers.shutdownNow();
            assertThat(workers.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void assignmentAfterDeletionCannotCreateOrphan() throws Exception {
        long id = create();
        mvc.perform(delete("/api/v1/departments/" + id).with(user("fixture").roles("ADMIN")))
                .andExpect(status().isNoContent());
        assertThatThrownBy(() -> insertEmployee(id)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM employees", Integer.class)).isZero();
    }

    private long create() {
        return service.createDepartment(DepartmentDto.builder().name("Deletion fixture").build()).getId();
    }

    private void insertEmployee(long id) {
        jdbc.update("INSERT INTO employees (first_name, last_name, email, department_id) VALUES (?, ?, ?, ?)",
                "Ada", "Lovelace", "concurrent-fixture@example.com", id);
    }

    private int departmentCount(long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM departments WHERE id = ?", Integer.class, id);
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
