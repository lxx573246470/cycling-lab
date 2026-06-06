package com.cyclinglab.platform.training.analyzer;

import java.util.ArrayList;
import java.util.List;

/**
 * Best rolling average power over a set of standard windows (5, 30, 60, 300,
 * 600, 1200, 1800 seconds). Mirrors the legacy {@code analyze_fit.py} output.
 */
final class BestRollingCalculator {

    private static final int[] WINDOWS = { 5, 30, 60, 300, 600, 1200, 1800 };

    private BestRollingCalculator() {}

    static List<BestRolling> compute(List<Sample> samples) {
        List<BestRolling> out = new ArrayList<>();
        if (samples == null || samples.isEmpty()) return out;
        for (int w : WINDOWS) {
            if (samples.size() < w) continue;
            int bestPower = 0;
            int bestOffset = 0;
            long sum = 0;
            int count = 0;
            // Sliding window: track (sum, count) of power entries
            for (int i = 0; i < samples.size(); i++) {
                Integer v = samples.get(i).power();
                if (v != null) { sum += v; count++; }
                if (i >= w) {
                    Integer old = samples.get(i - w).power();
                    if (old != null) { sum -= old; count--; }
                }
                if (i >= w - 1 && count == w) {
                    int avg = (int) Math.round((double) sum / w);
                    if (avg > bestPower) {
                        bestPower = avg;
                        bestOffset = i - w + 1;
                    }
                }
            }
            out.add(new BestRolling(w, bestPower, bestOffset));
        }
        return out;
    }
}
