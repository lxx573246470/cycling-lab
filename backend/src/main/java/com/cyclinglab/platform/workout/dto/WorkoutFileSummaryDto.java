package com.cyclinglab.platform.workout.dto;

import com.cyclinglab.platform.workout.WorkoutFileFormat;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkoutFileSummaryDto(
    UUID id,
    String name,
    String sportType,
    List<String> tags,
    WorkoutFileFormat format,
    long sizeBytes,
    UUID sourceTemplateId,
    Instant createdAt
) {}