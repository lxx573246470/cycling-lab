package com.cyclinglab.platform.library;

import java.util.List;

/**
 * The 6 top-level categories plus {@code uncategorized} which is the bucket
 * the markdown importer uses when a plan has no {@code category} frontmatter
 * (ImportReport surfaces a warning in that case).
 */
public enum WorkoutCategory {
    ENDURANCE("endurance"),
    RECOVERY("recovery"),
    INTERVALS("intervals"),
    OUTDOOR("outdoor"),
    TESTING("testing"),
    STRENGTH("strength"),
    UNCATEGORIZED("uncategorized");

    private final String code;

    WorkoutCategory(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static WorkoutCategory fromCode(String code) {
        if (code == null) return UNCATEGORIZED;
        for (WorkoutCategory c : values()) {
            if (c.code.equalsIgnoreCase(code)) return c;
        }
        throw new IllegalArgumentException("Unknown workout category: " + code);
    }

    public static List<WorkoutCategory> userSelectable() {
        return List.of(ENDURANCE, RECOVERY, INTERVALS, OUTDOOR, TESTING, STRENGTH);
    }
}
