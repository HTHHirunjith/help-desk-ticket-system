package com.hansana.helpdesk.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporaryPasswordGeneratorTest {

    private final TemporaryPasswordGenerator generator = new TemporaryPasswordGenerator();

    @Test
    void generateDefaultLengthReturns14CharacterSecurePassword() {
        String password = generator.generate();

        assertNotNull(password);
        assertEquals(14, password.length());

        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*()-_=+[]{}".indexOf(ch) >= 0);

        assertTrue(hasUpper, "Password must contain uppercase letters");
        assertTrue(hasLower, "Password must contain lowercase letters");
        assertTrue(hasDigit, "Password must contain digits");
        assertTrue(hasSpecial, "Password must contain special characters");
    }

    @Test
    void successiveGeneratedPasswordsAreUnique() {
        String pass1 = generator.generate();
        String pass2 = generator.generate();

        assertNotEquals(pass1, pass2);
    }
}
