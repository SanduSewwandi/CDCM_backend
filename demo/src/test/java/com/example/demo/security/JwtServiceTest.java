package com.example.demo.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
    }

    @Test
    void generateToken_ShouldCreateToken() {
        String token = jwtService.generateToken(
                "patient@gmail.com",
                "PATIENT"
        );

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void extractEmail_ShouldReturnCorrectEmail() {
        String token = jwtService.generateToken(
                "patient@gmail.com",
                "PATIENT"
        );

        String email = jwtService.extractEmail(token);

        assertEquals("patient@gmail.com", email);
    }

    @Test
    void extractRole_ShouldReturnCorrectRole() {
        String token = jwtService.generateToken(
                "doctor@gmail.com",
                "DOCTOR"
        );

        String role = jwtService.extractRole(token);

        assertEquals("DOCTOR", role);
    }

    @Test
    void isTokenValid_WithValidToken_ShouldReturnTrue() {
        String token = jwtService.generateToken(
                "hospital@gmail.com",
                "HOSPITAL"
        );

        boolean result = jwtService.isTokenValid(token);

        assertTrue(result);
    }

    @Test
    void isTokenValid_WithInvalidToken_ShouldReturnFalse() {
        String invalidToken = "invalid.jwt.token";

        boolean result = jwtService.isTokenValid(invalidToken);

        assertFalse(result);
    }

    @Test
    void isTokenValid_WithTamperedToken_ShouldReturnFalse() {
        String token = jwtService.generateToken(
                "patient@gmail.com",
                "PATIENT"
        );

        String tamperedToken = token + "tampered";

        boolean result = jwtService.isTokenValid(tamperedToken);

        assertFalse(result);
    }
}