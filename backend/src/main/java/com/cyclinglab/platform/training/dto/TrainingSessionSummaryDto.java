package com.cyclinglab.platform.training.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

/**
 * Parsed session summary. The zone distributions and rolling-best tables
 * are stored as JSON strings in the database, and are surfaced here as
 * already-parsed {@link JsonNode} so the front-end does not need to do a
 * second parse.
 */
public record TrainingSessionSummaryDto(
    UUID id,
    Instant startedAt,
    Integer durationSec,
    Double distanceM,
    Double energyKj,
    Integer avgHr,
    Integer maxHr,
    Integer avgPower,
    Integer maxPower,
    Integer normalizedPower,
    Double intensityFactor,
    Double trainingStressScore,
    Integer avgCadence,
    Integer maxCadence,
    Double hrDriftPct,
    JsonNode hrZoneDistribution,
    JsonNode powerZoneDistribution,
    JsonNode cadenceZoneDistribution,
    JsonNode tenMinSegments,
    JsonNode bestRolling
) {}
