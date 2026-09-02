package com.amit.ems.employeeservice.integration;

import com.amit.ems.employeeservice.EmployeeServiceApplication;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(
        classes = EmployeeServiceApplication.class,
        properties = {
                "jwt.secret=testcontainers-employee-jwt-secret-that-is-at-least-32-bytes",
                "spring.jpa.show-sql=false",
                "spring.jpa.properties.hibernate.format_sql=false"
        }
)
class EmployeeMySqlIntegrationIT {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("ems_db")
                    .withUsername("employee_test_user")
                    .withPassword("employee_test_password");

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM employees");
        jdbcTemplate.update("DELETE FROM departments");
    }

    @Test
    void flywayShouldCreateTheEmployeeSchemaOnMySql() {
        assertThat(flyway.info().current()).isNotNull();
        assertThat(flyway.info().current().getVersion().getVersion())
                .isEqualTo("1");

        assertThat(tableCount("departments")).isEqualTo(1);
        assertThat(tableCount("employees")).isEqualTo(1);

        assertThat(indexCount(
                "employees",
                "uk_employees_email",
                true
        )).isGreaterThanOrEqualTo(1);

        assertThat(indexCount(
                "employees",
                "uk_employees_auth_username",
                true
        )).isGreaterThanOrEqualTo(1);

        assertThat(indexCount(
                "employees",
                "idx_employees_department_id",
                false
        )).isGreaterThanOrEqualTo(1);

        Integer foreignKeyCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.referential_constraints
                WHERE constraint_schema = DATABASE()
                  AND table_name = 'employees'
                  AND constraint_name = 'fk_employees_department'
                """,
                Integer.class
        );

        assertThat(foreignKeyCount).isEqualTo(1);
    }

    @Test
    void employeesTableShouldEnforceUniqueEmailAddresses() {
        Long departmentId = insertDepartment("Engineering");

        insertEmployee(
                "first.employee@example.com",
                "first-employee",
                departmentId
        );

        assertThatThrownBy(
                () -> insertEmployee(
                        "first.employee@example.com",
                        "second-employee",
                        departmentId
                )
        ).isInstanceOf(DataAccessException.class);
    }

    @Test
    void employeesTableShouldEnforceUniqueAuthUsernames() {
        Long departmentId = insertDepartment("Human Resources");

        insertEmployee(
                "employee.one@example.com",
                "shared-auth-user",
                departmentId
        );

        assertThatThrownBy(
                () -> insertEmployee(
                        "employee.two@example.com",
                        "shared-auth-user",
                        departmentId
                )
        ).isInstanceOf(DataAccessException.class);
    }

    @Test
    void employeesTableShouldRejectUnknownDepartments() {
        assertThatThrownBy(
                () -> insertEmployee(
                        "orphan.employee@example.com",
                        "orphan-employee",
                        Long.MAX_VALUE
                )
        ).isInstanceOf(DataAccessException.class);
    }

    private Integer tableCount(String tableName) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """,
                Integer.class,
                tableName
        );
    }

    private Integer indexCount(
            String tableName,
            String indexName,
            boolean unique
    ) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                  AND non_unique = ?
                """,
                Integer.class,
                tableName,
                indexName,
                unique ? 0 : 1
        );
    }

    private Long insertDepartment(String name) {
        jdbcTemplate.update(
                "INSERT INTO departments (name) VALUES (?)",
                name
        );

        return jdbcTemplate.queryForObject(
                "SELECT id FROM departments WHERE name = ?",
                Long.class,
                name
        );
    }

    private void insertEmployee(
            String email,
            String authUsername,
            Long departmentId
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO employees (
                    first_name,
                    last_name,
                    email,
                    auth_username,
                    designation,
                    date_of_joining,
                    department_id
                )
                VALUES (?, ?, ?, ?, ?, CURRENT_DATE, ?)
                """,
                "Test",
                "Employee",
                email,
                authUsername,
                "Software Engineer",
                departmentId
        );
    }
}