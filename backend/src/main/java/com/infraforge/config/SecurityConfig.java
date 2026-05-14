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
 * Public endpoints:
 *   - /api/v1/auth/**       — register, login, refresh, logout
 *   - /api/v1/providers/**  — list providers, schema, generate (guest mode)
 *   - /api/v1/shared/**     — public read-only shared template views
 *   - /actuator/health|info — health checks
 *
 * Protected endpoints (require valid JWT):
 *   - /api/v1/templates/**  — template management
 *   - /api/v1/ai/**         — AI suggestions and BYOK settings
 *   - /api/v1/**            — all other v1 endpoints default to authenticated
 *
 * Phase 6 note: when Stripe webhooks are added, add /api/v1/webhooks/stripe to permitAll
 * (webhook signature verification handles security — not JWT).
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
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/providers/**").permitAll()
                .requestMatchers("/api/v1/shared/**").permitAll()
                .requestMatchers("/api/v1/health", "/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/v1/templates/**").authenticated()
                .requestMatchers("/api/v1/ai/**").authenticated()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().permitAll()
            )
            .exceptionHandling(ex -> ex
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
        return new BCryptPasswordEncoder(12);
    }
}
