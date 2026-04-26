package com.infraforge.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Helper to access the current authenticated user in any layer.
 *
 * Usage in a controller:
 *   Optional<UUID> userId = CurrentUser.id();
 *   boolean isAuth = CurrentUser.isAuthenticated();
 *
 * Abstracts the SecurityContext so if the auth mechanism changes (e.g. OAuth2),
 * only this class needs updating — not every controller that reads the user.
 */
public final class CurrentUser {

    private CurrentUser() {}

    public static Optional<UUID> id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return Optional.empty();
        }
        try {
            return Optional.of((UUID) auth.getPrincipal());
        } catch (ClassCastException e) {
            return Optional.empty();
        }
    }

    public static boolean isAuthenticated() {
        return id().isPresent();
    }

    public static UUID idOrThrow() {
        return id().orElseThrow(() -> new IllegalStateException("No authenticated user in context"));
    }
}