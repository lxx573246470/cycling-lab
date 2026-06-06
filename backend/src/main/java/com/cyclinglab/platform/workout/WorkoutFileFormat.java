package com.cyclinglab.platform.workout;

import java.util.Locale;

/** Supported workout file formats. The current M2 milestone only emits ZWO. */
public enum WorkoutFileFormat {
    ZWO,
    ERG,
    MRC,
    ZML;

    public static WorkoutFileFormat fromCode(String s) {
        if (s == null || s.isBlank()) return ZWO;
        try {
            return WorkoutFileFormat.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown workout file format: " + s);
        }
    }
}