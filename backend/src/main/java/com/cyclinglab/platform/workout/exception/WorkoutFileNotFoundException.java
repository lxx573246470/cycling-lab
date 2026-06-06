package com.cyclinglab.platform.workout.exception;

import java.util.UUID;

public class WorkoutFileNotFoundException extends RuntimeException {
    public WorkoutFileNotFoundException(UUID id) {
        super("Workout file not found: " + id);
    }
}