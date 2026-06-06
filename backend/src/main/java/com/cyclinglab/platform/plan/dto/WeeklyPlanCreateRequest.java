package com.cyclinglab.platform.plan.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body for {@code POST /api/v1/plans/weeks}. The system fills in the seven
 * daily plans automatically (Mon..Sun derived from {@code isoYear} +
 * {@code isoWeek}).
 */
public record WeeklyPlanCreateRequest(
    @NotNull @Min(2000) @Max(2100) Integer isoYear,
    @NotNull @Min(1) @Max(53) Integer isoWeek,
    @Size(max = 128) String title,
    String goalMd
) {}