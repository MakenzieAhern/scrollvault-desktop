package com.vsas.services;

import com.vsas.models.Scroll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.UncheckedIOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class ScrollServiceTest {
    @TempDir Path directory;
    private Path json;
    private Path files;
    private ScrollService service;

    @BeforeEach void setup() throws Exception {
        json = directory.resolve("scrolls.json");
        files = directory.resolve("files");
        Files.writeString(json, "[]");
        service = new ScrollService(json, files);
    }

    private Scroll upload(String id, String owner, String content) throws Exception {
        Path source = directory.resolve(id).resolve("same.txt");
        Files.createDirectories(source.getParent());
        Files.writeString(source, content);
        service.add(new Scroll(id, owner, "Example", "TEXT", source.toString()), source.toFile());
        return service.list().stream().filter(s -> id.equals(s.getID())).findFirst().orElseThrow();
    }

    @Test void emptyCatalogueStaysEmpty() { assertTrue(service.list().isEmpty()); }
    @Test void missingCatalogueStartsEmpty() throws Exception {
        Files.delete(json);
        assertTrue(service.list().isEmpty());
    }
    @Test void malformedCatalogueDoesNotGetOverwritten() throws Exception {
        Files.writeString(json, "{bad json}");
        assertThrows(UncheckedIOException.class, service::list);
        assertThrows(UncheckedIOException.class, () -> upload("S-001", "user", "hello"));
        assertEquals("{bad json}", Files.readString(json));
    }
    @Test void nullCatalogueIsRejected() throws Exception {
        Files.writeString(json, "null");
        assertThrows(UncheckedIOException.class, service::list);
    }
    @Test void uploadPersistsContentAndOwnership() throws Exception {
        Scroll uploaded = upload("S-001", "alice", "hello 世界");
        ScrollService reopened = new ScrollService(json, files);
        assertEquals(1, reopened.listUserScrolls("alice").size());
        assertEquals("hello 世界", reopened.readText(reopened.list().get(0)));
        assertTrue(Files.exists(reopened.getScrollFilePath(uploaded)));
    }
    @Test void sameOriginalFileNameNeverOverwritesAnotherUpload() throws Exception {
        Scroll first = upload("S-001", "alice", "first");
        Scroll second = upload("S-002", "bob", "second");
        assertNotEquals(first.getResourcePath(), second.getResourcePath());
        assertEquals("first", service.readText(first));
        assertEquals("second", service.readText(second));
        service.delete(second);
        assertEquals("first", service.readText(first));
    }
    @Test void duplicateIdsAreRejectedBeforeCopying() throws Exception {
        upload("S-001", "alice", "first");
        assertThrows(IllegalArgumentException.class, () -> upload("S-001", "bob", "second"));
        assertEquals("first", service.readText(service.list().get(0)));
        try (var stream = Files.list(files)) { assertEquals(1, stream.count()); }
    }
    @Test void deletingLastRecordRemovesFileAndDoesNotResurrectDemos() throws Exception {
        Scroll scroll = upload("S-001", "alice", "hello");
        Path path = service.getScrollFilePath(scroll);
        service.delete(scroll);
        assertTrue(service.list().isEmpty());
        assertFalse(Files.exists(path));
        assertEquals("[]", Files.readString(json));
    }
    @Test void deleteUsesPersistedPathInsteadOfCallerPath() throws Exception {
        Scroll first = upload("S-001", "alice", "first");
        Scroll second = upload("S-002", "bob", "second");
        first.setResourcePath(second.getResourcePath());
        service.delete(first);
        assertEquals("second", service.readText(second));
    }
    @Test void countersPersistAndTitleEditCannotResetThem() throws Exception {
        Scroll stale = upload("S-001", "alice", "hello");
        service.incrementViewCount(stale.getID());
        service.incrementViewCount(stale.getID());
        service.incrementDownloadCount(stale.getID());
        stale.setTitle("Updated");
        service.update(stale);
        Scroll saved = service.list().get(0);
        assertEquals("Updated", saved.getTitle());
        assertEquals(2, saved.getViewCount());
        assertEquals(1, saved.getDownloadCount());
    }
    @Test void missingIdsAreNoOps() throws Exception {
        service.incrementDownloadCount("absent");
        service.incrementViewCount("absent");
        service.update(new Scroll("absent", "x", "TEXT", null));
        service.delete(new Scroll("absent", "x", "TEXT", null));
        assertTrue(service.list().isEmpty());
    }
    @Test void ownershipFilterIsExactAndNullSafe() throws Exception {
        upload("S-001", "alice", "a"); upload("S-002", "bob", "b");
        assertEquals(1, service.listUserScrolls("alice").size());
        assertTrue(service.listUserScrolls("absent").isEmpty());
        assertTrue(service.listUserScrolls(null).isEmpty());
    }
    @Test void idsIncrementPastLargestNumericValue() throws Exception {
        upload("S-009", "alice", "a"); upload("S-custom", "bob", "b");
        assertEquals("S-010", service.generateNextScrollId());
    }
    @Test void emptyCatalogueStartsWithFirstId() { assertEquals("S-001", service.generateNextScrollId()); }
    @Test void traversalAbsoluteAndNullPathsCannotReadLocalFiles() throws Exception {
        Path secret = directory.resolve("secret.txt"); Files.writeString(secret, "secret");
        for (String path : new String[]{"/scrolls/../secret.txt", secret.toString(), "/scrolls/..\\secret.txt", null}) {
            Scroll scroll = new Scroll("S-1", "x", "TEXT", path);
            assertNull(service.open(scroll)); assertNull(service.getScrollFilePath(scroll));
        }
        assertNull(service.open(null)); assertNull(service.getScrollFilePath(null));
    }
    @Test void symlinksCannotExposeFilesOutsideStorage() throws Exception {
        Files.createDirectories(files);
        Path secret = directory.resolve("secret.txt"); Files.writeString(secret, "secret");
        Files.createSymbolicLink(files.resolve("link.txt"), secret);
        assertNull(service.open(new Scroll("S-1", "x", "TEXT", "/scrolls/link.txt")));
    }
    @Test void imageAndTextMissingResourcesHaveDefinedResults() {
        Scroll missing = new Scroll("S-1", "x", "TEXT", "/scrolls/missing.txt");
        assertTrue(service.readText(missing).startsWith("Resource not found:"));
        assertNull(service.readImage(missing));
    }
    @Test void legacyTimestampProvidesDate() throws Exception {
        Files.writeString(json, "[{\"id\":\"S-001\",\"uploadTimestamp\":\"Jan 1, 2025, 12:00:00 PM\"}]");
        assertEquals(java.time.LocalDate.of(2025, 1, 1), service.list().get(0).getUploadDate());
    }
}
