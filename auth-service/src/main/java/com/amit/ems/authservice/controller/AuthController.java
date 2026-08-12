package com.amit.ems.authservice.controller;

import com.amit.ems.authservice.dto.AuthRequest;
import com.amit.ems.authservice.dto.AuthResponse;
import com.amit.ems.authservice.dto.RegisterRequest;
import com.amit.ems.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(
        name = "Authentication",
        description = "Public registration and login operations"
)
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
            summary = "Register a user",
            description = "Public registration always assigns EMPLOYEE"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Registration successful"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Username already exists"
            )
    })
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    @Operation(
            summary = "Authenticate and issue a JWT",
            description = "Authenticate and issue a JWT for use with protected services"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "JWT is missing or invalid"
            )
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}