package com.infraforge.api.v1;

import com.infraforge.model.AuthDtos;
import com.infraforge.service.AuthService;
import com.infraforge.security.CurrentUser;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication endpoints — all public (no auth required).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        try {
            AuthDtos.AuthResponse response = authService.register(
                    request.getEmail(), request.getPassword());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        try {
            AuthDtos.AuthResponse response = authService.login(
                    request.getEmail(), request.getPassword());
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            // Never reveal whether email or password was wrong
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Invalid email or password"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        try {
            AuthDtos.AuthResponse response = authService.refresh(request.getRefreshToken());
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Invalid or expired refresh token"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) AuthDtos.RefreshRequest request) {
        if (request != null && request.getRefreshToken() != null) {
            authService.logout(request.getRefreshToken());
        }
        return ResponseEntity.noContent().build();
    }

    /** Logout from all devices — revokes all refresh tokens for the user */
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll() {
        CurrentUser.id().ifPresent(authService::logoutAll);
        return ResponseEntity.noContent().build();
    }

    /** Return current user info — useful for frontend to verify token on page load */
    @GetMapping("/me")
    public ResponseEntity<?> me() {
        return CurrentUser.id()
                .map(id -> ResponseEntity.ok(Map.of("authenticated", true, "userId", id.toString())))
                .orElse(ResponseEntity.ok(Map.of("authenticated", false)));
    }
}