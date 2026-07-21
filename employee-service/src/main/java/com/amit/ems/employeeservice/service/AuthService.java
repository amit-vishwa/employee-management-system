package com.amit.ems.employeeservice.service;

import com.amit.ems.employeeservice.dto.AuthRequest;
import com.amit.ems.employeeservice.dto.AuthResponse;
import com.amit.ems.employeeservice.dto.RegisterRequest;

public interface AuthService {
    void register(RegisterRequest request);
    AuthResponse login(AuthRequest request);
}