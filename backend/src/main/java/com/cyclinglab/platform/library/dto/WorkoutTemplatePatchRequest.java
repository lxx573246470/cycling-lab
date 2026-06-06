package com.cyclinglab.platform.library.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Partial update for a template. {@code structure} is intentionally NOT
 * patchable: changing the structure must go through {@code PUT} so that a new
 * version row is written (§15.1.4).
 */
public record WorkoutTemplatePatchRequest(
    @Size(max = 128) String name,
    @Pattern(regexp = "endurance|recovery|intervals|outdoor|testing|strength|uncategorized")
        String category,
    @Size(max = 32) String intensity,
    String descriptionMd,
    List<@Size(max = 32) String> tags,
    Boolean archived
) {}
