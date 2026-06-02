package com.cyclinglab.platform.auth;

import com.cyclinglab.platform.user.UserEntity;
import com.cyclinglab.platform.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${cyclinglab.jwt.secret}")
    private String secret;

    @Value("${cyclinglab.jwt.access-token-ttl-seconds}")
    private long accessTtlSeconds;

    @Value("${cyclinglab.jwt.refresh-token-ttl-seconds}")
    private long refreshTtlSeconds;

    @Value("${cyclinglab.jwt.issuer}")
    private String issuer;

    private SecretKey key;

    @PostConstruct
    void init() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                "cyclinglab.jwt.secret must be at least 32 bytes (256 bits) for HS256");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String issueAccessToken(UserEntity user) {
        return buildToken(user, "access", accessTtlSeconds);
    }

    public String issueRefreshToken(UserEntity user) {
        return buildToken(user, "refresh", refreshTtlSeconds);
    }

    public long getAccessTtlSeconds() {
        return accessTtlSeconds;
    }

    public long getRefreshTtlSeconds() {
        return refreshTtlSeconds;
    }

    public UUID extractUserId(String token) {
        Claims claims = parseClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    public String extractTokenType(String token) {
        return parseClaims(token).get("typ", String.class);
    }

    private String buildToken(UserEntity user, String type, long ttlSeconds) {
        Instant now = Instant.now();
        Instant exp = now.plus(Duration.ofSeconds(ttlSeconds));
        return Jwts.builder()
            .issuer(issuer)
            .subject(user.getId().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .claims(Map.of(
                "typ", type,
                "role", user.getRole().name(),
                "email", user.getEmail()
            ))
            .signWith(key, Jwts.SIG.HS256)
            .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .requireIssuer(issuer)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public UserRole extractRole(String token) {
        return UserRole.valueOf(parseClaims(token).get("role", String.class));
    }
}
