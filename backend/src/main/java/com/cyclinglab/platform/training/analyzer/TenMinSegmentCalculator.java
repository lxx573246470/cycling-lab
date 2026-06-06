package com.cyclinglab.platform.training.analyzer;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits the ride into consecutive 10-minute (600 s) buckets and averages
 * power, heart rate and cadence over each bucket. The label is
 * {@code "<start_min>-<end_min> min"} to match the legacy Python output.
 */
final class TenMinSegmentCalculator {

    private static final int BUCKET_SEC = 600;

    private TenMinSegmentCalculator() {}

    static List<TenMinSegment> compute(List<Sample> samples) {
        List<TenMinSegment> out = new ArrayList<>();
        if (samples == null || samples.isEmpty()) return out;
        for (int start = 0; start < samples.size(); start += BUCKET_SEC) {
            int end = Math.min(samples.size(), start + BUCKET_SEC);
            int size = end - start;
            if (size == 0) continue;
            int startMin = start / 60;
            int endMin = (int) Math.ceil(end / 60.0);
            String label = String.format("%02d-%02d min", startMin, endMin);
            out.add(new TenMinSegment(
                label,
                avgPower(samples, start, end),
                avgHr(samples, start, end),
                avgCadence(samples, start, end)
            ));
        }
        return out;
    }

    private static Double avgPower(List<Sample> s, int from, int to) {
        long sum = 0; int n = 0;
        for (int i = from; i < to; i++) {
            Integer v = s.get(i).power();
            if (v != null) { sum += v; n++; }
        }
        return n == 0 ? null : round1((double) sum / n);
    }

    private static Double avgHr(List<Sample> s, int from, int to) {
        long sum = 0; int n = 0;
        for (int i = from; i < to; i++) {
            Integer v = s.get(i).hr();
            if (v != null) { sum += v; n++; }
        }
        return n == 0 ? null : round1((double) sum / n);
    }

    private static Double avgCadence(List<Sample> s, int from, int to) {
        long sum = 0; int n = 0;
        for (int i = from; i < to; i++) {
            Integer v = s.get(i).cadence();
            if (v != null) { sum += v; n++; }
        }
        return n == 0 ? null : round1((double) sum / n);
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
