package com.cyclinglab.platform.plan;

import java.time.Instant;
import java.util.UUID;

/**
 * Daily plan status. The set is fixed; the frontend renders different colours
 * and copy per value (see doc/ARCHITECTURE.md 搂6.4).
 */
public enum DailyPlanStatus {
    PLANNED,
    DONE,
    PARTIAL,
    SKIPPED,
    RESCHEDULED;

    public static DailyPlanStatus fromCode(String s) {
        if (s == null || s.isBlank()) {
            return PLANNED;
        }
        try {
            return DailyPlanStatus.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown daily plan status: " + s);
        }
    }
}