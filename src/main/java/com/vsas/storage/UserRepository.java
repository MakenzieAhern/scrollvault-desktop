package com.vsas.storage;

import com.google.gson.*;
import com.vsas.models.User;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class UserRepository {
    private final Path file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, User> byUsername = new HashMap<>();

    public UserRepository() throws IOException {
        this(Path.of(System.getProperty("scrollvault.dataDir", "data"), "users.json"));
    }

    public UserRepository(Path file) throws IOException {
        this.file = file;
        if (!Files.exists(file)) {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, "[]");
        }
        try (Reader r = Files.newBufferedReader(file)) {
            User[] arr = gson.fromJson(r, User[].class);
            if (arr != null) for (User u : arr) byUsername.put(u.getUsername(), u);
        }
    }

    public synchronized void save(User u) throws IOException {
        byUsername.put(u.getUsername(), u);
        JsonFiles.write(file, gson.toJson(byUsername.values()));
    }

    public User findByUsername(String username) { return byUsername.get(username); }

    public boolean exists(String username) { return byUsername.containsKey(username); }

    public Collection<User> findAll() { return byUsername.values(); }
}
