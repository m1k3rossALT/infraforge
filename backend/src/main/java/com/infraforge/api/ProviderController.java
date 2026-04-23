package com.infraforge.api;

import com.infraforge.engine.ProviderRegistry;
import com.infraforge.engine.TemplateRenderer;
import com.infraforge.model.GenerateRequest;
import com.infraforge.model.ProviderSchema;
import com.infraforge.model.ProviderSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProviderController {

    private static final Logger log = LoggerFactory.getLogger(ProviderController.class);

    private final ProviderRegistry registry;
    private final TemplateRenderer renderer;

    public ProviderController(ProviderRegistry registry, TemplateRenderer renderer) {
        this.registry = registry;
        this.renderer = renderer;
    }

    /** List all registered providers — used by the frontend tab bar. */
    @GetMapping("/providers")
    public List<ProviderSummary> listProviders() {
        return registry.listSummaries();
    }

    /** Full schema for a provider — used to render the dynamic form. */
    @GetMapping("/providers/{id}/schema")
    public ResponseEntity<ProviderSchema> getSchema(@PathVariable String id) {
        return registry.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Generate a template from user-supplied field values.
     * Returns plain text — the rendered IaC file content.
     */
    @PostMapping("/providers/{id}/generate")
    public ResponseEntity<String> generate(
            @PathVariable String id,
            @RequestBody GenerateRequest request) {

        if (!registry.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }

        try {
            // Convert String values to Object map for FreeMarker data model
            Map<String, Object> model = new HashMap<>(request.getValues());
            String output = renderer.render(id, model);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .body(output);
        } catch (Exception e) {
            log.error("[ProviderController] Template rendering failed for provider '{}': {}", id, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("Template rendering error: " + e.getMessage());
        }
    }

    /**
     * Reserved endpoint for future AI-powered field suggestions.
     * Returns empty for now — frontend should handle this gracefully.
     */
    @PostMapping("/providers/{id}/suggest")
    public ResponseEntity<Map<String, String>> suggest(@PathVariable String id) {
        return ResponseEntity.ok(Map.of());
    }

    /** Standard health check. */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
            "status", "ok",
            "version", "0.1.0"
        );
    }
}
