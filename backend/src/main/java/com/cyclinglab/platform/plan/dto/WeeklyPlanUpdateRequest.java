package com.cyclinglab.platform.plan.dto;

import jakarta.validation.constraints.Size;

/** Body for {@code PUT /api/v1/plans/weeks/{id}}. */
public record WeeklyPlanUpdateRequest(
    @Size(max = 128) String title,
    String goalMd
) {}