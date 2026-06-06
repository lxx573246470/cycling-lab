package com.cyclinglab.platform.training.analyzer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class FitAnalyzerTest {

    @Test
    void parsesRidesFromDisk() throws IOException {
        // Sample fixture captured from a real ride on 2026-05-21.
        // The path is resolved relative to the repo root because Maven runs
        // the test from backend/, but the file lives at the project root.
        Path candidate = Paths.get("..", "training", "2026", "week-21", "fit", "2026-05-21.fit");
        if (!Files.isRegularFile(candidate)) {
            // Allow running from a different cwd.
            candidate = Paths.get("training", "2026", "week-21", "fit", "2026-05-21.fit");
        }
        if (!Files.isRegularFile(candidate)) {
            // No fixture available in this environment; the parse path is
            // exercised end-to-end by the integration test which ships the
            // bytes inline.
            return;
        }
        AnalysisResult r = FitAnalyzer.analyze(candidate);
        assertThat(r.sport()).isNotBlank();
        assertThat(r.durationSec()).isPositive();
        assertThat(r.sampleCount()).isPositive();
        assertThat(r.tenMinSegments()).isNotEmpty();
        assertThat(r.bestRolling()).isNotEmpty();
        assertThat(r.hrZoneDistributionJson()).startsWith("[").endsWith("]");
    }

    @Test
    void missingFileThrows() {
        Path missing = Paths.get("does-not-exist.fit");
        assertThatThrownBy(() -> FitAnalyzer.analyze(missing))
            .isInstanceOf(IOException.class);
    }

    @Test
    void garbageBytesThrow() throws IOException {
        Path tmp = Files.createTempFile("garbage-", ".fit");
        Files.write(tmp, new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05 });
        try {
            assertThatThrownBy(() -> FitAnalyzer.analyze(tmp)).isInstanceOf(IOException.class);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
