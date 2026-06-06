package com.cyclinglab.platform.library.exception;

import java.util.UUID;

/**
 * Thrown when a 404 is the correct response for a library / template lookup.
 * The "lookup another user's id" case is mapped to NOT_FOUND instead of
 * FORBIDDEN to avoid leaking existence (§15.1.3).
 */
public class TemplateNotFoundException extends RuntimeException {

    public TemplateNotFoundException(UUID id) {
        super("Workout template not found: " + id);
    }
}
