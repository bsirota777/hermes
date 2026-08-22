package com.hermes.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

// Verify-only counterpart to user-service's JwtService: every other service needs to
// check the token user-service issued, not mint new ones. Not a Spring bean itself -
// each service wires it as a @Bean in its own SecurityConfig, passing its jwt.secret
// (must be the SAME secret value as user-service's, via the shared JWT_SECRET env var).
public class JwtValidator {

    private final SecretKey key;

    public JwtValidator(String secret) {
        byte[] decoded = Base64.getDecoder().decode(secret);
        if (decoded.length < 32) {
            throw new IllegalArgumentException("JWT secret must decode to at least 32 bytes");
        }
        this.key = new SecretKeySpec(decoded, "HmacSHA256");
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
