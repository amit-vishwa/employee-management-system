package com.amit.ems.notificationservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notification API")
                        .version("1.0.0")
                        .description(
                                "Receives employee-created events and performs "
                                        + "best-effort notification processing. "
                                        + "The current implementation records "
                                        + "notification activity in application logs."
                        )
                        .contact(new Contact()
                                .name("Amit Vishwakarma")
                        ));
    }
}