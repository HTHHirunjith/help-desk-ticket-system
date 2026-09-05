package com.hansana.helpdesk.auth.security;

import com.hansana.helpdesk.user.entity.User;
import com.hansana.helpdesk.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String TEST_SECRET = "test-secret-key-that-is-at-least-32-bytes-long-for-hmac-sha256";
    private static final String OTHER_SECRET = "another-secret-key-that-is-also-at-least-32-bytes-long-hmac256";
    private static final long EXPIRATION_MS = 3600000L; // 1 hour

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, EXPIRATION_MS);
    }

    private User createSampleUser(UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("testuser@example.com");
        user.setPassword("hashed-secret-value");
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setRole(role);
        return user;
    }

    @Test
    void generatesTokenSuccessfullyForValidUser() {
        User user = createSampleUser(UserRole.USER);

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void extractsSubjectMatchingUserEmail() {
        User user = createSampleUser(UserRole.USER);

        String token = jwtService.generateToken(user);

        assertEquals("testuser@example.com", jwtService.extractUsername(token));
        assertEquals("testuser@example.com", jwtService.extractEmail(token));
    }

    @Test
    void extractsRoleCorrectlyForEachRole() {
        for (UserRole role : UserRole.values()) {
            User user = createSampleUser(role);
            String token = jwtService.generateToken(user);

            UserRole extractedRole = jwtService.extractRole(token);
            assertEquals(role, extractedRole);
        }
    }

    @Test
    void extractsUserIdCorrectly() {
        User user = createSampleUser(UserRole.SUPPORT_AGENT);
        String token = jwtService.generateToken(user);

        UUID extractedId = jwtService.extractUserId(token);
        assertEquals(user.getId(), extractedId);
    }

    @Test
    void validatesValidTokenSuccessfully() {
        User user = createSampleUser(UserRole.ADMIN);
        String token = jwtService.generateToken(user);

        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        JwtService otherService = new JwtService(OTHER_SECRET, EXPIRATION_MS);
        User user = createSampleUser(UserRole.USER);

        String tokenFromOther = otherService.generateToken(user);

        assertFalse(jwtService.validateToken(tokenFromOther));
    }

    @Test
    void rejectsExpiredToken() {
        User user = createSampleUser(UserRole.USER);
        Instant now = Instant.now();
        Instant pastIssuedAt = now.minusSeconds(120);
        Instant pastExpiry = now.minusSeconds(60);

        String expiredToken = jwtService.generateToken(user, pastIssuedAt, pastExpiry);

        assertFalse(jwtService.validateToken(expiredToken));
    }

    @Test
    void rejectsMalformedOrNullToken() {
        assertFalse(jwtService.validateToken("not-a-jwt"));
        assertFalse(jwtService.validateToken(""));
        assertFalse(jwtService.validateToken("   "));
        assertFalse(jwtService.validateToken(null));
        assertFalse(jwtService.validateToken("header.payload.signature-extra-junk"));
    }

    @Test
    void tokenDoesNotContainPasswordOrPasswordHash() {
        User user = createSampleUser(UserRole.USER);

        String token = jwtService.generateToken(user);
        Claims claims = jwtService.extractAllClaims(token);

        assertNull(claims.get("password"));
        assertNull(claims.get("password_hash"));
        assertNull(claims.get("passwordHash"));
        assertFalse(claims.containsKey("password"));
        assertFalse(claims.containsKey("password_hash"));
        assertFalse(claims.values().stream().anyMatch(v -> "hashed-secret-value".equals(String.valueOf(v))));
    }

    @Test
    void failsFastWhenSecretIsMissingOrTooShort() {
        assertThrows(IllegalStateException.class, () -> new JwtService(null, EXPIRATION_MS));
        assertThrows(IllegalStateException.class, () -> new JwtService("", EXPIRATION_MS));
        assertThrows(IllegalStateException.class, () -> new JwtService("   ", EXPIRATION_MS));
        assertThrows(IllegalStateException.class, () -> new JwtService("short-secret-under-32-bytes", EXPIRATION_MS));
    }
}
