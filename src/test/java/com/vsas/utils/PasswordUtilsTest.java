package com.vsas.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilsTest {

    @Test
    void hashAndVerify_roundTrip_succeeds() {
        String hash = PasswordUtils.hashPassword("secret123");
        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$10$"));
        assertTrue(PasswordUtils.verifyPassword("secret123", hash));
    }

    @Test
    void verify_incorrectPassword_fails() {
        String hash = PasswordUtils.hashPassword("correct");
        assertFalse(PasswordUtils.verifyPassword("wrong", hash));
    }
    @Test void distinctSaltsAndLongPasswordsAreHandled() {
        assertNotEquals(PasswordUtils.hashPassword("password"), PasswordUtils.hashPassword("password"));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtils.hashPassword("x".repeat(73)));
        assertFalse(PasswordUtils.verifyPassword(null, "bad"));
        assertFalse(PasswordUtils.verifyPassword("secret", "bad"));
    }
}
