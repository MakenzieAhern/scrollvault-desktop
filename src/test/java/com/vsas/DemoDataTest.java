package com.vsas;

import com.vsas.services.AuthService;
import com.vsas.services.ScrollService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DemoDataTest {
    @TempDir Path temporaryDirectory;

    @Test
    void createsSyntheticAccountsAndReadableDocumentsInNewDirectory() throws IOException {
        Path data = temporaryDirectory.resolve("demo");
        DemoData.seed(data);

        AuthService auth = new AuthService(data.resolve("users.json"));
        assertEquals(2, auth.getAllUsers().size());
        var admin = auth.login("admin", "admin123").orElseThrow();
        var reader = auth.login("reader", "reader123").orElseThrow();
        assertEquals("ADMIN", admin.getType());
        assertEquals("GENERAL", reader.getType());
        assertEquals("admin@example.com", admin.getEmail());
        assertEquals("reader@example.com", reader.getEmail());
        assertTrue(auth.login("reader", "incorrect").isEmpty());
        assertFalse(Files.readString(data.resolve("users.json")).contains("reader123"));

        ScrollService scrolls = new ScrollService(data.resolve("scrolls.json"), data.resolve("files"));
        assertEquals(3, scrolls.list().size());
        assertEquals(2, scrolls.listUserScrolls(reader.getIdKey()).size());
        for (var scroll : scrolls.list()) {
            assertEquals("TEXT", scroll.getType());
            assertTrue(scrolls.readText(scroll).startsWith(scroll.getTitle()));
            assertEquals(0, scroll.getDownloadCount());
            assertEquals(0, scroll.getViewCount());
        }
    }

    @Test
    void acceptsAnExistingEmptyDirectory() throws IOException {
        Path data = Files.createDirectory(temporaryDirectory.resolve("empty"));
        DemoData.seed(data);
        assertTrue(Files.isRegularFile(data.resolve("users.json")));
        assertTrue(Files.isRegularFile(data.resolve("scrolls.json")));
    }

    @Test
    void refusesExistingDataWithoutChangingIt() throws IOException {
        Path data = Files.createDirectory(temporaryDirectory.resolve("existing"));
        Path existing = data.resolve("users.json");
        String sentinel = "existing local records must survive";
        Files.writeString(existing, sentinel);

        IOException error = assertThrows(IOException.class, () -> DemoData.seed(data));
        assertTrue(error.getMessage().contains("Refusing to overwrite"));
        assertEquals(sentinel, Files.readString(existing));
        assertFalse(Files.exists(data.resolve("scrolls.json")));
        try (var entries = Files.list(data)) {
            assertEquals(1, entries.count());
        }
    }

    @Test
    void refusesASecondSeedWithoutChangingAccountOrCatalogueBytes() throws IOException {
        Path data = temporaryDirectory.resolve("demo");
        DemoData.seed(data);
        byte[] users = Files.readAllBytes(data.resolve("users.json"));
        byte[] catalogue = Files.readAllBytes(data.resolve("scrolls.json"));

        assertThrows(IOException.class, () -> DemoData.seed(data));
        assertArrayEquals(users, Files.readAllBytes(data.resolve("users.json")));
        assertArrayEquals(catalogue, Files.readAllBytes(data.resolve("scrolls.json")));
    }

    @Test
    void refusesAFileAsTheDestination() throws IOException {
        Path file = temporaryDirectory.resolve("existing-file");
        Files.writeString(file, "keep me");

        assertThrows(IOException.class, () -> DemoData.seed(file));
        assertEquals("keep me", Files.readString(file));
    }
}
