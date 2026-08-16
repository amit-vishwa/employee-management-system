package com.amit.ems.authservice.service.impl;

import com.amit.ems.authservice.dto.AuthRequest;
import com.amit.ems.authservice.dto.AuthResponse;
import com.amit.ems.authservice.dto.RegisterRequest;
import com.amit.ems.authservice.entity.Role;
import com.amit.ems.authservice.entity.User;
import com.amit.ems.authservice.repository.UserRepository;
import com.amit.ems.authservice.service.AuthService;
import com.amit.ems.common.security.JwtUtil;
import com.amit.ems.authservice.exception.UsernameAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        String username = request.getUsername();

        log.info("Registering user: {}", username);

        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException(username);
        }

        User user = User.builder()
                .username(username)
                .password(
                        passwordEncoder.encode(request.getPassword())
                )
                .role(Role.EMPLOYEE)
                .build();

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            /*
             * The pre-check improves the normal error path, while the
             * database unique constraint protects concurrent requests.
             */
            throw new UsernameAlreadyExistsException(username);
        }
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