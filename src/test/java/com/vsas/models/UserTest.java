package com.vsas.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void defaults_are_null() {
        User u = new User();
        assertNull(u.getIdKey());
        assertNull(u.getUsername());
        assertNull(u.getFullName());
        assertNull(u.getEmail());
        assertNull(u.getPhone());
        assertNull(u.getPasswordHash());
        assertNull(u.getType());
    }

    @Test
    void partial_fields_set_leave_others_null() throws Exception {
        User u = new User();
        set(u, "username", "onlyuser");
        assertEquals("onlyuser", u.getUsername());
        assertNull(u.getEmail());
        assertNull(u.getPhone());
    }

    @Test
    void getters_return_reflection_set_values() throws Exception {
        User u = new User();
        set(u, "idKey", "abcd1234");
        set(u, "username", "addy");
        set(u, "fullName", "Addy Delta");
        set(u, "email", "a@b.c");
        set(u, "phone", "0123456789");
        set(u, "passwordHash", "salt:hash");
        set(u, "type", "GENERAL");

        assertEquals("abcd1234", u.getIdKey());
        assertEquals("addy", u.getUsername());
        assertEquals("Addy Delta", u.getFullName());
        assertEquals("a@b.c", u.getEmail());
        assertEquals("0123456789", u.getPhone());
        assertEquals("salt:hash", u.getPasswordHash());
        assertEquals("GENERAL", u.getType());
    }

    private static void set(Object target, String field, Object value) throws Exception {
        var f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}
