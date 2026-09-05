package com.hansana.helpdesk.auth.security;

import com.hansana.helpdesk.user.entity.User;
import com.hansana.helpdesk.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret must be configured and non-blank.");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 256 bits (32 bytes) long.");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);
        return generateToken(user, now, expiry);
    }

    public String generateToken(User user, Instant issuedAt, Instant expiresAt) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("User email cannot be null or blank");
        }
        if (user.getRole() == null) {
            throw new IllegalArgumentException("User role cannot be null");
        }

        var builder = Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt));

        if (user.getId() != null) {
            builder.claim("userId", user.getId().toString());
        }

        return builder.signWith(signingKey).compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractEmail(String token) {
        return extractUsername(token);
    }

    public UserRole extractRole(String token) {
        Claims claims = extractAllClaims(token);
        String roleStr = claims.get("role", String.class);
        if (roleStr == null || roleStr.isBlank()) {
            throw new IllegalArgumentException("Token does not contain a role claim");
        }
        return UserRole.valueOf(roleStr);
    }

    public UUID extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        String userIdStr = claims.get("userId", String.class);
        if (userIdStr == null || userIdStr.isBlank()) {
            return null;
        }
        return UUID.fromString(userIdStr);
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Claims claims = extractAllClaims(token);
            if (claims.getSubject() == null || claims.getSubject().isBlank()) {
                return false;
            }
            String roleStr = claims.get("role", String.class);
            if (roleStr == null || roleStr.isBlank()) {
                return false;
            }
            UserRole.valueOf(roleStr);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
