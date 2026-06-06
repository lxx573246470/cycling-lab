package com.cyclinglab.platform.training.dto;

/** One record sample; used by the per-second detail stream. */
public record TrainingSampleDto(
    int tOffsetSec,
    Integer hr,
    Integer power,
    Integer cadence,
    Double speedMps,
    Double altitudeM,
    Double lat,
    Double lon
) {}
