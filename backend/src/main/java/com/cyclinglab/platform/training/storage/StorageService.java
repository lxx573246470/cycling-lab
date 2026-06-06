package com.cyclinglab.platform.training.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Storage abstraction for raw training files. The M3 milestone ships a local
 * filesystem implementation; a future MinIO / S3 implementation can plug in
 * here without changing callers.
 */
public interface StorageService {

    /**
     * Stores the bytes under {@code key} (e.g. {@code <userId>/<sha256>.fit}).
     * Returns the resolved absolute key, or throws on I/O error.
     */
    String put(String key, byte[] bytes) throws IOException;

    /** As {@link #put(String, byte[])} but streams from an input stream. */
    String put(String key, InputStream in) throws IOException;

    /** Resolves the absolute filesystem path for a stored key. */
    Path resolve(String key);

    /**
     * Reads all bytes for the given key. Throws if the object is missing.
     */
    byte[] readAllBytes(String key) throws IOException;

    /** Deletes the object if present. No-op if it does not exist. */
    void delete(String key) throws IOException;
}