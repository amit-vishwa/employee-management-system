package com.amit.ems.employeeservice.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class DepartmentConflictMySqlIT extends AbstractDepartmentConflictContract {
    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0")
            .withDatabaseName("ems_db")
            .withUsername("department_test_user")
            .withPassword("department_test_password");

    @DynamicPropertySource
    static void useMigratedDisposableMySql(DynamicPropertyRegistry properties) {
        String migrations = System.getProperty("mysql.migration.location");
        assertThat(migrations).as("Use the mysql-integration-tests Maven profile").isNotBlank();
        properties.add("spring.datasource.url", MYSQL::getJdbcUrl);
        properties.add("spring.datasource.username", MYSQL::getUsername);
        properties.add("spring.datasource.password", MYSQL::getPassword);
        properties.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        properties.add("spring.flyway.enabled", () -> "true");
        properties.add("spring.flyway.locations", () -> migrations);
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }
}
