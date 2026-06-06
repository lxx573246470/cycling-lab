package com.cyclinglab.platform.plan.dto;

import java.time.Instant;
import java.util.UUID;

/** Compact weekly-plan row for list views and dashboard widgets. */
public record WeeklyPlanSummaryDto(
    UUID id,
    Integer isoYear,
    Integer isoWeek,
    String title,
    WeeklyPlanDto.WeeklyPlanProgress progress,
    Instant updatedAt
) {}