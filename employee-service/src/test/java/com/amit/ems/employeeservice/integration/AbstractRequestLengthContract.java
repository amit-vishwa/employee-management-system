package com.amit.ems.employeeservice.integration;

import com.amit.ems.employeeservice.client.NotificationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/** Reads committed state outside test transactions; real services and persistence. */
abstract class AbstractRequestLengthContract {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean NotificationClient notifications;

    @BeforeEach
    void clearOwnedDatabase() {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        jdbc.update("DELETE FROM employees");
        jdbc.update("DELETE FROM departments");
    }

    static Stream<Arguments> boundaries() {
        return Stream.of("name", "firstName", "lastName", "email", "authUsername", "designation")
                .flatMap(field -> Stream.of(false, true).flatMap(update -> Stream.of(255, 256)
                        .map(length -> Arguments.of(field, update, length))));
    }

    @ParameterizedTest(name = "{0}, update={1}, length={2}")
    @MethodSource("boundaries")
    void validatesBeforeWritesAndPreservesCommittedState(String field, boolean update, int length) throws Exception {
        boolean department = "name".equals(field);
        String table = department ? "departments" : "employees";
        String column = switch (field) {
            case "firstName" -> "first_name";
            case "lastName" -> "last_name";
            case "authUsername" -> "auth_username";
            default -> field;
        };
        long id = 0;
        String original = "email".equals(field) ? "original@example.com" : "Original";
        if (update) {
            if (department) {
                jdbc.update("INSERT INTO departments (name) VALUES ('Original')");
            } else {
                jdbc.update("INSERT INTO employees (first_name,last_name,email,auth_username,designation) VALUES (?,?,?,?,?)",
                        "Original", "Original", "original@example.com", "Original", "Original");
            }
            id = jdbc.queryForObject("SELECT id FROM " + table, Long.class);
        }
        var body = new LinkedHashMap<String, Object>();
        if (department) {
            body.put("name", "Engineering");
        } else {
            body.putAll(Map.of("firstName", "Ada", "lastName", "Lovelace", "email", "ada@example.com"));
        }
        String value = "email".equals(field)
                ? "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(length - 193)
                : "x".repeat(length);
        body.put(field, value);
        String api = "/api/v1/" + table;
        var request = update ? put(api + "/" + id) : post(api);
        var result = mvc.perform(request.with(user("fixture").roles("ADMIN"))
                .contentType("application/json").content(mapper.writeValueAsString(body)));
        if (length == 256) {
            assertThat(result.andReturn().getResponse().getStatus()).isEqualTo(400);
            result.andExpect(jsonPath("$.message", containsString(field)))
                    .andExpect(jsonPath("$.message", containsString("255")));
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).isEqualTo(update ? 1 : 0);
            if (update) {
                assertThat(jdbc.queryForObject("SELECT " + column + " FROM " + table + " WHERE id = ?", String.class, id))
                        .isEqualTo(original);
            }
            verifyNoInteractions(notifications);
        } else {
            assertThat(result.andReturn().getResponse().getStatus()).isEqualTo(update ? 200 : 201);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT " + column + " FROM " + table, String.class)).isEqualTo(value);
        }
    }
}
