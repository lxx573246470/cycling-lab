package com.cyclinglab.platform.training.dto;

import com.cyclinglab.platform.training.TrainingFileStatus;
import java.time.Instant;
import java.util.UUID;

/** Full file detail; embeds the parsed session summary if any. */
public record TrainingFileDetailDto(
    UUID id,
    int isoYear,
    int isoWeek,
    String originalFilename,
    String sportType,
    long sizeBytes,
    String sha256,
    TrainingFileStatus status,
    String failureMessage,
    Instant recordedAt,
    Instant createdAt,
    Instant updatedAt,
    TrainingSessionSummaryDto session
) {}
