package com.vsas.storage;

import com.vsas.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryTest {

    @TempDir
    Path tempDir;

    private User newUser(String username) {
        User u = new User();
        try {
            set(u, "username", username);
            set(u, "fullName", "Test User");
            set(u, "email", "test@example.com");
            set(u, "phone", "0123456789");
            set(u, "type", "GENERAL");
            set(u, "passwordHash", "salt:hash");
            set(u, "idKey", "test1234");
        } catch (Exception e) { throw new RuntimeException(e); }
        return u;
    }

    @Test
    void save_and_find_user_in_memory_and_file() throws IOException {
        Path usersFile = tempDir.resolve("users.json");
        Files.writeString(usersFile, "[]");

        UserRepository repo = new UserRepository(usersFile);

        User u = newUser("alpha");
        repo.save(u);

        assertTrue(repo.exists("alpha"));
        assertNotNull(repo.findByUsername("alpha"));

        String json = Files.readString(usersFile);
        assertTrue(json.contains("alpha"));
    }

    @Test
    void findAll_returns_saved_users() throws IOException {
        Path usersFile = tempDir.resolve("users.json");
        Files.writeString(usersFile, "[]");

        UserRepository repo = new UserRepository(usersFile);

        repo.save(newUser("u1"));
        repo.save(newUser("u2"));

        assertEquals(2, repo.findAll().size());
    }

    @Test
    void constructor_creates_file_and_directories_if_not_exists() throws IOException {
        Path file = tempDir.resolve("nested/users.json");
        UserRepository repository = new UserRepository(file);
        assertTrue(repository.findAll().isEmpty());
        assertEquals("[]", Files.readString(file));
    }

    @Test
    void constructor_loads_existing_users_from_file() throws IOException {
        Path usersFile = tempDir.resolve("users.json");

        // Create a JSON file with existing users
        String json = "[{\"username\":\"alice\",\"fullName\":\"Alice Test\",\"email\":\"alice@test.com\"," +
                      "\"phone\":\"1111111111\",\"type\":\"GENERAL\",\"passwordHash\":\"hash1\",\"idKey\":\"key1\"}," +
                      "{\"username\":\"bob\",\"fullName\":\"Bob Test\",\"email\":\"bob@test.com\"," +
                      "\"phone\":\"2222222222\",\"type\":\"ADMIN\",\"passwordHash\":\"hash2\",\"idKey\":\"key2\"}]";
        Files.writeString(usersFile, json);

        // Create repository - it should load the users
        UserRepository repo = createRepoWithPath(usersFile);

        // Verify users were loaded
        assertTrue(repo.exists("alice"));
        assertTrue(repo.exists("bob"));
        assertEquals(2, repo.findAll().size());

        User alice = repo.findByUsername("alice");
        assertNotNull(alice);
        assertEquals("Alice Test", alice.getFullName());

        User bob = repo.findByUsername("bob");
        assertNotNull(bob);
        assertEquals("ADMIN", bob.getType());
    }

    @Test
    void constructor_handles_null_array_from_json() throws IOException {
        Path usersFile = tempDir.resolve("users.json");

        // Write null or invalid JSON that results in null array
        Files.writeString(usersFile, "null");

        // Create repository - should handle null gracefully
        UserRepository repo = createRepoWithPath(usersFile);

        // Verify no users were loaded
        assertEquals(0, repo.findAll().size());
    }

    @Test
    void constructor_handles_empty_json_file() throws IOException {
        Path usersFile = tempDir.resolve("users.json");

        // Write empty JSON array
        Files.writeString(usersFile, "[]");

        // Create repository
        UserRepository repo = createRepoWithPath(usersFile);

        // Verify no users were loaded
        assertEquals(0, repo.findAll().size());
        assertFalse(repo.exists("anyuser"));
        assertNull(repo.findByUsername("anyuser"));
    }

    @Test
    void constructor_handles_empty_array_with_whitespace() throws IOException {
        Path usersFile = tempDir.resolve("users.json");

        // Write empty JSON array with whitespace
        Files.writeString(usersFile, "  [  ]  ");

        // Create repository
        UserRepository repo = createRepoWithPath(usersFile);

        // Verify no users were loaded
        assertEquals(0, repo.findAll().size());
    }

    @Test
    void exists_returns_false_for_nonexistent_user() throws IOException {
        Path usersFile = tempDir.resolve("users.json");
        Files.writeString(usersFile, "[]");

        UserRepository repo = new UserRepository(usersFile);

        assertFalse(repo.exists("nonexistent"));
    }

    @Test
    void exists_returns_true_for_existing_user() throws IOException {
        Path usersFile = tempDir.resolve("users.json");
        Files.writeString(usersFile, "[]");

        UserRepository repo = new UserRepository(usersFile);

        repo.save(newUser("testuser"));
        assertTrue(repo.exists("testuser"));
    }

    @Test
    void findByUsername_returns_null_for_nonexistent_user() throws IOException {
        Path usersFile = tempDir.resolve("users.json");
        Files.writeString(usersFile, "[]");

        UserRepository repo = new UserRepository(usersFile);

        assertNull(repo.findByUsername("nonexistent"));
    }

    @Test
    void findByUsername_returns_user_for_existing_user() throws IOException {
        Path usersFile = tempDir.resolve("users.json");
        Files.writeString(usersFile, "[]");

        UserRepository repo = new UserRepository(usersFile);

        User user = newUser("testuser");
        repo.save(user);

        User found = repo.findByUsername("testuser");
        assertNotNull(found);
        assertEquals("testuser", found.getUsername());
    }

    @Test
    void save_overwrites_existing_user_with_same_username() throws IOException {
        Path usersFile = tempDir.resolve("users.json");
        Files.writeString(usersFile, "[]");

        UserRepository repo = new UserRepository(usersFile);

        // Save first version
        User user1 = newUser("testuser");
        repo.save(user1);

        // Save second version with same username
        User user2 = newUser("testuser");
        try {
            set(user2, "fullName", "Updated Name");
        } catch (Exception e) {
            fail(e);
        }
        repo.save(user2);

        // Verify only one user exists and it's the updated one
        assertEquals(1, repo.findAll().size());
        User found = repo.findByUsername("testuser");
        assertEquals("Updated Name", found.getFullName());
    }

    @Test
    void save_persists_user_to_file() throws IOException {
        Path usersFile = tempDir.resolve("users.json");
        Files.writeString(usersFile, "[]");

        UserRepository repo = new UserRepository(usersFile);

        User user = newUser("testuser");
        repo.save(user);

        // Read file directly and verify user was persisted
        String content = Files.readString(usersFile);
        assertTrue(content.contains("testuser"));
        assertTrue(content.contains("Test User"));
    }

    @Test
    void findAll_returns_empty_collection_when_no_users() throws IOException {
        Path usersFile = tempDir.resolve("users.json");
        Files.writeString(usersFile, "[]");

        UserRepository repo = new UserRepository(usersFile);

        assertTrue(repo.findAll().isEmpty());
    }

    @Test
    void findAll_returns_all_saved_users() throws IOException {
        Path usersFile = tempDir.resolve("users.json");
        Files.writeString(usersFile, "[]");

        UserRepository repo = new UserRepository(usersFile);

        repo.save(newUser("user1"));
        repo.save(newUser("user2"));
        repo.save(newUser("user3"));

        assertEquals(3, repo.findAll().size());
    }

    @Test
    void save_is_thread_safe() throws Exception {
        Path usersFile = tempDir.resolve("users.json");
        Files.writeString(usersFile, "[]");

        UserRepository repo = new UserRepository(usersFile);

        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> {
                for (int i = 0; i < 10; i++) repo.save(newUser("user_t1_" + i));
                return null;
            });
            var second = executor.submit(() -> {
                for (int i = 0; i < 10; i++) repo.save(newUser("user_t2_" + i));
                return null;
            });
            // Future.get propagates worker failures to JUnit's test thread.
            first.get();
            second.get();
        } finally {
            executor.shutdownNow();
        }

        // Verify all users were saved
        assertEquals(20, repo.findAll().size());
    }

    @Test
    void constructor_handles_malformed_json() throws IOException {
        Path usersFile = tempDir.resolve("users.json");

        // Write malformed JSON
        Files.writeString(usersFile, "{this is not valid json}");

        // Constructor should throw exception or handle gracefully
        try {
            UserRepository repo = createRepoWithPath(usersFile);
            // If it doesn't throw, verify it handled gracefully
            assertNotNull(repo);
        } catch (Exception e) {
            // Expected - malformed JSON should cause an exception
            assertTrue(e instanceof IOException || e instanceof com.google.gson.JsonSyntaxException);
        }
    }

    private static void set(Object target, String field, Object value) throws Exception {
        var f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private UserRepository createRepoWithPath(Path path) throws IOException {
        return new UserRepository(path);
    }
}
