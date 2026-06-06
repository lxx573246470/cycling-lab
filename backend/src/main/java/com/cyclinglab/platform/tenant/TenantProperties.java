package com.cyclinglab.platform.tenant;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Marker for the current schema design where all M1 tables live in the
 * default `public` schema and are isolated by {@code user_id}. Future
 * v0.2 work may switch to per-tenant schemas; for now this record only
 * exists so that {@code @EnableConfigurationProperties} can pick it up
 * if/when we add a schema prefix.
 */
@ConfigurationProperties(prefix = "cyclinglab.tenant")
public record TenantProperties(String mode) {
    public TenantProperties {
        if (mode == null || mode.isBlank()) {
            mode = "user-id";
        }
    }
}
