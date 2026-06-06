package com.cyclinglab.platform.training.analyzer;

import java.util.ArrayList;
import java.util.List;

/**
 * Normalized Power (NP) = 30-second rolling mean of power, raised to the
 * 4th power, averaged, then the 4th root. See TrainingPeaks / Coggan.
 */
final class NormalizedPowerCalculator {

    private static final int WINDOW = 30;

    private NormalizedPowerCalculator() {}

    static int compute(List<Sample> samples) {
        if (samples == null || samples.size() < WINDOW) return 0;
        List<Integer> power = new ArrayList<>(samples.size());
        for (Sample s : samples) {
            if (s.power() != null) power.add(s.power());
        }
        if (power.size() < WINDOW) return 0;
        // Sliding 30s sum
        double sum = 0;
        for (int i = 0; i < WINDOW; i++) sum += power.get(i);
        double total = Math.pow(sum / WINDOW, 4);
        for (int i = WINDOW; i < power.size(); i++) {
            sum += power.get(i) - power.get(i - WINDOW);
            total += Math.pow(sum / WINDOW, 4);
        }
        int n = power.size() - WINDOW + 1;
        double mean4 = total / n;
        return (int) Math.round(Math.pow(mean4, 0.25));
    }
}