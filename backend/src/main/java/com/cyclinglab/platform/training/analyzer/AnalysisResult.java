package com.cyclinglab.platform.training.analyzer;

import java.time.Instant;
import java.util.List;

/** Output of the FIT analyzer: session summary + per-second record stream. */
public record AnalysisResult(
    Instant startedAt,
    String sport,
    String device,
    int durationSec,
    Double distanceM,
    Double energyKj,
    int avgHr,
    int maxHr,
    int avgPower,
    int maxPower,
    int normalizedPower,
    Double intensityFactor,
    Double trainingStressScore,
    int avgCadence,
    int maxCadence,
    Double hrDriftPct,
    String hrZoneDistributionJson,
    String powerZoneDistributionJson,
    String cadenceZoneDistributionJson,
    List<TenMinSegment> tenMinSegments,
    List<BestRolling> bestRolling,
    List<Sample> samples,
    int sampleCount,
    Integer isoYear,
    Integer isoWeek
) {
    public String tenMinSegmentsJson() {
        return JsonCodec.toJson(tenMinSegments);
    }
    public String bestRollingJson() {
        return JsonCodec.toJson(bestRolling);
    }
}