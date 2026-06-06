package com.cyclinglab.platform.training;

import java.time.Instant;
import java.util.UUID;

/** Lifecycle states for an uploaded training file. */
public enum TrainingFileStatus {
    PENDING,
    PARSING,
    READY,
    FAILED;

    public static TrainingFileStatus fromCode(String s) {
        if (s == null || s.isBlank()) return PENDING;
        try {
            return TrainingFileStatus.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown training file status: " + s);
        }
    }
}