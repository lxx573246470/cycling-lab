package com.cyclinglab.platform.library.exception;

import java.util.UUID;

public class TemplateNameConflictException extends RuntimeException {

    public record Conflict(UUID conflictWith) {}

    private final Conflict conflict;

    public TemplateNameConflictException(String name, UUID conflictWith) {
        super("Workout template name already in use: " + name);
        this.conflict = new Conflict(conflictWith);
    }

    public Conflict getConflict() {
        return conflict;
    }
}
