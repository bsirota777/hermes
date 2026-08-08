package com.hermes.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
    }

    @Test
    void generateToken_withEmailOnly_canBeExtractedBack() {
        String token = jwtService.generateToken("driver@example.com");

        assertThat(jwtService.extractEmail(token)).isEqualTo("driver@example.com");
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void generateToken_withEmailAndRole_carriesRoleClaim() {
        String token = jwtService.generateToken("admin@example.com", "ADMIN");

        assertThat(jwtService.extractEmail(token)).isEqualTo("admin@example.com");
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void extractRole_onTokenGeneratedWithoutRole_returnsNull() {
        String token = jwtService.generateToken("driver@example.com");

        assertThat(jwtService.extractRole(token)).isNull();
    }

    @Test
    void isValid_withMalformedToken_returnsFalse() {
        assertThat(jwtService.isValid("not-a-real-jwt")).isFalse();
    }

    @Test
    void isValid_withTokenSignedByDifferentKey_returnsFalse() {
        JwtService otherService = new JwtService();
        String tokenFromOtherService = otherService.generateToken("driver@example.com");

        assertThat(jwtService.isValid(tokenFromOtherService)).isFalse();
    }

    @Test
    void extractEmail_withTamperedToken_throws() {
        String token = jwtService.generateToken("driver@example.com");
        String[] parts = token.split("\\.");
        // Corrupt a character in the middle of the payload segment
        String tamperedPayload = parts[1].substring(0, parts[1].length() - 1)
                + (parts[1].endsWith("A") ? "B" : "A");
        String tampered = parts[0] + "." + tamperedPayload + "." + parts[2];

        org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> jwtService.extractEmail(tampered));
    }
}