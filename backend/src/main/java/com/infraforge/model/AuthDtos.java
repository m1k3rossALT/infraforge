package com.infraforge.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Auth request/response DTOs.
 * Kept in one file since they are small and tightly related.
 */
public final class AuthDtos {

    private AuthDtos() {}

    // ─── Requests ─────────────────────────────────────────────────────────────

    public static class RegisterRequest {
        @Email(message = "Must be a valid email address")
        @NotBlank(message = "Email is required")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RefreshRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    // ─── Responses ────────────────────────────────────────────────────────────

    public static class AuthResponse {
        private final String accessToken;
        private final String refreshToken;
        private final UserInfo user;

        public AuthResponse(String accessToken, String refreshToken, UserInfo user) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.user = user;
        }

        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public UserInfo getUser() { return user; }
    }

    public static class UserInfo {
        private final String id;
        private final String email;
        private final String role;

        public UserInfo(User user) {
            this.id = user.getId().toString();
            this.email = user.getEmail();
            this.role = user.getRole().name();
        }

        public String getId() { return id; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
    }
}