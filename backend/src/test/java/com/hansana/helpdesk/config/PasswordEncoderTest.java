package com.hansana.helpdesk.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordEncoderTest {

    @Test
    void encodedPasswordDoesNotEqualPlaintextAndMatchesOriginal() {
        PasswordEncoder encoder = new SecurityConfig().passwordEncoder();
        String plaintext = "correct-horse-battery-staple";

        String encoded = encoder.encode(plaintext);

        assertNotEquals(plaintext, encoded);
        assertTrue(encoder.matches(plaintext, encoded));
        assertFalse(encoder.matches("wrong-password", encoded));
    }
}
