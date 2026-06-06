package com.cyclinglab.platform.admin.dto;

import com.cyclinglab.platform.user.UserRole;
import com.cyclinglab.platform.user.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record AdminUserDto(
    UUID id,
    String email,
    String displayName,
    UserRole role,
    UserStatus status,
    Instant createdAt,
    Instant updatedAt
) {}