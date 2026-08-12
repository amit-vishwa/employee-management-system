package com.amit.ems.employeeservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI employeeServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Employee Management API")
                        .version("1.0.0")
                        .description(
                                "Manages employees and departments. "
                                        + "Business endpoints require an ADMIN "
                                        + "or HR JWT issued by auth-service."
                        )
                        .contact(new Contact()
                                .name("Amit Vishwakarma")
                        ))
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER_AUTH_SCHEME,
                                new SecurityScheme()
                                        .name(BEARER_AUTH_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description(
                                                "Enter the JWT issued by "
                                                        + "auth-service. Swagger UI "
                                                        + "adds the Bearer prefix."
                                        )
                        ));
    }
}