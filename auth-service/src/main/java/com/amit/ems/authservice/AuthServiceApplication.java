package com.amit.ems.authservice;

import com.amit.ems.common.config.EnableCorrelationIds;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableCorrelationIds
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                AuthServiceApplication.class,
                args
        );
    }
}