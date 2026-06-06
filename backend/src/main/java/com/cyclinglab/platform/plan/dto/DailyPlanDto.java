package com.cyclinglab.platform.plan.dto;

import com.cyclinglab.platform.plan.DailyPlanStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Single day inside a weekly plan. */
public record DailyPlanDto(
    UUID id,
    LocalDate date,
    Integer weekday,
    String targetText,
    UUID templateId,
    Integer templateVersion,
    String templateName,
    String notesMd,
    DailyPlanStatus status,
    UUID actualSessionId,
    Instant updatedAt
) {}