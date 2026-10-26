package com.amit.ems.employeeservice.integration;

import com.amit.ems.employeeservice.dto.DepartmentDto;
import com.amit.ems.employeeservice.service.DepartmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full MVC/security/service/repository flow; no outer test transaction or mocked persistence. */
abstract class AbstractDepartmentConflictContract {
    private static final String API = "/api/v1/departments";
    @Autowired MockMvc mvc;
    @Autowired DepartmentService service;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clearOwnedDatabase() {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        jdbc.update("DELETE FROM employees");
        jdbc.update("DELETE FROM departments");
    }

    @Test
    void newDepartmentReturns201AndCommits() throws Exception {
        mvc.perform(post(API).with(user("fixture").roles("ADMIN"))
                        .contentType("application/json").content("{\"name\":\"Engineering\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber());
        assertThat(count()).isEqualTo(1);
    }

    @Test
    void duplicateCreateReturns409WithoutExtraRow() throws Exception {
        create("Engineering");
        mvc.perform(post(API).with(user("fixture").roles("ADMIN"))
                        .contentType("application/json").content("{\"name\":\"Engineering\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("A department with that name already exists"))
                .andExpect(jsonPath("$.path").value(API));
        assertThat(count()).isEqualTo(1);
    }

    @Test
    void conflictingRenameReturns409AndRollsBackOriginalName() throws Exception {
        create("Engineering");
        long sales = create("Sales");
        mvc.perform(put(API + "/" + sales).with(user("fixture").roles("HR"))
                        .contentType("application/json").content("{\"name\":\"Engineering\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A department with that name already exists"));
        assertThat(name(sales)).isEqualTo("Sales");
        assertThat(count()).isEqualTo(2);
    }

    @Test
    void unchangedNameUpdateReturns200() throws Exception {
        long id = create("Engineering");
        mvc.perform(put(API + "/" + id).with(user("fixture").roles("ADMIN"))
                        .contentType("application/json").content("{\"name\":\"Engineering\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Engineering"));
        assertThat(count()).isEqualTo(1);
    }

    @Test
    void availableRenameReturns200AndCommits() throws Exception {
        long id = create("Engineering");
        mvc.perform(put(API + "/" + id).with(user("fixture").roles("ADMIN"))
                        .contentType("application/json").content("{\"name\":\"Platform\"}"))
                .andExpect(status().isOk());
        assertThat(name(id)).isEqualTo("Platform");
        assertThat(count()).isEqualTo(1);
    }

    @Test
    void concurrentCreatesLeaveOneWinnerAndOne409() throws Exception {
        var workers = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try {
            java.util.concurrent.Callable<Integer> request = () -> {
                ready.countDown();
                assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                return mvc.perform(post(API).with(user("fixture").roles("ADMIN"))
                                .contentType("application/json").content("{\"name\":\"Concurrent\"}"))
                        .andReturn().getResponse().getStatus();
            };
            var first = workers.submit(request);
            var second = workers.submit(request);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
            assertThat(count()).isEqualTo(1);
        } finally {
            start.countDown();
            workers.shutdownNow();
            assertThat(workers.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void unrelatedDatabaseFailureStays500AndDoesNotLeakSql() throws Exception {
        // DTO length validation is separate work; this deliberately reaches the real column limit.
        mvc.perform(post(API).with(user("fixture").roles("ADMIN"))
                        .contentType("application/json")
                        .content("{\"name\":\"" + "x".repeat(256) + "\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected internal error occurred"));
        assertThat(count()).isZero();
    }

    private long create(String name) {
        return service.createDepartment(DepartmentDto.builder().name(name).build()).getId();
    }

    private String name(long id) {
        return jdbc.queryForObject("SELECT name FROM departments WHERE id = ?", String.class, id);
    }

    private int count() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM departments", Integer.class);
    }
}
