package com.cyclinglab.platform.profile.dto;

import com.cyclinglab.platform.profile.RiderProfileEntity.Bike;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Body for {@code PUT /api/v1/profile}. All fields are required so the rider
 * is forced to give the data the system needs to compute zones.
 */
public record RiderProfileUpsertRequest(
    @NotBlank @Size(max = 64) String displayName,
    @NotNull @Min(100) @Max(230) Short heightCm,
    @NotNull @Min(value = 30) @Max(value = 200) BigDecimal weightKg,
    @NotNull @Min(100) @Max(230) Short maxHr,
    @Min(30) @Max(120) Short restingHr,
    @Min(100) @Max(230) Short thresholdHr,
    @NotNull @Min(50) @Max(600) Short ftp,
    @NotNull @Min(40) @Max(130) Short cadenceLow,
    @NotNull @Min(40) @Max(130) Short cadenceHigh,
    @Valid List<Bike> bikes,
    @Size(max = 128) String powerMeter,
    @Size(max = 128) String hrStrap,
    @Size(max = 128) String headUnit,
    Map<String, String> goals,
    Map<String, Object> preferences,
    Boolean isPublic
) {}
