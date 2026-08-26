package com.hermes.user.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The post-split JwtService takes secret + expirationMs via its constructor (bound from
// jwt.secret / jwt.expiration-ms in production) instead of the old monolith's no-arg constructor
// with an internally generated key - so every test here builds its own instance directly.
// It also no longer has an extractRole method: JwtAuthFilter re-derives authorities from the DB
// via UserService.loadUserByUsername rather than trusting a role claim in the token.
class JwtServiceTest {

    // Base64, decodes to exactly 32 bytes - the minimum the constructor accepts.
    private static final String VALID_SECRET = "vTjn89nwZ1y4e1j9w9EgvYynGxHYY9EcvY//zXVsqkU=";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(VALID_SECRET, 3_600_000);
    }

    @Test
    void constructor_throws_whenSecretDecodesToFewerThan32Bytes() {
        String shortSecret = java.util.Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new JwtService(shortSecret, 3_600_000))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generateToken_thenExtractEmail_roundTrips() {
        String token = jwtService.generateToken("driver@example.com", "USER");

        assertThat(jwtService.extractEmail(token)).isEqualTo("driver@example.com");
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void isValid_returnsFalse_forMalformedToken() {
        assertThat(jwtService.isValid("not-a-real-jwt")).isFalse();
    }

    @Test
    void isValid_returnsFalse_forTokenSignedByDifferentKey() {
        String otherSecret = java.util.Base64.getEncoder().encodeToString(new byte[32]);
        JwtService otherService = new JwtService(otherSecret, 3_600_000);
        String tokenFromOtherService = otherService.generateToken("driver@example.com", "USER");

        assertThat(jwtService.isValid(tokenFromOtherService)).isFalse();
    }

    @Test
    void isValid_returnsFalse_forExpiredToken() throws InterruptedException {
        JwtService shortLivedService = new JwtService(VALID_SECRET, 1);
        String token = shortLivedService.generateToken("driver@example.com", "USER");

        Thread.sleep(20);

        assertThat(shortLivedService.isValid(token)).isFalse();
    }

    @Test
    void extractEmail_withTamperedToken_throws() {
        String token = jwtService.generateToken("driver@example.com", "USER");
        String[] parts = token.split("\\.");
        String tamperedPayload = parts[1].substring(0, parts[1].length() - 1)
                + (parts[1].endsWith("A") ? "B" : "A");
        String tampered = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertThatThrownBy(() -> jwtService.extractEmail(tampered)).isInstanceOf(Exception.class);
    }
}
