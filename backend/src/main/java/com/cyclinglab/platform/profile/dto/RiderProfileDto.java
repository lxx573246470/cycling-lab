package com.cyclinglab.platform.profile.dto;

import com.cyclinglab.platform.profile.RiderProfileEntity.Bike;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Full rider profile read/upsert body. The {@code displayName} defaults to the
 * user's account display name; "leave it null in PATCH" means "do not change".
 */
public record RiderProfileDto(
    UUID userId,
    String displayName,
    Short heightCm,
    BigDecimal weightKg,
    Short maxHr,
    Short restingHr,
    Short thresholdHr,
    Short ftp,
    Short cadenceLow,
    Short cadenceHigh,
    List<Bike> bikes,
    String powerMeter,
    String hrStrap,
    String headUnit,
    Map<String, String> goals,
    Map<String, Object> preferences,
    Boolean isPublic,
    Instant createdAt,
    Instant updatedAt
) {}
