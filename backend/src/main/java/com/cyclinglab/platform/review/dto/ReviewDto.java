package com.cyclinglab.platform.review.dto;

import com.cyclinglab.platform.review.ReviewScope;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Full review representation (list, get, create response). */
public record ReviewDto(
    UUID id,
    ReviewScope scope,
    UUID scopeId,
    Integer isoYear,
    Integer isoWeek,
    LocalDate periodStart,
    LocalDate periodEnd,
    String title,
    String contentMd,
    JsonNode metrics,
    Instant createdAt,
    Instant updatedAt
) {}