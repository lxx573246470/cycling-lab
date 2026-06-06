package com.cyclinglab.platform.training.analyzer;

import java.util.List;

/** One (down-sampled) record of a parsed session. */
public record Sample(
    int tOffsetSec,
    Integer hr,
    Integer power,
    Integer cadence,
    Double speed,
    Double altitude,
    Double lat,
    Double lon
) {}