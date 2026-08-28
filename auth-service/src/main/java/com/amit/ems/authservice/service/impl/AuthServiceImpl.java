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

import static com.amit.ems.common.logging.LogSanitizer.sanitize;

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
        String safeUsername = sanitize(username);

        log.info(
                "security_event=registration_attempt username={}",
                safeUsername
        );

        if (userRepository.existsByUsername(username)) {
            log.warn(
                    "security_event=registration_rejected "
                            + "username={} reason=username_exists",
                    safeUsername
            );

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

            log.info(
                    "security_event=registration_succeeded "
                            + "username={} role={}",
                    safeUsername,
                    user.getRole()
            );
        } catch (DataIntegrityViolationException exception) {
            log.warn(
                    "security_event=registration_rejected "
                            + "username={} "
                            + "reason=concurrent_username_conflict",
                    safeUsername
            );

            throw new UsernameAlreadyExistsException(username);
        }
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        String username = request.getUsername();
        String safeUsername = sanitize(username);

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> {
                    log.warn(
                            "security_event=login_failed "
                                    + "username={} "
                                    + "reason=bad_credentials",
                            safeUsername
                    );

                    return new BadCredentialsException(
                            "Invalid username or password"
                    );
                });

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            log.warn(
                    "security_event=login_failed "
                            + "username={} "
                            + "reason=bad_credentials",
                    safeUsername
            );

            throw new BadCredentialsException(
                    "Invalid username or password"
            );
        }

        String token = jwtUtil.generateToken(
                user.getUsername(),
                "ROLE_" + user.getRole().name()
        );

        log.info(
                "security_event=login_succeeded "
                        + "username={} role={}",
                safeUsername,
                user.getRole()
        );

        return new AuthResponse(token);
    }
}