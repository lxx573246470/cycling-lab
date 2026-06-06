package com.cyclinglab.platform.plan.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Full weekly-plan detail (header + 7 day cards). */
public record WeeklyPlanDto(
    UUID id,
    Integer isoYear,
    Integer isoWeek,
    LocalDate weekStart,
    LocalDate weekEnd,
    String title,
    String goalMd,
    List<DailyPlanDto> days,
    WeeklyPlanProgress progress,
    Instant createdAt,
    Instant updatedAt
) {
    /**
     * Per-status count across the seven days. Useful for the dashboard widget
     * and the list-page summary line.
     */
    public record WeeklyPlanProgress(
        int total,
        int planned,
        int done,
        int partial,
        int skipped,
        int rescheduled
    ) {
        public static WeeklyPlanProgress empty() {
            return new WeeklyPlanProgress(0, 0, 0, 0, 0, 0);
        }
    }
}