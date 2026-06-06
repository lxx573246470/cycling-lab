package com.cyclinglab.platform.library.dto;

import java.util.List;

public record CategoryDto(String code, String label) {

    public static List<CategoryDto> all() {
        return List.of(
            new CategoryDto("endurance", "Endurance"),
            new CategoryDto("recovery", "Recovery"),
            new CategoryDto("intervals", "Intervals"),
            new CategoryDto("outdoor", "Outdoor"),
            new CategoryDto("testing", "Testing"),
            new CategoryDto("strength", "Strength")
        );
    }
}
