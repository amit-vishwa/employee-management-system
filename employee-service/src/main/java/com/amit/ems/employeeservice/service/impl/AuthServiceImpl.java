package com.amit.ems.employeeservice.service.impl;

import com.amit.ems.employeeservice.dto.AuthRequest;
import com.amit.ems.employeeservice.dto.AuthResponse;
import com.amit.ems.employeeservice.dto.RegisterRequest;
import com.amit.ems.employeeservice.entity.User;
import com.amit.ems.employeeservice.repository.UserRepository;
import com.amit.ems.employeeservice.security.JwtUtil;
import com.amit.ems.employeeservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Override
    public void register(RegisterRequest request) {
        log.info("Registering user: {}", request.getUsername());
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();
        userRepository.save(user);
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);
        log.info("User logged in: {}", request.getUsername());
        return new AuthResponse(token);
    }
}