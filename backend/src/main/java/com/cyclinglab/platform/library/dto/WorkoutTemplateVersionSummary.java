package com.cyclinglab.platform.library.dto;

import java.time.Instant;

public record WorkoutTemplateVersionSummary(
    int version,
    String changeNote,
    Instant createdAt
) {}
