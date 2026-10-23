package com.vsas.storage;

import java.io.IOException;
import java.nio.file.*;

/** Replace complete JSON documents without truncating the last good file on a failed write. */
public final class JsonFiles {
    private JsonFiles() { }

    public static void write(Path target, String json) throws IOException {
        Path absolute = target.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        Path temporary = Files.createTempFile(absolute.getParent(), ".scrollvault-", ".json.tmp");
        try {
            Files.writeString(temporary, json);
            try {
                Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
