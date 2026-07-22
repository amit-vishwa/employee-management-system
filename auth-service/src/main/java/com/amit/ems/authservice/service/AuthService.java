package com.amit.ems.authservice.service;

import com.amit.ems.authservice.dto.AuthRequest;
import com.amit.ems.authservice.dto.AuthResponse;
import com.amit.ems.authservice.dto.RegisterRequest;

public interface AuthService {
    void register(RegisterRequest request);
    AuthResponse login(AuthRequest request);
}