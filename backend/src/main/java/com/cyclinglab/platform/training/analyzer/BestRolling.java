package com.cyclinglab.platform.training.analyzer;

/** Best rolling average over a fixed window. */
public record BestRolling(int windowSec, int avgPower, int atOffsetSec) {}