package com.cyclinglab.platform.importer;

import java.util.List;

/**
 * Result of an md-to-database import run. Reported back to the user so they
 * can confirm before anything is committed (design §15.1.5).
 */
public record ImportReport(
    RiderProfileSummary riderProfile,
    List<TemplateSummary> templates
) {
    public record RiderProfileSummary(String status, List<String> warnings) {}

    public record TemplateSummary(
        String name,
        String category,
        String status,
        List<String> warnings
    ) {}
}
