package com.vsas;

import com.vsas.models.Scroll;
import com.vsas.services.AuthService;
import com.vsas.services.ScrollService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Explicit, non-destructive setup of synthetic accounts and documents for a local demo. */
public final class DemoData {
    private DemoData() { }

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            throw new IllegalArgumentException("Usage: DemoData [empty-data-directory]");
        }
        Path destination = Path.of(args.length == 1 ? args[0]
                : System.getProperty("scrollvault.dataDir", "data"));
        seed(destination);
        System.out.println("Created synthetic demo data in " + destination.toAbsolutePath().normalize());
        System.out.println("Demo accounts: admin / admin123 and reader / reader123");
        System.out.println("These public credentials are for local demonstration only.");
    }

    /** Refuses an existing nonempty directory, file, or symbolic link before writing any data. */
    public static void seed(Path destination) throws IOException {
        Path dataDirectory = destination.toAbsolutePath().normalize();
        if (Files.exists(dataDirectory, LinkOption.NOFOLLOW_LINKS)) {
            if (!Files.isDirectory(dataDirectory, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Demo data destination must be a real directory: " + dataDirectory);
            }
            try (var entries = Files.list(dataDirectory)) {
                if (entries.findAny().isPresent()) {
                    throw new IOException("Refusing to overwrite existing data in " + dataDirectory
                            + ". Choose a new or empty directory.");
                }
            }
        } else {
            Files.createDirectories(dataDirectory);
        }

        // An explicit empty catalogue prevents legacy bundled fallback records from entering the demo.
        Files.writeString(dataDirectory.resolve("scrolls.json"), "[]", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW);
        AuthService auth = new AuthService(dataDirectory.resolve("users.json"));
        auth.register("ADMIN-0001", "admin", "Demo Administrator", "admin@example.com", "",
                "admin123", "ADMIN");
        auth.register("READER-0001", "reader", "Demo Reader", "reader@example.com", "",
                "reader123", "GENERAL");

        ScrollService scrolls = new ScrollService(dataDirectory.resolve("scrolls.json"),
                dataDirectory.resolve("files"));
        addText(scrolls, "S-001", "ADMIN-0001", "Welcome to ScrollVault", "welcome.txt", """
                Welcome to ScrollVault

                This library contains synthetic documents created for the portfolio demo.
                No real user records or private documents are included.

                Try the application:
                1. Browse as a guest and preview a document.
                2. Sign in as reader to upload, download, and manage your own documents.
                3. Open My Scrolls to edit a text document or change its title.
                4. Sign in as admin to inspect user management and usage statistics.

                Demo credentials are public. Use this dataset only for local evaluation.
                """);
        addText(scrolls, "S-002", "READER-0001", "Architecture Notes", "architecture-notes.txt", """
                Architecture Notes

                JavaFX FXML views delegate interactions to controllers.
                Authentication and document services manage application operations.
                JSON files store account records, document metadata, and activity counters.
                Uploaded document contents live in the data directory's files folder.

                This is a single-instance desktop application. Local file permissions
                protect its data; the interface is not a network security boundary.
                """);
        addText(scrolls, "S-003", "READER-0001", "Demo Checklist", "demo-checklist.txt", """
                Demo Checklist

                [ ] Search by title and filter the document list by upload date.
                [ ] Download a text document and compare its contents with the preview.
                [ ] Upload two files with the same filename and verify both remain distinct.
                [ ] Rename a document in My Scrolls, then edit its text content.
                [ ] Refresh the administrator's statistics after previewing a document.

                This checklist is sample content, not a claim that these checks have passed.
                """);
    }

    private static void addText(ScrollService scrolls, String id, String uploaderId,
                                String title, String fileName, String content) throws IOException {
        Path temporaryDirectory = Files.createTempDirectory("scrollvault-demo-");
        Path source = temporaryDirectory.resolve(fileName);
        try {
            Files.writeString(source, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            scrolls.add(new Scroll(id, uploaderId, title, "TEXT", ""), source.toFile());
        } finally {
            Files.deleteIfExists(source);
            Files.deleteIfExists(temporaryDirectory);
        }
    }
}
