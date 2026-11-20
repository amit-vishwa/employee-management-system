package com.amit.ems.employeeservice.integration;

import com.amit.ems.employeeservice.dto.EmployeeDto;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Request-only SQL counts, with committed JDBC fixtures and no outer test transaction. */
@TestPropertySource(properties = {
        "spring.jpa.open-in-view=false",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.cache.use_second_level_cache=false",
        "spring.jpa.properties.hibernate.cache.use_query_cache=false",
        "logging.level.org.hibernate.stat=OFF",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"
})
abstract class AbstractEmployeeListBaseline {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper mapper;
    @Autowired EntityManagerFactory emf;

    @BeforeAll
    static void startReport(TestInfo info) throws Exception {
        Path report = Path.of("target", info.getTestClass().orElseThrow().getSimpleName() + ".csv");
        Files.createDirectories(report.getParent());
        Files.writeString(report, "employees,departments,sql_statements,response_bytes\n", StandardCharsets.UTF_8);

    }

    @ParameterizedTest(name = "employees={0}, departments={1}")
    @CsvSource({"0,0,2", "10,0,2084", "10,1,2154", "10,10,2156",
            "100,1,21777", "100,10,21797", "100,100,21961"})
    void measureEmployeeList(int employees, int departments, int expectedBytes) throws Exception {
        Path report = Path.of("target", getClass().getSimpleName() + ".csv");
        if (employees == 0) {
            try (Connection connection = jdbc.getDataSource().getConnection()) {
                var metadata = connection.getMetaData();
                System.out.printf("BASELINE_ENV java=%s database=%s %s hibernate=%s%n",
                        System.getProperty("java.version"), metadata.getDatabaseProductName(),
                        metadata.getDatabaseProductVersion(), org.hibernate.Version.getVersionString());
            }
        }
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(emf.getProperties().get("hibernate.generate_statistics").toString()).isEqualTo("true");
        // This test owns its isolated database. Explicit IDs keep serialized payloads reproducible.
        jdbc.update("DELETE FROM employees");
        jdbc.update("DELETE FROM departments");
        for (int id = 1; id <= departments; id++) {
            jdbc.update("INSERT INTO departments (id,name) VALUES (?,?)", id, "Department-" + id);
        }
        for (int id = 1; id <= employees; id++) {
            Integer department = departments == 0 ? null : (id - 1) % departments + 1;
            jdbc.update("INSERT INTO employees (id,first_name,last_name,email,designation,date_of_joining,department_id) "
                            + "VALUES (?,?,?,?,?,?,?)", id, "Employee-" + id, "Example",
                    "employee-" + id + "@example.invalid", "Engineer", java.sql.Date.valueOf("2026-01-01"), department);
        }
        var factory = emf.unwrap(SessionFactory.class);
        var statistics = factory.getStatistics();
        long firstQueries = -1;
        int firstBytes = -1;
        // Two separate requests prove repeatability without a reused persistence context.
        for (int repetition = 0; repetition < 2; repetition++) {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            factory.getCache().evictAllRegions();
            statistics.clear();
            byte[] response = mvc.perform(get("/api/v1/employees").with(user("baseline").roles("ADMIN")))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
            long queries = statistics.getPrepareStatementCount();
            EmployeeDto[] rows = mapper.readValue(response, EmployeeDto[].class);
            assertThat(rows).hasSize(employees);
            assertThat(rows).extracting(EmployeeDto::getId).doesNotHaveDuplicates();
            for (EmployeeDto row : rows) {
                assertThat(row.getId()).isBetween(1L, (long) employees);
                assertThat(row.getFirstName()).isEqualTo("Employee-" + row.getId());
                assertThat(row.getLastName()).isEqualTo("Example");
                assertThat(row.getEmail()).isEqualTo("employee-" + row.getId() + "@example.invalid");
                assertThat(row.getAuthUsername()).isNull();
                assertThat(row.getDesignation()).isEqualTo("Engineer");
                assertThat(row.getDateOfJoining()).isEqualTo(java.time.LocalDate.of(2026, 1, 1));
                Long expected = departments == 0 ? null : (row.getId() - 1) % departments + 1;
                assertThat(row.getDepartmentId()).isEqualTo(expected);
                assertThat(row.getDepartmentName()).isEqualTo(expected == null ? null : "Department-" + expected);
            }
            assertThat(queries).as("employee list fetches departments in one statement").isEqualTo(1);
            assertThat(response.length).as("serialized size matches the pre-optimization fixture")
                    .isEqualTo(expectedBytes);
            if (repetition == 0) {
                firstQueries = queries;
                firstBytes = response.length;
            } else {
                assertThat(queries).as("cold-request SQL count is repeatable").isEqualTo(firstQueries);
                assertThat(response.length).as("serialized byte count is repeatable").isEqualTo(firstBytes);
            }
        }
        String line = String.format("%d,%d,%d,%d%n", employees, departments, firstQueries, firstBytes);
        Files.writeString(report, line, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        System.out.print("BASELINE " + line);
    }
}
