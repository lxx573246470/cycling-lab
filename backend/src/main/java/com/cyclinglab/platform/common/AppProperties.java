package com.cyclinglab.platform.common;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cyclinglab")
public record AppProperties(Jwt jwt, Storage storage, Cors cors) {

    public record Jwt(String secret, long accessTokenTtlSeconds, long refreshTokenTtlSeconds, String issuer) {}

    public record Storage(Minio minio) {
        public record Minio(String endpoint, String accessKey, String secretKey, String bucket) {}
    }

    public record Cors(List<String> allowedOrigins) {}
}
