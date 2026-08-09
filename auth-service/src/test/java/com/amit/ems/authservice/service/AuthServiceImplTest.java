package com.amit.ems.authservice.service;

import com.amit.ems.authservice.dto.AuthRequest;
import com.amit.ems.authservice.dto.AuthResponse;
import com.amit.ems.authservice.dto.RegisterRequest;
import com.amit.ems.authservice.entity.Role;
import com.amit.ems.authservice.entity.User;
import com.amit.ems.authservice.exception.UsernameAlreadyExistsException;
import com.amit.ems.authservice.repository.UserRepository;
import com.amit.ems.authservice.service.impl.AuthServiceImpl;
import com.amit.ems.common.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_shouldEncodePasswordAndSaveUser() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("amit");
        request.setPassword("plain-password");

        when(passwordEncoder.encode("plain-password"))
                .thenReturn("encoded-password");

        authService.register(request);

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(passwordEncoder).encode("plain-password");
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals("amit", savedUser.getUsername());
        assertEquals("encoded-password", savedUser.getPassword());
        assertEquals(Role.EMPLOYEE, savedUser.getRole());
    }

    @Test
    void register_shouldRejectExistingUsername() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("amit");
        request.setPassword("plain-password");

        when(userRepository.existsByUsername("amit"))
                .thenReturn(true);

        UsernameAlreadyExistsException exception = assertThrows(
                UsernameAlreadyExistsException.class,
                () -> authService.register(request)
        );

        assertEquals(
                "Username already exists: amit",
                exception.getMessage()
        );

        verify(userRepository).existsByUsername("amit");
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void register_shouldTranslateConcurrentDuplicateConstraintViolation() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("amit");
        request.setPassword("plain-password");

        when(userRepository.existsByUsername("amit"))
                .thenReturn(false);

        when(passwordEncoder.encode("plain-password"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenThrow(
                        new DataIntegrityViolationException(
                                "Duplicate username"
                        )
                );

        UsernameAlreadyExistsException exception = assertThrows(
                UsernameAlreadyExistsException.class,
                () -> authService.register(request)
        );

        assertEquals(
                "Username already exists: amit",
                exception.getMessage()
        );

        verify(userRepository).existsByUsername("amit");
        verify(passwordEncoder).encode("plain-password");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void login_shouldReturnTokenWhenCredentialsAreValid() {
        AuthRequest request = createAuthRequest(
                "amit",
                "plain-password"
        );

        User storedUser = createUser(
                "amit",
                "encoded-password",
                Role.ADMIN
        );

        when(userRepository.findByUsername("amit"))
                .thenReturn(Optional.of(storedUser));

        when(passwordEncoder.matches(
                "plain-password",
                "encoded-password"
        )).thenReturn(true);

        when(jwtUtil.generateToken("amit", "ROLE_ADMIN"))
                .thenReturn("generated-jwt");

        AuthResponse response = authService.login(request);

        assertEquals("generated-jwt", response.getToken());

        verify(userRepository).findByUsername("amit");
        verify(passwordEncoder).matches(
                "plain-password",
                "encoded-password"
        );
        verify(jwtUtil).generateToken(
                "amit",
                "ROLE_ADMIN"
        );
    }

    @Test
    void login_shouldRejectUnknownUsername() {
        AuthRequest request = createAuthRequest(
                "unknown",
                "some-password"
        );

        when(userRepository.findByUsername("unknown"))
                .thenReturn(Optional.empty());

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );

        assertEquals(
                "Invalid username or password",
                exception.getMessage()
        );

        verify(userRepository).findByUsername("unknown");
        verifyNoInteractions(passwordEncoder, jwtUtil);
    }

    @Test
    void login_shouldRejectIncorrectPassword() {
        AuthRequest request = createAuthRequest(
                "amit",
                "wrong-password"
        );

        User storedUser = createUser(
                "amit",
                "encoded-password",
                Role.ADMIN
        );

        when(userRepository.findByUsername("amit"))
                .thenReturn(Optional.of(storedUser));

        when(passwordEncoder.matches(
                "wrong-password",
                "encoded-password"
        )).thenReturn(false);

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );

        assertEquals(
                "Invalid username or password",
                exception.getMessage()
        );

        verify(userRepository).findByUsername("amit");
        verify(passwordEncoder).matches(
                "wrong-password",
                "encoded-password"
        );
        verify(jwtUtil, never())
                .generateToken("amit", "ROLE_ADMIN");
    }

    private AuthRequest createAuthRequest(
            String username,
            String password
    ) {
        AuthRequest request = new AuthRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private User createUser(
            String username,
            String encodedPassword,
            Role role
    ) {
        return User.builder()
                .id(1L)
                .username(username)
                .password(encodedPassword)
                .role(role)
                .build();
    }

}