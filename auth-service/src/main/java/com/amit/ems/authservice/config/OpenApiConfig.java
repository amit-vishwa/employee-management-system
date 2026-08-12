package com.amit.ems.authservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Authentication API")
                        .version("1.0.0")
                        .description(
                                "Registers users, validates credentials, "
                                        + "and issues JWTs for protected services. "
                                        + "Public registration always creates "
                                        + "an EMPLOYEE account."
                        )
                        .contact(new Contact()
                                .name("Amit Vishwakarma")
                        ));
    }
}