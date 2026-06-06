package com.cyclinglab.platform.training.storage;

import com.cyclinglab.platform.common.AppProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Local-filesystem implementation of {@link StorageService}. The root is
 * resolved once at startup from {@code cyclinglab.storage.local.root} (or
 * {@code ./data/training} by default). All keys are interpreted as
 * forward-slash-relative paths under that root; path traversal is rejected.
 */
@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements StorageService {

    private final AppProperties appProperties;

    private Path root() {
        String root = appProperties.storage().local() == null
            ? null
            : appProperties.storage().local().root();
        if (root == null || root.isBlank()) {
            root = "./data/training";
        }
        Path r = Paths.get(root).toAbsolutePath().normalize();
        try {
            Files.createDirectories(r);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create storage root: " + r, e);
        }
        return r;
    }

    @Override
    public String put(String key, byte[] bytes) throws IOException {
        Path target = safe(key);
        Files.createDirectories(target.getParent());
        Files.write(target, bytes);
        return key;
    }

    @Override
    public String put(String key, InputStream in) throws IOException {
        Path target = safe(key);
        Files.createDirectories(target.getParent());
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        return key;
    }

    @Override
    public Path resolve(String key) {
        return safe(key);
    }

    @Override
    public byte[] readAllBytes(String key) throws IOException {
        Path p = safe(key);
        if (!Files.exists(p)) {
            throw new IOException("Object not found: " + key);
        }
        return Files.readAllBytes(p);
    }

    @Override
    public void delete(String key) throws IOException {
        Path p = safe(key);
        Files.deleteIfExists(p);
    }

    private Path safe(String key) {
        if (key == null || key.isBlank() || key.contains("..")) {
            throw new IllegalArgumentException("Invalid storage key: " + key);
        }
        Path r = root();
        Path resolved = r.resolve(key).normalize();
        if (!resolved.startsWith(r)) {
            throw new IllegalArgumentException("Storage key escapes root: " + key);
        }
        return resolved;
    }
}
