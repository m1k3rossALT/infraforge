package com.infraforge.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * JWT access token and refresh token utilities.
 *
 * Access tokens  — short-lived JWTs (15 min), carry userId and role as claims.
 * Refresh tokens — random 256-bit strings, stored as SHA-256 hashes in the DB.
 *
 * The JWT secret must be at least 256 bits. Set via INFRAFORGE_JWT_SECRET env var.
 * Default is provided for development only — always override in production.
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${infraforge.jwt.secret:dev-secret-change-this-in-production-must-be-32-chars}")
    private String jwtSecret;

    @Value("${infraforge.jwt.access-token-expiry-ms:900000}") // 15 minutes
    private long accessTokenExpiryMs;

    @Value("${infraforge.jwt.refresh-token-expiry-days:7}")
    private int refreshTokenExpiryDays;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // ─── Access token ────────────────────────────────────────────────────────

    public String generateAccessToken(UUID userId, String email, String role) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiryMs))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims validateAccessToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(validateAccessToken(token).getSubject());
    }

    public String extractRole(String token) {
        return validateAccessToken(token).get("role", String.class);
    }

    // ─── Refresh token ───────────────────────────────────────────────────────

    /** Generate a cryptographically secure random refresh token */
    public String generateRefreshToken() {
        byte[] bytes = new byte[32]; // 256 bits
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Hash a refresh token for safe storage in the database */
    public String hashRefreshToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash refresh token", e);
        }
    }

    public long getRefreshTokenExpiryDays() {
        return refreshTokenExpiryDays;
    }
}