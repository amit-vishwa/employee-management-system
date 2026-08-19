package com.amit.ems.employeeservice.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url="
                + "jdbc:h2:mem:employee_migration_test;"
                + "MODE=MySQL;"
                + "DB_CLOSE_DELAY=-1;"
                + "DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@ActiveProfiles("test")
class EmployeeFlywayMigrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migration_shouldCreateVersionedEmployeeSchema() {
        assertThat(flyway.info().current())
                .isNotNull();

        assertThat(
                flyway.info()
                        .current()
                        .getVersion()
                        .getVersion()
        ).isEqualTo("1");

        assertThat(tableExists("DEPARTMENTS"))
                .isTrue();

        assertThat(tableExists("EMPLOYEES"))
                .isTrue();

        assertThat(columnExists(
                "EMPLOYEES",
                "DEPARTMENT_ID"
        )).isTrue();

        assertThat(columnExists(
                "EMPLOYEES",
                "AUTH_USERNAME"
        )).isTrue();

        assertThat(columnExists(
                "EMPLOYEES",
                "DATE_OF_JOINING"
        )).isTrue();
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = 'PUBLIC'
                  AND TABLE_NAME = ?
                """,
                Integer.class,
                tableName
        );

        return count != null && count == 1;
    }

    private boolean columnExists(
            String tableName,
            String columnName
    ) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = 'PUBLIC'
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """,
                Integer.class,
                tableName,
                columnName
        );

        return count != null && count == 1;
    }
}