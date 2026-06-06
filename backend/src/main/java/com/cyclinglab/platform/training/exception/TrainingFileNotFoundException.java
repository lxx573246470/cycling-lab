package com.cyclinglab.platform.training.exception;

import java.util.UUID;

/** Thrown by TrainingService when the requested file does not exist (or belongs to another user). */
public class TrainingFileNotFoundException extends RuntimeException {

    private final UUID id;

    public TrainingFileNotFoundException(UUID id) {
        super("Training file not found: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
