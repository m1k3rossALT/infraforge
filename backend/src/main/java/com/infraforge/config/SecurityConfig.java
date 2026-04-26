package com.infraforge.config;

import com.infraforge.security.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Security configuration.
 *
 * Design decisions:
 *   - Stateless session (JWT) — no server-side session state
 *   - CSRF disabled — not needed for stateless JWT APIs
 *   - Anonymous access to provider and generate endpoints — supports guest mode
 *   - Shared template endpoint is fully public — access gated by unguessable UUID token
 *   - All template management endpoints require authentication
 *   - Auth endpoints (register/login/refresh) are public
 *
 * To add OAuth2 in the future: add .oauth2ResourceServer() config here
 * and update JwtAuthFilter to handle OAuth2 tokens.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, ObjectMapper objectMapper) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public — auth endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                // Public — provider endpoints (guest mode: anyone can generate templates)
                .requestMatchers("/api/v1/providers/**").permitAll()
                // Public — shared template views (access controlled by unguessable UUID token)
                .requestMatchers("/api/v1/shared/**").permitAll()
                // Public — health check
                .requestMatchers("/api/v1/health", "/actuator/health", "/actuator/info").permitAll()
                // Protected — template management requires login
                .requestMatchers("/api/v1/templates/**").authenticated()
                // All other /api/v1/** requires auth by default
                .requestMatchers("/api/v1/**").authenticated()
                // Everything else is open (static frontend assets)
                .anyRequest().permitAll()
            )
            .exceptionHandling(ex -> ex
                // Return JSON 401 instead of redirect to login page
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("status", 401);
                    body.put("error", "Unauthorized");
                    body.put("message", "Authentication required");
                    body.put("path", request.getRequestURI());
                    body.put("timestamp", Instant.now().toString());
                    response.getWriter().write(objectMapper.writeValueAsString(body));
                })
                // Return JSON 403 instead of default page
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("status", 403);
                    body.put("error", "Forbidden");
                    body.put("message", "Insufficient permissions");
                    body.put("path", request.getRequestURI());
                    body.put("timestamp", Instant.now().toString());
                    response.getWriter().write(objectMapper.writeValueAsString(body));
                })
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Cost factor 12 — recommended for 2024+ hardware
        return new BCryptPasswordEncoder(12);
    }
}