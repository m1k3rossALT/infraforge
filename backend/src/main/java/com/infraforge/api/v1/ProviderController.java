package com.infraforge.api.v1;

import com.infraforge.engine.ProviderRegistry;
import com.infraforge.engine.TemplateRenderer;
import com.infraforge.model.GenerateRequest;
import com.infraforge.model.ProviderSchema;
import com.infraforge.model.ProviderSummary;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider API — v1.
 * Migrated from /api/ to /api/v1/ as part of Phase 3e API versioning.
 */
@RestController
@RequestMapping("/api/v1")
public class ProviderController {

    private static final Logger log = LoggerFactory.getLogger(ProviderController.class);

    private final ProviderRegistry registry;
    private final TemplateRenderer renderer;

    public ProviderController(ProviderRegistry registry, TemplateRenderer renderer) {
        this.registry = registry;
        this.renderer = renderer;
    }

    @GetMapping("/providers")
    public List<ProviderSummary> listProviders() {
        return registry.listSummaries();
    }

    @GetMapping("/providers/{id}/schema")
    public ResponseEntity<ProviderSchema> getSchema(@PathVariable String id) {
        return registry.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/providers/{id}/generate")
    public ResponseEntity<String> generate(
            @PathVariable String id,
            @Valid @RequestBody GenerateRequest request) {

        if (registry.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Map<String, Object> model = new HashMap<>();
            Map<String, Object> sectionsModel = new LinkedHashMap<>();

            if (request.getSections() != null) {
                for (Map.Entry<String, GenerateRequest.SectionData> entry : request.getSections().entrySet()) {
                    Map<String, Object> sectionMap = new HashMap<>();
                    sectionMap.put("enabled", entry.getValue().isEnabled());
                    sectionMap.put("instances", entry.getValue().getInstances() != null
                            ? entry.getValue().getInstances()
                            : List.of());
                    sectionsModel.put(entry.getKey(), sectionMap);
                }
            }
            model.put("sections", sectionsModel);

            String output = renderer.render(id, model);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .body(output);

        } catch (Exception e) {
            log.error("[ProviderController] Render failed for provider '{}': {}", id, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("Template rendering error: " + e.getMessage());
        }
    }

    /** Reserved — AI field suggestions. No-op until Phase 4. */
    @PostMapping("/providers/{id}/suggest")
    public ResponseEntity<Map<String, String>> suggest(@PathVariable String id) {
        return ResponseEntity.ok(Map.of());
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "version", "0.3.0");
    }
}