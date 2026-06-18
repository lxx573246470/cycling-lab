package com.cyclinglab.platform.auth;

import com.cyclinglab.platform.common.AppProperties;
import com.cyclinglab.platform.tenant.TenantContext;
import com.cyclinglab.platform.user.UserEntity;
import com.cyclinglab.platform.user.UserRepository;
import com.cyclinglab.platform.user.UserRole;
import com.cyclinglab.platform.user.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class LocalAuthFilter extends OncePerRequestFilter {

    private final AppProperties appProperties;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain chain
    ) throws ServletException, IOException {
        if (!localAuthDisabled()) {
            chain.doFilter(request, response);
            return;
        }

        UserEntity user = localUser();
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        var authToken = new UsernamePasswordAuthenticationToken(user, null, authorities);
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
        TenantContext.setCurrentUserId(user.getId());

        chain.doFilter(request, response);
    }

    private boolean localAuthDisabled() {
        return appProperties.auth() != null && appProperties.auth().disabled();
    }

    private synchronized UserEntity localUser() {
        String email = configuredEmail();
        return userRepository.findByEmail(email).map(existing -> {
            boolean changed = false;
            if (existing.getStatus() != UserStatus.ACTIVE) {
                existing.setStatus(UserStatus.ACTIVE);
                changed = true;
            }
            if (existing.getRole() != UserRole.ADMIN) {
                existing.setRole(UserRole.ADMIN);
                changed = true;
            }
            String displayName = configuredDisplayName();
            if (!displayName.equals(existing.getDisplayName())) {
                existing.setDisplayName(displayName);
                changed = true;
            }
            return changed ? userRepository.save(existing) : existing;
        }).orElseGet(() -> {
            UserEntity user = new UserEntity();
            user.setEmail(email);
            user.setDisplayName(configuredDisplayName());
            user.setPasswordHash("{noop}local-auth-disabled");
            user.setRole(UserRole.ADMIN);
            user.setStatus(UserStatus.ACTIVE);
            return userRepository.save(user);
        });
    }

    private String configuredEmail() {
        AppProperties.Auth.LocalUser localUser = appProperties.auth() == null ? null : appProperties.auth().localUser();
        String email = localUser == null ? null : localUser.email();
        return (email == null || email.isBlank()) ? "local@cycling-lab.local" : email.trim().toLowerCase();
    }

    private String configuredDisplayName() {
        AppProperties.Auth.LocalUser localUser = appProperties.auth() == null ? null : appProperties.auth().localUser();
        String displayName = localUser == null ? null : localUser.displayName();
        return (displayName == null || displayName.isBlank()) ? "Local Rider" : displayName.trim();
    }
}
