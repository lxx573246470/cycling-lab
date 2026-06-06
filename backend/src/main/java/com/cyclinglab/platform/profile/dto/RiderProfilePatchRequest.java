package com.cyclinglab.platform.profile.dto;

import com.cyclinglab.platform.profile.RiderProfileEntity.Bike;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Body for {@code PATCH /api/v1/profile}. Every field is optional; a
 * {@code null} or absent field means "do not change". This shape is required
 * by the design (§15.1.3): "字段为空表示不改".
 */
public record RiderProfilePatchRequest(
    @Size(max = 64) String displayName,
    @Min(100) @Max(230) Short heightCm,
    @Min(value = 30) @Max(value = 200) BigDecimal weightKg,
    @Min(100) @Max(230) Short maxHr,
    @Min(30) @Max(120) Short restingHr,
    @Min(100) @Max(230) Short thresholdHr,
    @Min(50) @Max(600) Short ftp,
    @Min(40) @Max(130) Short cadenceLow,
    @Min(40) @Max(130) Short cadenceHigh,
    @Valid List<Bike> bikes,
    @Size(max = 128) String powerMeter,
    @Size(max = 128) String hrStrap,
    @Size(max = 128) String headUnit,
    Map<String, String> goals,
    Map<String, Object> preferences,
    Boolean isPublic
) {}
