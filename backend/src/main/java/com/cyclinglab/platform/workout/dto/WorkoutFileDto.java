package com.cyclinglab.platform.workout.dto;

import com.cyclinglab.platform.workout.WorkoutFileFormat;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkoutFileDto(
    UUID id,
    String name,
    String sportType,
    List<String> tags,
    String description,
    String xml,
    UUID sourceTemplateId,
    WorkoutFileFormat format,
    long sizeBytes,
    Instant createdAt,
    Instant updatedAt
) {}