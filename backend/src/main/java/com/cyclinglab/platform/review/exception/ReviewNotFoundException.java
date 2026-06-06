package com.cyclinglab.platform.review.exception;

import java.util.UUID;

/** Thrown when a review is not found (or belongs to another user). */
public class ReviewNotFoundException extends RuntimeException {

    private final UUID id;

    public ReviewNotFoundException(UUID id) {
        super("Review not found: " + id);
        this.id = id;
    }

    public ReviewNotFoundException(String message) {
        super(message);
        this.id = null;
    }

    public UUID getId() {
        return id;
    }
}