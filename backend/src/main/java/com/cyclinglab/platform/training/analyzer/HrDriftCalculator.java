package com.cyclinglab.platform.training.analyzer;

import java.util.List;

/**
 * HR decoupling: (HR2/power2) / (HR1/power1) - 1, computed over the second
 * vs the first half of the ride. Returns null when the ride is too short or
 * any of the four halves is missing.
 */
final class HrDriftCalculator {

    private HrDriftCalculator() {}

    static Double compute(List<Sample> samples) {
        if (samples == null || samples.size() < 120) return null;
        int mid = samples.size() / 2;

        Double p1 = avg(samples.subList(0, mid), true);
        Double p2 = avg(samples.subList(mid, samples.size()), true);
        Double h1 = avg(samples.subList(0, mid), false);
        Double h2 = avg(samples.subList(mid, samples.size()), false);
        if (p1 == null || p2 == null || h1 == null || h2 == null) return null;
        if (p1 == 0 || p2 == 0 || h1 == 0 || h2 == 0) return null;
        return ((h2 / p2) / (h1 / p1) - 1) * 100;
    }

    private static Double avg(List<Sample> window, boolean wantPower) {
        long sum = 0;
        int count = 0;
        for (Sample s : window) {
            Integer v = wantPower ? s.power() : s.hr();
            if (v != null) { sum += v; count++; }
        }
        return count == 0 ? null : (double) sum / count;
    }
}