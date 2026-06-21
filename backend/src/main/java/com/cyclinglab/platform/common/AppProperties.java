package com.cyclinglab.platform.common;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cyclinglab")
public record AppProperties(Jwt jwt, Auth auth, Storage storage, Content content, Cors cors) {

    public record Jwt(String secret, long accessTokenTtlSeconds, long refreshTokenTtlSeconds, String issuer) {}

    public record Auth(boolean disabled, LocalUser localUser) {
        public record LocalUser(String email, String displayName) {}
    }

    public record Storage(Minio minio, Local local) {
        public record Minio(String endpoint, String accessKey, String secretKey, String bucket) {}
        public record Local(String root) {}
    }

    public record Content(String root) {}

    public record Cors(List<String> allowedOrigins) {}
}
