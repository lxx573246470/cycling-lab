package com.cyclinglab.platform.profile.dto;

import java.util.List;

/**
 * Response for {@code GET /api/v1/profile/derived-zones}. The Coggan 7-zone
 * power and HR ranges are computed server-side; the frontend just renders
 * them (see §15.1.6).
 */
public record DerivedZonesResponse(
    List<HrZone> hrZones,
    List<PowerZone> powerZones,
    CadenceRange cadenceRange,
    Integer ftp,
    Integer maxHr,
    Integer thresholdHr,
    String computedAt
) {
    public record HrZone(int zone, String name, double low, double high, int bpmLow, int bpmHigh) {}

    public record PowerZone(int zone, String name, double low, double high, int wattsLow, int wattsHigh) {}

    public record CadenceRange(int low, int high) {}
}
