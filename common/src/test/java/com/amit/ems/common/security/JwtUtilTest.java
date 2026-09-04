package com.amit.ems.common.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String JWT_SECRET =
            "thisIsATestSecretKeyLongEnoughForSecureJwtSigning1234567890";

    private static final long EXPIRATION_MS = 3_600_000L;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = createJwtUtil(JWT_SECRET);
    }

    @Test
    void generateToken_shouldPreserveUsernameAndRole() {
        String token = jwtUtil.generateToken(
                "amit",
                "ROLE_ADMIN"
        );

        assertEquals(
                "amit",
                jwtUtil.extractUsername(token)
        );

        assertEquals(
                "ROLE_ADMIN",
                jwtUtil.extractRole(token)
        );
    }

    @Test
    void isTokenValid_shouldReturnTrueForMatchingUsername() {
        String token = jwtUtil.generateToken(
                "amit",
                "ROLE_HR"
        );

        assertTrue(
                jwtUtil.isTokenValid(token, "amit")
        );
    }

    @Test
    void isTokenValid_shouldReturnFalseForDifferentUsername() {
        String token = jwtUtil.generateToken(
                "amit",
                "ROLE_EMPLOYEE"
        );

        assertFalse(
                jwtUtil.isTokenValid(token, "different-user")
        );
    }

    @Test
    void extractUsername_shouldRejectTokenSignedWithDifferentSecret() {
        String token = jwtUtil.generateToken(
                "amit",
                "ROLE_ADMIN"
        );

        JwtUtil jwtUtilWithDifferentSecret = createJwtUtil(
                "anotherTestSecretKeyLongEnoughForJwtSigning0987654321"
        );

        assertThrows(
                JwtException.class,
                () -> jwtUtilWithDifferentSecret.extractUsername(token)
        );
    }

    @Test
    void isTokenValid_shouldRejectExpiredToken() {
        // Expire well in the past; no sleeps or timing-sensitive boundary checks.
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", -60_000L);
        String token = jwtUtil.generateToken("amit", "ROLE_EMPLOYEE");

        assertThrows(ExpiredJwtException.class,
                () -> jwtUtil.isTokenValid(token, "amit"));
    }

    @Test
    void isTokenValid_shouldRejectMalformedToken() {
        assertThrows(JwtException.class,
                () -> jwtUtil.isTokenValid("not-a-jwt", "amit"));
    }

    @Test
    void isTokenValid_shouldRejectUnsignedToken() {
        String token = Jwts.builder()
                .subject("amit")
                .claim("role", "ROLE_ADMIN")
                .compact();

        assertThrows(JwtException.class,
                () -> jwtUtil.isTokenValid(token, "amit"));
    }

    private JwtUtil createJwtUtil(String secret) {
        JwtUtil utility = new JwtUtil();

        ReflectionTestUtils.setField(
                utility,
                "secret",
                secret
        );

        ReflectionTestUtils.setField(
                utility,
                "expirationMs",
                EXPIRATION_MS
        );

        return utility;
    }
}
