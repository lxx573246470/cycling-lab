package com.cyclinglab.platform.training.dto;

import com.cyclinglab.platform.training.TrainingFileStatus;
import java.time.Instant;
import java.util.UUID;

/** Lightweight summary for list views. */
public record TrainingFileSummaryDto(
    UUID id,
    int isoYear,
    int isoWeek,
    String originalFilename,
    String sportType,
    long sizeBytes,
    TrainingFileStatus status,
    Instant recordedAt,
    Instant createdAt
) {}
