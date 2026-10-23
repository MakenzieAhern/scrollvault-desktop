package com.vsas.utils;

import java.util.prefs.Preferences;

public class PrefsService {
    private static final String NODE = "com.vsas.app";
    private static final String KEY_REMEMBER = "rememberMe";
    private static final String KEY_USERNAME = "rememberUsername";
    private static final String KEY_THEME = "theme"; // light|dark
    private static final String KEY_LANGUAGE = "language"; // e.g., en|es

    private static Preferences prefs() {
        return Preferences.userRoot().node(System.getProperty("scrollvault.preferencesNode", NODE));
    }

    public static void setRememberedUser(String username) {
        prefs().putBoolean(KEY_REMEMBER, true);
        prefs().put(KEY_USERNAME, username == null ? "" : username);
    }

    public static void clearRememberedUser() {
        prefs().putBoolean(KEY_REMEMBER, false);
        prefs().remove(KEY_USERNAME);
    }

    public static boolean isRememberMeEnabled() { return prefs().getBoolean(KEY_REMEMBER, false); }
    public static String getRememberedUsername() { return prefs().get(KEY_USERNAME, null); }

    public static void setTheme(String theme) { prefs().put(KEY_THEME, theme); }
    public static String getTheme() { return prefs().get(KEY_THEME, "dark"); }

    public static void setLanguage(String lang) { prefs().put(KEY_LANGUAGE, lang); }
    public static String getLanguage() { return prefs().get(KEY_LANGUAGE, "en"); }
}
