package com.cyclinglab.platform.library.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkoutTemplateListItem(
    UUID id,
    String name,
    String category,
    String intensity,
    List<String> tags,
    int blockCount,
    long totalDurationSec,
    boolean archived,
    Instant updatedAt
) {}
