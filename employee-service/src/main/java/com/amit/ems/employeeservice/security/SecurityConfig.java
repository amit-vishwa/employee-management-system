package com.amit.ems.employeeservice.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static com.amit.ems.common.logging.LogSanitizer.sanitize;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(
                                        HttpStatus.UNAUTHORIZED
                                )
                        )
                        .accessDeniedHandler(
                                (request, response, exception) -> {
                                    Authentication authentication =
                                            SecurityContextHolder
                                                    .getContext()
                                                    .getAuthentication();

                                    String username =
                                            authentication != null
                                                    ? authentication.getName()
                                                    : "anonymous";

                                    log.warn(
                                            "security_event=access_denied "
                                                    + "username={} "
                                                    + "method={} path={}",
                                            sanitize(username),
                                            sanitize(request.getMethod()),
                                            sanitize(request.getRequestURI())
                                    );

                                    response.setStatus(
                                            HttpServletResponse
                                                    .SC_FORBIDDEN
                                    );
                                }
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/employees/me"
                        ).hasRole("EMPLOYEE")
                        .requestMatchers(
                                "/api/v1/departments/**"
                        ).hasAnyRole("ADMIN", "HR")
                        .requestMatchers(
                                "/api/v1/employees/**"
                        ).hasAnyRole("ADMIN", "HR")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}