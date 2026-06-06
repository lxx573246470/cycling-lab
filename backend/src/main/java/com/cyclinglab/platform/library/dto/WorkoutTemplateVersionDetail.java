package com.cyclinglab.platform.library.dto;

import java.time.Instant;

public record WorkoutTemplateVersionDetail(
    int version,
    String structureJson,
    String changeNote,
    Instant createdAt
) {}
