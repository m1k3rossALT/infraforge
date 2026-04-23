package com.infraforge.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infraforge.model.ProviderSchema;
import com.infraforge.model.ProviderSummary;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Scans classpath:/providers/ at startup and registers every provider that has
 * a valid schema.json.  Adding a new provider requires no code changes — drop a
 * folder with schema.json + template.ftl and restart.
 */
@Component
public class ProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(ProviderRegistry.class);

    private final ObjectMapper objectMapper;
    // Insertion-ordered map so provider tabs appear in a consistent order
    private final Map<String, ProviderSchema> registry = new LinkedHashMap<>();

    public ProviderRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] schemas = resolver.getResources("classpath:providers/*/schema.json");

        for (Resource schemaResource : schemas) {
            try {
                ProviderSchema schema = objectMapper.readValue(schemaResource.getInputStream(), ProviderSchema.class);
                registry.put(schema.getId(), schema);
                log.info("[ProviderRegistry] Registered provider: {}", schema.getId());
            } catch (Exception e) {
                log.error("[ProviderRegistry] Failed to load schema from {}: {}", schemaResource.getDescription(), e.getMessage());
            }
        }

        log.info("[ProviderRegistry] {} provider(s) loaded: {}", registry.size(), registry.keySet());
    }

    public List<ProviderSummary> listSummaries() {
        List<ProviderSummary> summaries = new ArrayList<>();
        registry.values().forEach(s -> summaries.add(new ProviderSummary(s.getId(), s.getLabel())));
        return summaries;
    }

    public Optional<ProviderSchema> findById(String id) {
        return Optional.ofNullable(registry.get(id));
    }
}
