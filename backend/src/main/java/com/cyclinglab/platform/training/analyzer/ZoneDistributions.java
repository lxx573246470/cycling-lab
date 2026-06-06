package com.cyclinglab.platform.training.analyzer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes the per-zone time distribution for heart rate, power and cadence.
 * The HR and power zones are derived from the ride average (no explicit max
 * HR / FTP is needed). Cadence is bucketed into 5 fixed bins matching the
 * legacy {@code analyze_fit.py} output, so the same note can be produced
 * from the web pipeline and the legacy CLI.
 *
 * <p>The output is JSON (a list of {@code {name, count, pct}} rows) that the
 * front-end can render directly. The same shape is stored in
 * {@code training_session.hr_zone_distribution} (jsonb).
 */
final class ZoneDistributions {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ZoneDistributions() {}

    static String hrJson(List<Sample> samples, int avgHr) {
        if (avgHr <= 0) return "[]";
        return bucketedJson(
            samples,
            s -> s.hr(),
            new double[][] {
                {0,            0.6 * avgHr, 0},
                {0.6 * avgHr,  0.7 * avgHr, 1},
                {0.7 * avgHr,  0.8 * avgHr, 2},
                {0.8 * avgHr,  0.9 * avgHr, 3},
                {0.9 * avgHr,  10_000,      4}
            }
        );
    }

    static String powerJson(List<Sample> samples) {
        return bucketedJson(
            samples,
            s -> s.power(),
            new double[][] {
                {0,   50,   0},
                {50,  100,  1},
                {100, 125,  2},
                {125, 150,  3},
                {150, 175,  4},
                {175, 200,  5},
                {200, 250,  6},
                {250, 10_000, 7}
            }
        );
    }

    static String cadenceJson(List<Sample> samples) {
        return bucketedJson(
            samples,
            s -> s.cadence(),
            new double[][] {
                {0, 1,       0},
                {1, 70,      1},
                {70, 85,     2},
                {85, 101,    3},
                {101, 10_000, 4}
            }
        );
    }

    private static String bucketedJson(List<Sample> samples,
                                        java.util.function.Function<Sample, Integer> pick,
                                        double[][] buckets) {
        long total = 0;
        long[] counts = new long[buckets.length];
        for (Sample s : samples) {
            Integer v = pick.apply(s);
            if (v == null) continue;
            total++;
            for (int i = 0; i < buckets.length; i++) {
                if (v >= buckets[i][0] && v < buckets[i][1]) {
                    counts[i]++;
                    break;
                }
            }
        }
        List<ZoneRow> rows = new ArrayList<>(buckets.length);
        for (int i = 0; i < buckets.length; i++) {
            double pct = total == 0 ? 0.0 : round1(counts[i] * 100.0 / total);
            String name = "Z" + (int) buckets[i][2];
            rows.add(new ZoneRow(name, counts[i], pct));
        }
        try {
            return MAPPER.writeValueAsString(rows);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    public record ZoneRow(String name, long count, double pct) {}
}
