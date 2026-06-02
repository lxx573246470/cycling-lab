package com.cyclinglab.platform.auth;

import com.cyclinglab.platform.user.UserEntity;
import com.cyclinglab.platform.user.UserRepository;
import com.cyclinglab.platform.user.UserRole;
import com.cyclinglab.platform.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new AuthException("Email already registered");
        }
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setDisplayName(req.displayName().trim());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);
        return buildTokenResponse(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        UserEntity user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AuthException("Invalid email or password"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthException("Account is disabled");
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new AuthException("Invalid email or password");
        }
        return buildTokenResponse(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshRequest req) {
        String token = req.refreshToken();
        try {
            String type = jwtService.extractTokenType(token);
            if (!"refresh".equals(type)) {
                throw new AuthException("Invalid refresh token");
            }
            var userId = jwtService.extractUserId(token);
            UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new AuthException("Account is disabled");
            }
            return buildTokenResponse(user);
        } catch (AuthException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AuthException("Invalid refresh token");
        }
    }

    private TokenResponse buildTokenResponse(UserEntity user) {
        String access = jwtService.issueAccessToken(user);
        String refresh = jwtService.issueRefreshToken(user);
        return new TokenResponse(
            access,
            refresh,
            jwtService.getAccessTtlSeconds(),
            new TokenResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name()
            )
        );
    }
}
