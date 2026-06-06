package com.cyclinglab.platform.training.analyzer;

/**
 * Intensity Factor (IF) = NP / FTP. The Training Stress Score (TSS) needs
 * both NP and ride duration; we compute it directly in
 * {@link FitAnalyzer#analyze} once the duration is known, so this class only
 * does the ratio. Without an explicit FTP we return null.
 */
final class IntensityFactorCalculator {

    record IfResult(Double intensityFactor) {}

    private IntensityFactorCalculator() {}

    static IfResult compute(int normalizedPower) {
        return compute(normalizedPower, null);
    }

    static IfResult compute(int normalizedPower, Integer ftp) {
        if (normalizedPower <= 0 || ftp == null || ftp <= 0) {
            return new IfResult(null);
        }
        double ifVal = (double) normalizedPower / ftp;
        return new IfResult(round2(ifVal));
    }

    /**
     * Standard TSS = (durationSec * NP * IF) / (FTP * 3600) * 100. Returns
     * null when IF is null or duration is non-positive.
     */
    static Double tss(int durationSec, int normalizedPower, Integer ftp) {
        IfResult r = compute(normalizedPower, ftp);
        if (r.intensityFactor() == null || durationSec <= 0) return null;
        double ifVal = r.intensityFactor();
        double tss = (double) durationSec * normalizedPower * ifVal / (ftp * 3600.0) * 100.0;
        return round2(tss);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
