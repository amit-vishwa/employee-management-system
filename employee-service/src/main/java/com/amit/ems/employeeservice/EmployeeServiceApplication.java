package com.amit.ems.employeeservice;

import com.amit.ems.common.config.EnableCorrelationIds;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableCorrelationIds
public class EmployeeServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(
				EmployeeServiceApplication.class,
				args
		);
	}
}