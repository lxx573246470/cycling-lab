package com.cyclinglab.platform.workout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Body for {@code POST /api/v1/workout-files}. Exactly one of
 * {@code sourceTemplateId} or {@code structureJson} must be supplied.
 */
public record WorkoutFileCreateRequest(
    @NotBlank @Size(max = 128) String name,
    @Pattern(regexp = "bike|run|row") String sportType,
    @Size(max = 20_000) String description,
    List<@Size(max = 32) String> tags,
    UUID sourceTemplateId,
    @NotNull String structureJson
) {}