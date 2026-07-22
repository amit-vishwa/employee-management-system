package com.amit.ems.authservice.service.impl;

import com.amit.ems.authservice.dto.AuthRequest;
import com.amit.ems.authservice.dto.AuthResponse;
import com.amit.ems.authservice.dto.RegisterRequest;
import com.amit.ems.authservice.entity.User;
import com.amit.ems.authservice.repository.UserRepository;
import com.amit.ems.authservice.service.AuthService;
import com.amit.ems.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
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
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(user.getUsername(), "ROLE_" + user.getRole().name());
        log.info("User logged in: {}", request.getUsername());
        return new AuthResponse(token);
    }
}