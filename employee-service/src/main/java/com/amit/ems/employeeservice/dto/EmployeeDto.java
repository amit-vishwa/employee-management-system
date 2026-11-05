package com.amit.ems.employeeservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDto {

    private Long id;

    @NotBlank(message = "First name is required")
    @Size(max = 255, message = "firstName must not exceed 255 characters")
    @Schema(maxLength = 255)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 255, message = "lastName must not exceed 255 characters")
    @Schema(maxLength = 255)
    private String lastName;

    @Email(message = "Valid email is required")
    @NotBlank(message = "Email is required")
    @Size(max = 255, message = "email must not exceed 255 characters")
    @Schema(maxLength = 255)
    private String email;

    @Schema(
            description = "Authentication username linked to this employee",
            example = "amit",
            maxLength = 255,
            nullable = true
    )
    @Size(max = 255, message = "authUsername must not exceed 255 characters")
    private String authUsername;

    @Size(max = 255, message = "designation must not exceed 255 characters")
    @Schema(maxLength = 255)
    private String designation;

    private LocalDate dateOfJoining;

    private Long departmentId;

    private String departmentName;
}