package com.cyclinglab.platform.library.dto;

import com.cyclinglab.platform.library.WorkoutSource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full template detail (incl. {@code structureJson}). Returned by
 * {@code GET /api/v1/library/templates/{id}} and the create / replace
 * endpoints.
 */
public record WorkoutTemplateDto(
    UUID id,
    String name,
    String category,
    String intensity,
    List<String> tags,
    String descriptionMd,
    String structureJson,
    WorkoutStructureSummaryDto structure,
    WorkoutSource source,
    boolean archived,
    int currentVersion,
    Instant createdAt,
    Instant updatedAt
) {
    public record WorkoutStructureSummaryDto(int blockCount, long totalDurationSec) {}
}
