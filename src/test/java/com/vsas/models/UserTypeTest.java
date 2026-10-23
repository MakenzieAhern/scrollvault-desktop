package com.vsas.models;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class UserTypeTest {

    @Test
    void values_contains_expected_entries() {
        EnumSet<UserType> all = EnumSet.allOf(UserType.class);
        assertEquals(3, all.size());
        assertTrue(all.contains(UserType.ADMIN));
        assertTrue(all.contains(UserType.GENERAL));
        assertTrue(all.contains(UserType.GUEST));
    }

    @Test
    void valueOf_accepts_uppercase_and_rejects_lowercase() {
        assertEquals(UserType.ADMIN, UserType.valueOf("ADMIN"));
        assertThrows(IllegalArgumentException.class, () -> UserType.valueOf("admin"));
    }

    @Test
    void ordinals_and_names_match_declaration() {
        assertEquals(0, UserType.ADMIN.ordinal());
        assertEquals(1, UserType.GENERAL.ordinal());
        assertEquals(2, UserType.GUEST.ordinal());

        assertEquals("ADMIN", UserType.ADMIN.name());
        assertEquals("ADMIN", UserType.ADMIN.toString());
    }
}
