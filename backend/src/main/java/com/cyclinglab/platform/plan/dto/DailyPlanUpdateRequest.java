package com.cyclinglab.platform.plan.dto;

import com.cyclinglab.platform.plan.DailyPlanStatus;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Body for {@code PUT /api/v1/plans/weeks/{id}/days/{dayId}}. Any field can be
 * left null to leave it unchanged (PATCH-style). The template reference is
 * double-null: pass {@code templateId = null} to clear; omit the key to leave
 * the existing reference in place. See {@link #templateIdPresent}.
 */
public record DailyPlanUpdateRequest(
    @Size(max = 4000) String targetText,
    Boolean templateIdPresent,
    UUID templateId,
    Integer templateVersion,
    @Size(max = 20_000) String notesMd,
    DailyPlanStatus status
) {
    /** Convenience constructor used by tests / callers that want "no change". */
    public static DailyPlanUpdateRequest empty() {
        return new DailyPlanUpdateRequest(null, null, null, null, null, null);
    }
}