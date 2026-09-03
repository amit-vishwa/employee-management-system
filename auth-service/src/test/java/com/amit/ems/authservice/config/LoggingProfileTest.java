package com.amit.ems.authservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingProfileTest {

    private ApplicationContextRunner runner(String profile) {
        return new ApplicationContextRunner()
                .withPropertyValues(
                        "spring.config.location=classpath:/application.yml",
                        "spring.profiles.active=" + profile
                )
                .withInitializer(
                        new ConfigDataApplicationContextInitializer()
                );
    }

    @Test
    void defaultProfileShouldUseRestrainedLogging() {
        runner("default").run(context -> {
            assertThat(context).hasNotFailed();

            Environment environment = context.getEnvironment();

            assertThat(environment.getProperty("logging.level.root"))
                    .isEqualTo("INFO");
            assertThat(environment.getProperty("logging.level.com.amit.ems"))
                    .isEqualTo("INFO");
            assertThat(environment.getProperty("logging.level.org.hibernate.SQL"))
                    .isEqualTo("WARN");
            assertThat(environment.getProperty("spring.jpa.show-sql", Boolean.class))
                    .isFalse();
            assertThat(environment.getProperty(
                    "spring.jpa.properties.hibernate.format_sql",
                    Boolean.class
            )).isFalse();

            assertSensitiveLoggingRemainsDisabled(environment);
            assertCorrelationPatternIsPreserved(environment);
        });
    }

    @Test
    void devProfileShouldEnableDiagnosticsWithoutParameterValues() {
        runner("dev").run(context -> {
            assertThat(context).hasNotFailed();

            Environment environment = context.getEnvironment();

            assertThat(environment.getProperty("logging.level.root"))
                    .isEqualTo("INFO");
            assertThat(environment.getProperty("logging.level.com.amit.ems"))
                    .isEqualTo("DEBUG");
            assertThat(environment.getProperty("logging.level.org.hibernate.SQL"))
                    .isEqualTo("DEBUG");
            assertThat(environment.getProperty("spring.jpa.show-sql", Boolean.class))
                    .isFalse();
            assertThat(environment.getProperty(
                    "spring.jpa.properties.hibernate.format_sql",
                    Boolean.class
            )).isTrue();

            assertSensitiveLoggingRemainsDisabled(environment);
            assertCorrelationPatternIsPreserved(environment);
        });
    }

    private void assertSensitiveLoggingRemainsDisabled(Environment environment) {
        assertThat(environment.getProperty(
                "logging.level.org.hibernate.orm.jdbc.bind"
        )).isEqualTo("OFF");

        assertThat(environment.getProperty(
                "logging.level.org.hibernate.orm.jdbc.extract"
        )).isEqualTo("OFF");
    }

    private void assertCorrelationPatternIsPreserved(Environment environment) {
        assertThat(environment.getProperty("logging.pattern.console"))
                .contains("[correlationId=%X{correlationId:-none}]");
    }
}