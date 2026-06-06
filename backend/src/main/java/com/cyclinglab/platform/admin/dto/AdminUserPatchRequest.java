package com.cyclinglab.platform.admin.dto;

import com.cyclinglab.platform.user.UserRole;
import com.cyclinglab.platform.user.UserStatus;
import jakarta.validation.constraints.NotNull;

public record AdminUserPatchRequest(
    @NotNull UserRole role,
    @NotNull UserStatus status
) {}