package com.amit.ems.authservice.controller;

import com.amit.ems.authservice.dto.AuthResponse;
import com.amit.ems.authservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import com.amit.ems.authservice.dto.RegisterRequest;
import com.amit.ems.authservice.exception.UsernameAlreadyExistsException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void register_shouldReturnSuccessForValidRequest() throws Exception {
        doNothing().when(authService).register(
                argThat(request ->
                        request != null
                                && "amit".equals(request.getUsername())
                                && "password123".equals(request.getPassword())
                )
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "amit",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "User registered successfully"
                ));

        verify(authService).register(
                argThat(request ->
                        "amit".equals(request.getUsername())
                                && "password123".equals(
                                        request.getPassword()
                                )
                )
        );
    }

    @Test
    void login_shouldReturnTokenForValidRequest() throws Exception {
        when(authService.login(
                argThat(request ->
                        request != null
                                && "amit".equals(request.getUsername())
                                && "password123".equals(
                                        request.getPassword()
                                )
                )
        )).thenReturn(new AuthResponse("generated-jwt"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "amit",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token")
                        .value("generated-jwt"));
    }

    @Test
    void register_shouldReturnBadRequestForInvalidRequest()
            throws Exception {

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/auth/register"))
                .andExpect(jsonPath("$.message")
                        .isNotEmpty());
    }

    @Test
    void login_shouldReturnUnauthorizedForInvalidCredentials()
            throws Exception {

        when(authService.login(
                argThat(request ->
                        request != null
                                && "amit".equals(request.getUsername())
                )
        )).thenThrow(
                new BadCredentialsException(
                        "Invalid username or password"
                )
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "amit",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message")
                        .value("Invalid username or password"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/auth/login"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void register_shouldReturnConflictForDuplicateUsername()
            throws Exception {

        doThrow(new UsernameAlreadyExistsException("amit"))
                .when(authService)
                .register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "username": "amit",
                              "password": "password123"
                            }
                            """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("Username already exists: amit"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/auth/register"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}