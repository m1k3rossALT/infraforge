package com.infraforge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration.
 * Allowed origins are driven by the INFRAFORGE_CORS_ORIGINS environment variable.
 * Default: localhost:3000 (Docker) and localhost:5173 (Vite dev server).
 * In production set: INFRAFORGE_CORS_ORIGINS=https://your-domain.com
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${infraforge.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "DELETE")
                .allowedHeaders("Content-Type", "Authorization", "X-Request-ID")
                .maxAge(3600);
    }
}
