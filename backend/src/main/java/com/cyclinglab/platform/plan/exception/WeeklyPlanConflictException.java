package com.cyclinglab.platform.plan.exception;

import java.util.UUID;

/**
 * Thrown when a user tries to create a second weekly plan for the same
 * {@code (isoYear, isoWeek)}. Mapped to 409.
 */
public class WeeklyPlanConflictException extends RuntimeException {

    public record Conflict(UUID conflictWith) {}

    private final Conflict conflict;

    public WeeklyPlanConflictException(int isoYear, int isoWeek, UUID conflictWith) {
        super("Weekly plan already exists for " + isoYear + "-W" + isoWeek);
        this.conflict = new Conflict(conflictWith);
    }

    public Conflict getConflict() {
        return conflict;
    }
}