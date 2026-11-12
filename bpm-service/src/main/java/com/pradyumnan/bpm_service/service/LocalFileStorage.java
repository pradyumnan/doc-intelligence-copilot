package com.pradyumnan.bpm_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class LocalFileStorage implements FileStorage {

    // Keys are generated here, never taken from a user, and must look exactly like this
    private static final Pattern KEY = Pattern.compile("[0-9a-f-]{36}\\.(png|jpg)");

    private final Path root;

    public LocalFileStorage(@Value("${app.storage.dir:uploads}") String dir) throws IOException {
        this.root = Path.of(dir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Override
    public String save(byte[] content, String extension) throws IOException {
        String key = UUID.randomUUID() + "." + extension;
        Files.write(resolve(key), content);
        return key;
    }

    @Override
    public byte[] load(String key) throws IOException {
        return Files.readAllBytes(resolve(key));
    }

    private Path resolve(String key) {
        if (key == null || !KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) { // belt and braces against path tricks
            throw new IllegalArgumentException("Invalid storage key");
        }
        return path;
    }
}
