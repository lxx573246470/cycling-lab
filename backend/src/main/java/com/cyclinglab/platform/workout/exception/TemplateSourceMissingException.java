package com.cyclinglab.platform.workout.exception;

/**
 * Thrown when the requested template does not exist or the user has no
 * permission to read it. Maps to 404 to avoid leaking existence.
 */
public class TemplateSourceMissingException extends RuntimeException {
    public TemplateSourceMissingException(String message) {
        super(message);
    }
}