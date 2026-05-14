package com.infraforge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infraforge.model.PrebuiltTemplate;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Loads prebuilt templates from JSON files at application startup.
 *
 * Scanning pattern: classpath:prebuilts/*.json
 * This works both in exploded class directories (local dev) and inside a JAR
 * (Docker production) because PathMatchingResourcePatternResolver handles both.
 *
 * Adding a new prebuilt:
 *   1. Create backend/src/main/resources/prebuilts/{id}.json
 *   2. Restart the backend
 *   No code changes required.
 *
 * The loaded map is keyed by prebuilt ID for O(1) lookup by the controller.
 * Templates are stored in insertion order (alphabetical by filename) for
 * consistent ordering in list responses.
 */
@Service
public class PrebuiltTemplateLoader {

    private static final Logger log = LoggerFactory.getLogger(PrebuiltTemplateLoader.class);

    private final ObjectMapper objectMapper;
    private final Map<String, PrebuiltTemplate> prebuilts = new LinkedHashMap<>();

    public PrebuiltTemplateLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:prebuilts/*.json");

            for (Resource resource : resources) {
                try {
                    PrebuiltTemplate template = objectMapper.readValue(
                        resource.getInputStream(), PrebuiltTemplate.class
                    );
                    if (template.getId() == null || template.getId().isBlank()) {
                        log.warn("[PrebuiltTemplateLoader] Skipping {} — missing id field",
                            resource.getFilename());
                        continue;
                    }
                    prebuilts.put(template.getId(), template);
                    log.info("[PrebuiltTemplateLoader] Loaded prebuilt '{}' ({})",
                        template.getName(), template.getId());
                } catch (Exception e) {
                    log.error("[PrebuiltTemplateLoader] Failed to load {}: {}",
                        resource.getFilename(), e.getMessage());
                }
            }

            log.info("[PrebuiltTemplateLoader] {} prebuilt templates loaded", prebuilts.size());

        } catch (Exception e) {
            log.warn("[PrebuiltTemplateLoader] No prebuilts directory found or empty: {}", e.getMessage());
        }
    }

    public List<PrebuiltTemplate> findAll() {
        return List.copyOf(prebuilts.values());
    }

    public Optional<PrebuiltTemplate> findById(String id) {
        return Optional.ofNullable(prebuilts.get(id));
    }
}