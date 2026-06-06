package com.cyclinglab.platform.plan.exception;

import java.util.UUID;

/**
 * Thrown when a weekly plan id does not exist for the current user. Mapped to
 * 404 by {@code GlobalExceptionHandler}.
 */
public class WeeklyPlanNotFoundException extends RuntimeException {

    public WeeklyPlanNotFoundException(UUID id) {
        super("Weekly plan not found: " + id);
    }
}