package com.amit.ems.employeeservice.dto;

import com.amit.ems.employeeservice.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotNull
    private Role role;
}