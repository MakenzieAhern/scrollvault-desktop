package com.vsas.services;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.vsas.models.Scroll;
import com.vsas.storage.JsonFiles;
import javafx.scene.image.Image;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/** File-backed scroll catalogue for a single local application instance. */
public class ScrollService {
    private final Path scrollsJsonPath;
    private final Path scrollsStoragePath;

    public ScrollService() {
        this(Path.of(System.getProperty("scrollvault.dataDir", "data"), "scrolls.json"),
             Path.of(System.getProperty("scrollvault.dataDir", "data"), "files"));
    }

    public ScrollService(Path jsonPath, Path storagePath) {
        this.scrollsJsonPath = jsonPath;
        this.scrollsStoragePath = storagePath.toAbsolutePath().normalize();
    }

    private static final class LocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        @Override public JsonElement serialize(LocalDate src, java.lang.reflect.Type type, JsonSerializationContext context) {
            return src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.toString());
        }
        @Override public LocalDate deserialize(JsonElement json, java.lang.reflect.Type type, JsonDeserializationContext context) {
            return (json == null || json.isJsonNull()) ? null : LocalDate.parse(json.getAsString());
        }
    }

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter()).create();

    public List<Scroll> list() {
        if (!Files.exists(scrollsJsonPath)) return new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(scrollsJsonPath)) {
            List<Scroll> result = gson.fromJson(reader, TypeToken.getParameterized(List.class, Scroll.class).getType());
            if (result == null || result.stream().anyMatch(Objects::isNull)) {
                throw new IOException("Expected an array of scroll records in " + scrollsJsonPath);
            }
            return result;
        } catch (IOException | RuntimeException ex) {
            // Never substitute demo records for corrupt data: the next save would destroy the catalogue.
            throw new UncheckedIOException("Unable to read scroll catalogue: " + scrollsJsonPath,
                    ex instanceof IOException io ? io : new IOException(ex));
        }
    }

    private void save(List<Scroll> scrolls) throws IOException {
        JsonFiles.write(scrollsJsonPath, gson.toJson(scrolls));
    }

    public List<Scroll> listUserScrolls(String uploaderID) {
        if (uploaderID == null) return new ArrayList<>();
        return list().stream().filter(scroll -> uploaderID.equals(scroll.getUploaderID())).toList();
    }

    public void add(Scroll scroll, File sourceFile) throws IOException {
        Objects.requireNonNull(scroll, "Scroll is required");
        Objects.requireNonNull(sourceFile, "Source file is required");
        if (scroll.getID() == null || scroll.getID().isBlank()) throw new IllegalArgumentException("Scroll ID is required");
        List<Scroll> scrolls = new ArrayList<>(list());
        if (scrolls.stream().anyMatch(s -> Objects.equals(s.getID(), scroll.getID()))) {
            throw new IllegalArgumentException("Scroll ID already exists: " + scroll.getID());
        }
        Files.createDirectories(scrollsStoragePath);
        // Two users can upload a file with the same original name without sharing or overwriting bytes.
        String safeName = sourceFile.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
        String storageFileName = UUID.randomUUID() + "-" + safeName;
        Path storagePath = scrollsStoragePath.resolve(storageFileName);
        Files.copy(sourceFile.toPath(), storagePath);
        Scroll toSave = gson.fromJson(gson.toJson(scroll), Scroll.class);
        toSave.setResourcePath("/scrolls/" + storageFileName);
        scrolls.add(toSave);
        try {
            save(scrolls);
        } catch (IOException ex) {
            Files.deleteIfExists(storagePath);
            throw ex;
        }
    }

    public String generateNextScrollId() {
        int maxId = 0;
        for (Scroll scroll : list()) {
            String id = scroll.getID();
            if (id != null && id.startsWith("S-")) {
                try { maxId = Math.max(maxId, Integer.parseInt(id.substring(2))); }
                catch (NumberFormatException ignored) { }
            }
        }
        return String.format("S-%03d", maxId + 1);
    }

    private Path storedPath(Scroll scroll) {
        if (scroll == null || scroll.getResourcePath() == null || !scroll.getResourcePath().startsWith("/scrolls/")) return null;
        String fileName = scroll.getResourcePath().substring("/scrolls/".length());
        // Persisted paths are identifiers, never arbitrary paths into the user's filesystem.
        if (fileName.isBlank() || fileName.contains("/") || fileName.contains("\\") || fileName.equals(".") || fileName.equals("..")) return null;
        Path candidate = scrollsStoragePath.resolve(fileName).normalize();
        if (!candidate.startsWith(scrollsStoragePath) || Files.isSymbolicLink(candidate)) return null;
        return candidate;
    }

    private boolean isBundledResource(Scroll scroll) {
        String path = scroll == null ? null : scroll.getResourcePath();
        return path != null && !path.contains("..") && !path.contains("\\")
                && (path.startsWith("/scrolls/") || path.equals("/icons/hero-bg.png"));
    }

    public InputStream open(Scroll scroll) {
        Path path = storedPath(scroll);
        try {
            if (path != null && Files.isRegularFile(path)) return Files.newInputStream(path);
        } catch (IOException ignored) { }
        return isBundledResource(scroll) ? getClass().getResourceAsStream(scroll.getResourcePath()) : null;
    }

    public String readText(Scroll scroll) {
        try (InputStream in = open(scroll)) {
            if (in == null) return "Resource not found: " + (scroll == null ? "null" : scroll.getResourcePath());
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "Failed to read: " + ex.getMessage();
        }
    }

    public Image readImage(Scroll scroll) {
        try (InputStream in = open(scroll)) {
            return in == null ? null : new Image(in);
        } catch (IOException ex) {
            return null;
        }
    }

    public void delete(Scroll scroll) throws IOException {
        Objects.requireNonNull(scroll, "Scroll is required");
        List<Scroll> scrolls = new ArrayList<>(list());
        Scroll stored = scrolls.stream().filter(s -> Objects.equals(s.getID(), scroll.getID())).findFirst().orElse(null);
        if (stored == null) return;
        scrolls.removeIf(s -> Objects.equals(s.getID(), scroll.getID()));
        save(scrolls);
        // Resolve the stored record, not a caller-supplied path, and retain any legacy shared file.
        if (scrolls.stream().noneMatch(s -> Objects.equals(s.getResourcePath(), stored.getResourcePath()))) {
            Path path = storedPath(stored);
            if (path != null) Files.deleteIfExists(path);
        }
    }

    public void update(Scroll scroll) throws IOException {
        List<Scroll> scrolls = new ArrayList<>(list());
        for (int i = 0; i < scrolls.size(); i++) {
            if (Objects.equals(scrolls.get(i).getID(), scroll.getID())) {
                // Preview/download events can make the object held by a table stale.
                scroll.setDownloadCount(Math.max(scroll.getDownloadCount(), scrolls.get(i).getDownloadCount()));
                scroll.setViewCount(Math.max(scroll.getViewCount(), scrolls.get(i).getViewCount()));
                scrolls.set(i, scroll);
                save(scrolls);
                return;
            }
        }
    }

    public void incrementDownloadCount(String scrollId) throws IOException { incrementCount(scrollId, true); }
    public void incrementViewCount(String scrollId) throws IOException { incrementCount(scrollId, false); }

    private void incrementCount(String scrollId, boolean download) throws IOException {
        List<Scroll> scrolls = new ArrayList<>(list());
        for (Scroll scroll : scrolls) {
            if (Objects.equals(scroll.getID(), scrollId)) {
                if (download) scroll.incrementDownloadCount(); else scroll.incrementViewCount();
                save(scrolls);
                return;
            }
        }
    }

    /** Return only writable uploaded files; bundled resources must never be edited in place. */
    public Path getScrollFilePath(Scroll scroll) {
        Path path = storedPath(scroll);
        return path != null && Files.isRegularFile(path) ? path : null;
    }

}
