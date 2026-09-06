package com.amit.ems.authservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        // Passing means the complete auth-service Spring context,
        // security configuration, JPA layer, and test datasource are valid.
    }

    @Test
    void shouldNotCreateDefaultUserDetailsService() {
        assertThat(applicationContext.getBeansOfType(UserDetailsService.class))
                .isEmpty();
    }
}
