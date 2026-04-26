package com.infraforge.service;

import com.infraforge.model.*;
import com.infraforge.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Authentication business logic.
 *
 * Extensibility:
 *   - Add email verification by adding a verified flag to User and sending
 *     a verification email here before allowing login
 *   - Add OAuth2 by adding a new method that creates/finds a user by provider ID
 *   - Add MFA by adding a second factor check step in login()
 */
@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthDtos.AuthResponse register(String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        User user = new User();
        user.setEmail(email.toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(User.Role.EDITOR);
        userRepository.save(user);

        log.info("[AuthService] Registered new user: {}", user.getEmail());
        return issueTokens(user);
    }

    public AuthDtos.AuthResponse login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new BadCredentialsException("Account is disabled");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        log.info("[AuthService] Login: {}", user.getEmail());
        return issueTokens(user);
    }

    public AuthDtos.AuthResponse refresh(String rawRefreshToken) {
        String tokenHash = jwtUtil.hashRefreshToken(rawRefreshToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (!stored.isValid()) {
            throw new BadCredentialsException("Refresh token has expired or been revoked");
        }

        // Rotate — revoke old token and issue a new one
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        log.debug("[AuthService] Rotated refresh token for user: {}", stored.getUser().getId());
        return issueTokens(stored.getUser());
    }

    public void logout(String rawRefreshToken) {
        String tokenHash = jwtUtil.hashRefreshToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("[AuthService] Logout for user: {}", token.getUser().getId());
        });
    }

    public void logoutAll(java.util.UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("[AuthService] All sessions revoked for user: {}", userId);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private AuthDtos.AuthResponse issueTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());

        String rawRefreshToken = jwtUtil.generateRefreshToken();
        String tokenHash = jwtUtil.hashRefreshToken(rawRefreshToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiresAt(
                OffsetDateTime.now().plusDays(jwtUtil.getRefreshTokenExpiryDays()));
        refreshTokenRepository.save(refreshToken);

        return new AuthDtos.AuthResponse(
                accessToken,
                rawRefreshToken,
                new AuthDtos.UserInfo(user));
    }
}