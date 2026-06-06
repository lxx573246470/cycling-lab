package com.cyclinglab.platform.review.dto;

import com.cyclinglab.platform.review.ReviewScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/** Body of POST /api/v1/reviews. */
public record ReviewCreateRequest(
    @NotNull ReviewScope scope,
    UUID scopeId,
    Integer isoYear,
    Integer isoWeek,
    LocalDate periodStart,
    LocalDate periodEnd,
    @NotBlank @Size(max = 200) String title,
    @NotNull @Size(max = 100_000) String contentMd,
    Object metrics
) {}