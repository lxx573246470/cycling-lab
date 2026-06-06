package com.cyclinglab.platform.plan.exception;

import java.util.UUID;

/**
 * Thrown when the requested day id does not exist for the given weekly plan.
 * Mapped to 404.
 */
public class DailyPlanNotFoundException extends RuntimeException {

    public DailyPlanNotFoundException(UUID id) {
        super("Daily plan not found: " + id);
    }
}