package com.cyclinglab.platform.tenant;

import java.util.UUID;

/**
 * Holds the current authenticated user's id for the duration of a request.
 * Populated by JwtAuthFilter, consumed by Hibernate filter enabling logic and
 * controllers that need a typed principal without going through the
 * SecurityContext.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setCurrentUserId(UUID userId) {
        CURRENT.set(userId);
    }

    public static UUID getCurrentUserId() {
        UUID id = CURRENT.get();
        if (id == null) {
            throw new IllegalStateException("No current user in TenantContext (request not authenticated?)");
        }
        return id;
    }

    public static UUID getCurrentUserIdOrNull() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
