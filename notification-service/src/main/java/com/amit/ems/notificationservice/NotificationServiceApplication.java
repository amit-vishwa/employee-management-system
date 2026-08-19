package com.amit.ems.notificationservice;

import com.amit.ems.common.config.EnableCorrelationIds;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableCorrelationIds
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                NotificationServiceApplication.class,
                args
        );
    }
}