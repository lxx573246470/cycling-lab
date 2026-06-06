package com.cyclinglab.platform.admin;

import com.cyclinglab.platform.admin.dto.AdminUserDto;
import com.cyclinglab.platform.admin.exception.AdminUserNotFoundException;
import com.cyclinglab.platform.library.dto.PageResponse;
import com.cyclinglab.platform.tenant.TenantContext;
import com.cyclinglab.platform.user.UserEntity;
import com.cyclinglab.platform.user.UserRepository;
import com.cyclinglab.platform.user.UserRole;
import com.cyclinglab.platform.user.UserStatus;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin-only user management. {@link com.cyclinglab.platform.auth.SecurityConfig}
 * gates {@code /api/v1/admin/**} on {@code ROLE_ADMIN} so by the time we get
 * here we know the caller is elevated.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserDto> list(String q, String role, String status, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(
            safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<UserEntity> all = userRepo.findAll(pageable);
        List<AdminUserDto> items = all.getContent().stream()
            .filter(u -> matchesText(u, q))
            .filter(u -> matchesRole(u, role))
            .filter(u -> matchesStatus(u, status))
            .map(this::toDto)
            .toList();
        return new PageResponse<>(items, safePage, safeSize, all.getTotalElements(), all.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminUserDto get(UUID id) {
        return toDto(loadOwned(id));
    }

    @Transactional
    public AdminUserDto patch(UUID id, UserRole role, UserStatus status) {
        UserEntity u = loadOwned(id);
        if (role != null) u.setRole(role);
        if (status != null) u.setStatus(status);
        return toDto(userRepo.save(u));
    }

    @Transactional
    public void delete(UUID id) {
        UserEntity u = loadOwned(id);
        if (u.getId().equals(TenantContext.getCurrentUserIdOrNull())) {
            throw new IllegalArgumentException("admin cannot delete their own account");
        }
        userRepo.delete(u);
    }

    private UserEntity loadOwned(UUID id) {
        return userRepo.findById(id)
            .orElseThrow(() -> new AdminUserNotFoundException(id));
    }

    private boolean matchesText(UserEntity u, String q) {
        if (q == null || q.isBlank()) return true;
        String needle = q.toLowerCase(Locale.ROOT);
        return u.getEmail().toLowerCase(Locale.ROOT).contains(needle)
            || u.getDisplayName().toLowerCase(Locale.ROOT).contains(needle);
    }

    private boolean matchesRole(UserEntity u, String role) {
        if (role == null || role.isBlank()) return true;
        try {
            return u.getRole() == UserRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean matchesStatus(UserEntity u, String status) {
        if (status == null || status.isBlank()) return true;
        try {
            return u.getStatus() == UserStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private AdminUserDto toDto(UserEntity u) {
        return new AdminUserDto(
            u.getId(),
            u.getEmail(),
            u.getDisplayName(),
            u.getRole(),
            u.getStatus(),
            u.getCreatedAt(),
            u.getUpdatedAt()
        );
    }
}