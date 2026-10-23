package com.vsas.security;

import com.vsas.models.User;

public class SessionManager {
    private static User currentUser;

    public static void login(User user) { currentUser = user; }
    public static void logout() { currentUser = null; }
    public static User getCurrentUser() { return currentUser; }

    public static boolean isLoggedIn() { return currentUser != null; }

    public static User guest() {
        User g = new User();
        try {
            var usernameField = com.vsas.models.User.class.getDeclaredField("username");
            usernameField.setAccessible(true);
            usernameField.set(g, "guest");
        } catch (Exception ignored) {}
        return g;
    }
}
