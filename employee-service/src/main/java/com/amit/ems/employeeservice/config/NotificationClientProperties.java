package com.amit.ems.employeeservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "notification.client")
public record NotificationClientProperties(
        Duration connectTimeout,
        Duration readTimeout
) {
}