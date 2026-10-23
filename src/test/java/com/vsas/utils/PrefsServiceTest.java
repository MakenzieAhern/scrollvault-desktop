package com.vsas.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

class PrefsServiceTest {

    @BeforeEach
    void resetNode() throws BackingStoreException {
        Preferences node = Preferences.userRoot().node(System.getProperty("scrollvault.preferencesNode", "com.vsas.app"));
        node.clear();
        node.flush();
    }

    @Test
    void defaults_are_returned_when_not_set() {
        assertFalse(PrefsService.isRememberMeEnabled());
        assertNull(PrefsService.getRememberedUsername());
        assertEquals("dark", PrefsService.getTheme());
        assertEquals("en", PrefsService.getLanguage());
    }

    @Test
    void remember_me_can_be_set_and_cleared() {
        PrefsService.setRememberedUser("john");
        assertTrue(PrefsService.isRememberMeEnabled());
        assertEquals("john", PrefsService.getRememberedUsername());

        PrefsService.clearRememberedUser();
        assertFalse(PrefsService.isRememberMeEnabled());
        assertNull(PrefsService.getRememberedUsername());
    }

    @Test
    void theme_and_language_round_trip() {
        PrefsService.setTheme("light");
        assertEquals("light", PrefsService.getTheme());

        PrefsService.setLanguage("es");
        assertEquals("es", PrefsService.getLanguage());
    }
}
