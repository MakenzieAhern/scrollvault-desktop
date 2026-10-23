package com.vsas.services;

import com.vsas.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    @TempDir
    Path tempDir;

    private Path usersFile() throws IOException {
        Path p = tempDir.resolve("users.json");
        if (!Files.exists(p)) Files.writeString(p, "[]");
        return p;
    }

    @Test
    void register_and_login_success() throws IOException {
        AuthService auth = new AuthService(usersFile());
        User u = auth.register(null, "testuser", "Test User", "", "", "pass123", "GENERAL");
        assertNotNull(u);
        assertEquals("testuser", u.getUsername());
        assertNotNull(u.getPasswordHash());

        Optional<User> logged = auth.login("testuser", "pass123");
        assertTrue(logged.isPresent());
    }

    @Test
    void register_duplicate_username_throws() throws IOException {
        AuthService auth = new AuthService(usersFile());
        auth.register(null, "dupe", "A B", "", "", "pass123", "GENERAL");
        assertThrows(IllegalArgumentException.class, () ->
                auth.register(null, "dupe", "C D", "", "", "pass123", "GENERAL")
        );
    }

    @Test
    void register_admin_generates_admin_idkey_and_unique() throws Exception {
        AuthService auth = new AuthService(usersFile());
        User a1 = auth.register(null, "admin1", "A B", "", "", "pass123", "ADMIN");
        User a2 = auth.register(null, "admin2", "C D", "", "", "pass123", "ADMIN");
        assertNotNull(a1.getIdKey());
        assertTrue(a1.getIdKey().matches("admin\\d{4}"));
        assertNotEquals(a1.getIdKey(), a2.getIdKey());
    }

    @Test
    void register_general_prefix_from_fullname_and_fallbacks() throws Exception {
        AuthService auth = new AuthService(usersFile());

        User g1 = auth.register(null, "jsmith", "John Smith", "", "", "pass123", "GENERAL");
        assertTrue(g1.getIdKey().startsWith("josm"));

        User g2 = auth.register(null, "ada", "Ada", "", "", "pass123", "GENERAL");
        assertTrue(g2.getIdKey().startsWith("adax"));

        User g3 = auth.register(null, "bee", null, "", "", "pass123", "GENERAL");
        assertTrue(g3.getIdKey().startsWith("beex"));
    }

    @Test
    void register_duplicate_idkey_throws() throws Exception {
        AuthService auth = new AuthService(usersFile());
        auth.register("abcd1234", "u1", "A B", "", "", "pass123", "GENERAL");
        assertThrows(IllegalArgumentException.class, () ->
                auth.register("abcd1234", "u2", "C D", "", "", "pass123", "GENERAL")
        );
    }

    @Test
    void updateProfile_changes_username_and_password_hash() throws Exception {
        AuthService auth = new AuthService(usersFile());
        User u = auth.register(null, "olduser", "Old Name", "e@x", "0123456789", "pass123", "GENERAL");
        String oldHash = u.getPasswordHash();

        User updated = auth.updateProfile(u, "newuser", "New Name", "n@x", "0987654321", "newpass1");
        assertEquals("newuser", updated.getUsername());
        assertEquals("New Name", updated.getFullName());
        assertEquals("n@x", updated.getEmail());
        assertEquals("0987654321", updated.getPhone());
        assertNotEquals(oldHash, updated.getPasswordHash());
    }

    @Test
    void updateProfile_duplicate_username_throws() throws Exception {
        AuthService auth = new AuthService(usersFile());
        User a = auth.register(null, "userA", "A B", "", "0123456789", "pass123", "GENERAL");
        User b = auth.register(null, "userB", "B C", "", "0123456789", "pass123", "GENERAL");

        assertThrows(IllegalArgumentException.class, () ->
                auth.updateProfile(a, "userB", null, null, null, null)
        );
    }

    @Test
    void updateProfile_without_password_keeps_hash() throws Exception {
        AuthService auth = new AuthService(usersFile());
        User u = auth.register(null, "keep", "K E", "", "0123456789", "pass123", "GENERAL");
        String oldHash = u.getPasswordHash();

        User updated = auth.updateProfile(u, "keep", null, null, null, null);
        assertEquals(oldHash, updated.getPasswordHash());
    }

    @Test
    void login_wrong_password_returns_empty() throws Exception {
        AuthService auth = new AuthService(usersFile());
        auth.register(null, "name1", "N A", "", "0123456789", "secret1", "GENERAL");
        assertTrue(auth.login("name1", "wrong").isEmpty());
        assertTrue(auth.login("nope", "secret1").isEmpty());
    }

    @Test void roleChangesKeepOwnershipIdentity() throws Exception {
        AuthService auth = new AuthService(usersFile());
        User original = auth.register(null, "owner", "Owner", "", "", "pass123", "GENERAL");
        auth.updateUserRole("owner", "ADMIN");
        assertEquals(original.getIdKey(), auth.login("owner", "pass123").orElseThrow().getIdKey());
        auth.updateUserRole("owner", "GENERAL");
        assertEquals(original.getIdKey(), auth.login("owner", "pass123").orElseThrow().getIdKey());
    }

    @Test void legacyPasswordIsUpgradedOnlyOnSuccessfulLogin() throws Exception {
        AuthService auth = new AuthService(usersFile());
        byte[] salt = "legacy-salt".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var digest = java.security.MessageDigest.getInstance("SHA-256"); digest.update(salt);
        String legacy = java.util.Base64.getEncoder().encodeToString(salt) + ":" +
                java.util.Base64.getEncoder().encodeToString(digest.digest("pass123".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        Files.writeString(usersFile(), "[{\"username\":\"legacy\",\"passwordHash\":\"" + legacy + "\",\"type\":\"GENERAL\"}]");
        assertTrue(auth.login("legacy", "wrong").isEmpty());
        assertTrue(Files.readString(usersFile()).contains(legacy));
        User logged = auth.login("legacy", "pass123").orElseThrow();
        assertTrue(logged.getPasswordHash().startsWith("$2a$"));
        assertTrue(auth.login("legacy", "pass123").isPresent());
    }

    @Test void profileRejectsShortPasswordAndRegistrationRejectsUnknownRole() throws Exception {
        AuthService auth = new AuthService(usersFile());
        User user = auth.register(null, "person", "Person", "", "", "pass123", "general");
        assertEquals("GENERAL", user.getType());
        assertThrows(IllegalArgumentException.class, () -> auth.updateProfile(user, null, null, null, null, "short"));
        assertThrows(IllegalArgumentException.class, () -> auth.register(null, "bad", "Bad", "", "", "pass123", "SUPER"));
    }

    @Test void longLegacyPasswordRemainsValidWithoutUnsafeRehash() throws Exception {
        AuthService auth = new AuthService(usersFile());
        String password = "密".repeat(25); // 75 UTF-8 bytes, beyond bcrypt's safe limit.
        byte[] salt = "legacy-salt".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var digest = java.security.MessageDigest.getInstance("SHA-256"); digest.update(salt);
        String legacy = java.util.Base64.getEncoder().encodeToString(salt) + ":" +
                java.util.Base64.getEncoder().encodeToString(digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        String json = "[{\"username\":\"legacy\",\"passwordHash\":\"" + legacy + "\",\"type\":\"GENERAL\"}]";
        Files.writeString(usersFile(), json);
        assertEquals(legacy, auth.login("legacy", password).orElseThrow().getPasswordHash());
        assertEquals(json, Files.readString(usersFile()));
        assertTrue(auth.login("legacy", password + "x").isEmpty());
    }
}
