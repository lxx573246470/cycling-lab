package com.cyclinglab.platform.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WorkoutTemplateCreateRequest(
    @NotBlank @Size(max = 128) String name,
    @NotBlank @Pattern(regexp = "endurance|recovery|intervals|outdoor|testing|strength|uncategorized")
        String category,
    @Size(max = 32) String intensity,
    String descriptionMd,
    @NotNull String structureJson,
    java.util.List<@Size(max = 32) String> tags
) {}
