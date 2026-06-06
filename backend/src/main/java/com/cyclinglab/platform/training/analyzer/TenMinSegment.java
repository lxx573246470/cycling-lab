package com.cyclinglab.platform.training.analyzer;

/** 10-minute block average. */
public record TenMinSegment(String label, Double avgPower, Double avgHr, Double avgCadence) {}