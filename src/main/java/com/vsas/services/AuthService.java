package com.vsas.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.vsas.models.User;
import com.vsas.utils.PasswordUtils;
import com.vsas.storage.JsonFiles;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuthService {
    private final Path usersPath;
    private final Gson gson = new Gson();

    public AuthService(Path usersPath) { this.usersPath = usersPath; }

    private List<User> loadAll() throws IOException {
        if (!Files.exists(usersPath)) return new ArrayList<>();
        try (Reader r = Files.newBufferedReader(usersPath)) {
            List<User> list = gson.fromJson(r, TypeToken.getParameterized(List.class, User.class).getType());
            return list == null ? new ArrayList<>() : list;
        }
    }

    private void saveAll(List<User> users) throws IOException {
        JsonFiles.write(usersPath, gson.toJson(users));
    }

    public Optional<User> login(String username, String password) throws IOException {
        List<User> users = loadAll();
        for (User u : users) {
            if (u.getUsername().equalsIgnoreCase(username)) {
                if (PasswordUtils.verifyPassword(password, u.getPasswordHash())) {
                    // bcrypt accepts at most 72 UTF-8 bytes. Preserve access to legacy accounts
                    // with longer passwords until their owner explicitly chooses a new password.
                    if (PasswordUtils.needsRehash(u.getPasswordHash())
                            && password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72) {
                        u.setPasswordHash(PasswordUtils.hashPassword(password));
                        saveAll(users);
                    }
                    return Optional.of(u);
                }
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public User register(String idKey,
                         String username,
                         String fullName,
                         String email,
                         String phone,
                         String plainPassword,
                         String type) throws IOException {
        List<User> users = loadAll();
        type = (type == null || type.isBlank()) ? "GENERAL" : type.trim().toUpperCase(java.util.Locale.ROOT);
        if (!java.util.Set.of("GENERAL", "ADMIN", "GUEST").contains(type)) throw new IllegalArgumentException("Invalid role");
        // Auto-generate idKey according to rules if none provided
        if (idKey == null || idKey.isBlank()) {
            String t = (type == null) ? "GENERAL" : type.toUpperCase();
            if ("ADMIN".equals(t)) {
                idKey = generateAdminIdKey(users);
            } else if ("GENERAL".equals(t)) {
                idKey = generateGeneralIdKey(users, fullName, username);
            } else {
                // For other types (e.g., GUEST), id key not required; leave as null
                idKey = null;
            }
        }
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Username is required");
        if (plainPassword == null || plainPassword.length() < 6) throw new IllegalArgumentException("Password must be at least 6 characters");

        boolean idExists = idKeyExists(users, idKey);
        if (idExists) throw new IllegalArgumentException("ID key already exists");
        boolean userExists = usernameExists(users, username);
        if (userExists) throw new IllegalArgumentException("Username already exists");

        JsonObject o = new JsonObject();
        o.addProperty("idKey", idKey);
        o.addProperty("username", username);
        o.addProperty("fullName", fullName == null ? "" : fullName);
        o.addProperty("email", email == null ? "" : email);
        o.addProperty("phone", phone == null ? "" : phone);
        o.addProperty("passwordHash", PasswordUtils.hashPassword(plainPassword));
        o.addProperty("type", (type == null || type.isBlank()) ? "GENERAL" : type);

        User created = gson.fromJson(o, User.class);
        users.add(created);
        saveAll(users);
        return created;
    }

    public User updateProfile(com.vsas.models.User current,
                              String newUsername,
                              String newFullName,
                              String newEmail,
                              String newPhone,
                              String newPlainPassword) throws IOException {
        if (current == null) throw new IllegalArgumentException("No current user");

        List<User> users = loadAll();

        // find existing user by idKey if present, otherwise by username
        int idx = -1;
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            boolean matchById = (current.getIdKey() != null && current.getIdKey().equalsIgnoreCase(u.getIdKey()));
            boolean matchByUsername = (current.getIdKey() == null && current.getUsername() != null && current.getUsername().equalsIgnoreCase(u.getUsername()));
            if (matchById || matchByUsername) { idx = i; break; }
        }
        if (idx < 0) throw new IllegalArgumentException("User not found");

        // If username changes, ensure uniqueness
        String targetUsername = (newUsername == null || newUsername.isBlank()) ? current.getUsername() : newUsername;
        if (!targetUsername.equalsIgnoreCase(current.getUsername()) && usernameExists(users, targetUsername)) {
            throw new IllegalArgumentException("Username already exists");
        }

        String fullName = (newFullName == null) ? current.getFullName() : newFullName;
        String email = (newEmail == null) ? current.getEmail() : newEmail;
        String phone = (newPhone == null) ? current.getPhone() : newPhone;

        String passwordHash;
        if (newPlainPassword != null && !newPlainPassword.isBlank()) {
            if (newPlainPassword.length() < 6) throw new IllegalArgumentException("Password must be at least 6 characters");
            passwordHash = PasswordUtils.hashPassword(newPlainPassword);
        } else {
            passwordHash = current.getPasswordHash();
        }

        JsonObject o = new JsonObject();
        o.addProperty("idKey", current.getIdKey());
        o.addProperty("username", targetUsername);
        o.addProperty("fullName", fullName == null ? "" : fullName);
        o.addProperty("email", email == null ? "" : email);
        o.addProperty("phone", phone == null ? "" : phone);
        o.addProperty("passwordHash", passwordHash);
        o.addProperty("type", current.getType());

        User updated = gson.fromJson(o, User.class);
        users.set(idx, updated);
        saveAll(users);
        return updated;
    }

    private boolean idKeyExists(List<User> users, String key) {
        if (key == null) return false;
        for (User u : users) {
            if (u.getIdKey() != null && u.getIdKey().equalsIgnoreCase(key)) return true;
        }
        return false;
    }

    private boolean usernameExists(List<User> users, String name) {
        if (name == null) return false;
        for (User u : users) {
            if (u.getUsername() != null && u.getUsername().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private String generateAdminIdKey(List<User> users, java.util.Random rnd) {
        String candidate;
        do {
            int n = 1000 + rnd.nextInt(9000);
            candidate = "admin" + n;
        } while (idKeyExists(users, candidate));
        return candidate;
    }

    private String generateAdminIdKey(List<User> users) {
        return generateAdminIdKey(users, new java.util.Random());
    }

    private String generateGeneralIdKey(List<User> users, String fullName, String username) {
        String prefix = buildNamePrefix(fullName, username);
        java.util.Random rnd = new java.util.Random();
        String candidate;
        do {
            int n = 1000 + rnd.nextInt(9000);
            candidate = prefix + n;
        } while (idKeyExists(users, candidate));
        return candidate;
    }

    private String buildNamePrefix(String fullName, String username) {
        String first = null, last = null;
        if (fullName != null && !fullName.isBlank()) {
            String[] parts = fullName.trim().split("\\s+");
            if (parts.length >= 1) first = parts[0];
            if (parts.length >= 2) last = parts[parts.length - 1];
        }
        String a = (first == null) ? null : first.toLowerCase();
        String b = (last == null) ? null : last.toLowerCase();
        String ab;
        if (a != null && b != null) {
            ab = take(a, 2) + take(b, 2);
        } else if (a != null) {
            ab = take(a, 4);
        } else if (b != null) {
            ab = take(b, 4);
        } else {
            String un = username == null ? "user" : username.toLowerCase();
            ab = take(un, 4);
        }
        // Ensure exactly 4 letters; pad with 'x' if shorter
        if (ab.length() < 4) {
            StringBuilder sb = new StringBuilder(ab);
            while (sb.length() < 4) sb.append('x');
            ab = sb.toString();
        } else if (ab.length() > 4) {
            ab = ab.substring(0, 4);
        }
        return ab;
    }

    private String take(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n);
    }

    public java.util.Collection<User> getAllUsers() throws IOException {
        return loadAll();
    }

    public void updateUserRole(String username, String newRole) throws IOException {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (newRole == null || newRole.isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }

        List<User> users = loadAll();
        int idx = -1;
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUsername().equalsIgnoreCase(username)) {
                idx = i;
                break;
            }
        }

        if (idx < 0) {
            throw new IllegalArgumentException("User not found: " + username);
        }

        User user = users.get(idx);
        String normalizedRole = newRole.toUpperCase();

        // Validate role
        if (!normalizedRole.equals("ADMIN") && !normalizedRole.equals("GENERAL") && !normalizedRole.equals("GUEST")) {
            throw new IllegalArgumentException("Invalid role. Must be ADMIN, GENERAL, or GUEST");
        }

        // Identity is stable: scroll ownership refers to this key, independent of role.
        String newIdKey = user.getIdKey();

        JsonObject o = new JsonObject();
        o.addProperty("idKey", newIdKey);
        o.addProperty("username", user.getUsername());
        o.addProperty("fullName", user.getFullName());
        o.addProperty("email", user.getEmail());
        o.addProperty("phone", user.getPhone());
        o.addProperty("passwordHash", user.getPasswordHash());
        o.addProperty("type", normalizedRole);

        User updated = gson.fromJson(o, User.class);
        users.set(idx, updated);
        saveAll(users);

        // Update session if the current user's role was changed
        if (com.vsas.security.SessionManager.getCurrentUser() != null &&
            com.vsas.security.SessionManager.getCurrentUser().getUsername().equalsIgnoreCase(username)) {
            com.vsas.security.SessionManager.login(updated);
        }
    }

    /**
     * Deletes a user from the system.
     * @param username The username of the user to delete
     * @param currentAdminUsername The username of the admin performing the deletion (to prevent self-deletion)
     * @throws IOException if there's an error reading/writing the users file
     * @throws IllegalArgumentException if user not found or admin tries to delete themselves
     */
    public void deleteUser(String username, String currentAdminUsername) throws IOException {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        if (username.equalsIgnoreCase(currentAdminUsername)) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }

        List<User> users = loadAll();
        int idx = -1;
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUsername().equalsIgnoreCase(username)) {
                idx = i;
                break;
            }
        }

        if (idx < 0) {
            throw new IllegalArgumentException("User not found: " + username);
        }

        users.remove(idx);
        saveAll(users);
    }

    /**
     * Creates a new user with admin privileges (admin-initiated user creation).
     * @param username The username for the new user
     * @param fullName The full name of the new user
     * @param email The email address
     * @param phone The phone number
     * @param plainPassword The plain text password (will be hashed)
     * @param type The user type (ADMIN or GENERAL)
     * @return The newly created User object
     * @throws IOException if there's an error reading/writing the users file
     * @throws IllegalArgumentException if username already exists or validation fails
     */
    public User createUserByAdmin(String username, String fullName, String email, String phone,
                                  String plainPassword, String type) throws IOException {
        // Validate inputs
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name cannot be empty");
        }
        if (plainPassword == null || plainPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        List<User> users = loadAll();

        // Check if username already exists
        if (usernameExists(users, username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Normalize type
        String normalizedType = (type == null || type.isBlank()) ? "GENERAL" : type.toUpperCase();
        if (!normalizedType.equals("ADMIN") && !normalizedType.equals("GENERAL")) {
            normalizedType = "GENERAL";
        }

        // Generate ID key based on type
        String idKey;
        if ("ADMIN".equals(normalizedType)) {
            idKey = generateAdminIdKey(users);
        } else {
            idKey = generateGeneralIdKey(users, fullName, username);
        }

        JsonObject o = new JsonObject();
        o.addProperty("idKey", idKey);
        o.addProperty("username", username);
        o.addProperty("fullName", fullName);
        o.addProperty("email", email == null ? "" : email);
        o.addProperty("phone", phone == null ? "" : phone);
        o.addProperty("passwordHash", PasswordUtils.hashPassword(plainPassword));
        o.addProperty("type", normalizedType);

        User created = gson.fromJson(o, User.class);
        users.add(created);
        saveAll(users);
        return created;
    }
}
