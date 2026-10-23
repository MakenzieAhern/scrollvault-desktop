package com.vsas.utils;

import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class PasswordUtils {
    private static final String HASH_ALGO = "SHA-256";
    private PasswordUtils() {}

    public static String hashPassword(String password) {
        if (password == null) throw new IllegalArgumentException("Password is required");
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) throw new IllegalArgumentException("Password must be at most 72 UTF-8 bytes");
        return BCrypt.hashpw(password, BCrypt.gensalt(10));
    }

    public static boolean needsRehash(String stored) {
        return stored != null && !stored.startsWith("$2a$");
    }

    public static boolean verifyPassword(String password, String stored) {
        try {
            if (password == null || stored == null) return false;
            if (stored.startsWith("$2a$")) {
                if (password.getBytes(StandardCharsets.UTF_8).length > 72) return false;
                return BCrypt.checkpw(password, stored);
            }
            // Legacy salted SHA-256 values are upgraded after a successful login.
            String[] parts = stored.split(":", 2);
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expected = Base64.getDecoder().decode(parts[1]);
            byte[] given = digest(password, salt);
            if (expected.length != given.length) return false;
            int r = 0; for (int i = 0; i < expected.length; i++) r |= expected[i] ^ given[i];
            return r == 0;
        } catch (Exception e) { return false; }
    }

    private static byte[] digest(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGO);
            md.update(salt);
            return md.digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
