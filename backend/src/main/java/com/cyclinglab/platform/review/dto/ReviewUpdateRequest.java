package com.cyclinglab.platform.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Body of PUT /api/v1/reviews/{id} (full replace). */
public record ReviewUpdateRequest(
    @NotNull Integer isoYear,
    @NotNull Integer isoWeek,
    LocalDate periodStart,
    LocalDate periodEnd,
    @NotBlank @Size(max = 200) String title,
    @NotNull @Size(max = 100_000) String contentMd,
    Object metrics
) {}