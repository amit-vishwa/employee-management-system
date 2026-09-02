package com.amit.ems.authservice.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class AuthMySqlIntegrationIT {

    private static final String MIGRATION_LOCATION =
            "classpath:db/migration";

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("auth_db")
                    .withUsername("auth_test_user")
                    .withPassword("auth_test_password");

    private static Flyway flyway;
    private static JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void initializeDatabase() {
        flyway = Flyway.configure()
                .dataSource(
                        MYSQL.getJdbcUrl(),
                        MYSQL.getUsername(),
                        MYSQL.getPassword()
                )
                .locations(MIGRATION_LOCATION)
                .failOnMissingLocations(true)
                .load();

        var migrationResult = flyway.migrate();

        assertThat(migrationResult.migrationsExecuted)
                .as("Flyway must execute the auth-service MySQL migration")
                .isGreaterThan(0);

        DriverManagerDataSource dataSource =
                new DriverManagerDataSource(
                        MYSQL.getJdbcUrl(),
                        MYSQL.getUsername(),
                        MYSQL.getPassword()
                );

        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void flywayShouldCreateTheAuthSchemaOnMySql() {
        assertThat(flyway.info().current()).isNotNull();

        assertThat(flyway.info().current().getVersion().getVersion())
                .isEqualTo("1");

        Integer tableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = 'users'
                """,
                Integer.class
        );

        Integer uniqueIndexCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'users'
                  AND index_name = 'uk_users_username'
                  AND non_unique = 0
                """,
                Integer.class
        );

        assertThat(tableCount).isEqualTo(1);
        assertThat(uniqueIndexCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    void usersTableShouldEnforceUniqueUsernames() {
        insertUser("duplicate-user", "EMPLOYEE");

        assertThatThrownBy(
                () -> insertUser("duplicate-user", "HR")
        ).isInstanceOf(DataAccessException.class);
    }

    @Test
    void usersTableShouldRejectUnsupportedRoles() {
        assertThatThrownBy(
                () -> insertUser(
                        "invalid-role-user",
                        "SUPER_ADMIN"
                )
        ).isInstanceOf(DataAccessException.class);
    }

    private void insertUser(
            String username,
            String role
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO users (
                    username,
                    password,
                    role
                )
                VALUES (?, ?, ?)
                """,
                username,
                "test-password-hash",
                role
        );
    }
}