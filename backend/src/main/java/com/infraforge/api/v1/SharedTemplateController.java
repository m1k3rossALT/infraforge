package com.infraforge.api.v1;

import com.infraforge.engine.ProviderRegistry;
import com.infraforge.engine.TemplateRenderer;
import com.infraforge.model.Template;
import com.infraforge.service.TemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Public endpoint for shared template views.
 *
 * No authentication required — access is controlled entirely by the share token,
 * which is a UUID and not guessable by enumeration.
 *
 * Returns only the generated code output and display metadata.
 * formState and userId are never exposed — the recipient sees the result, not
 * the internal structure.
 *
 * This controller is intentionally separate from TemplateController so that:
 *   - Security rules are explicit (/api/v1/shared/** is permitAll in SecurityConfig)
 *   - The public contract is clearly defined and won't accidentally grow
 *   - Future rate-limiting or analytics can be applied to shared views independently
 */
@RestController
@RequestMapping("/api/v1/shared")
public class SharedTemplateController {

    private static final Logger log = LoggerFactory.getLogger(SharedTemplateController.class);

    private final TemplateService templateService;
    private final TemplateRenderer templateRenderer;
    private final ProviderRegistry providerRegistry;

    public SharedTemplateController(TemplateService templateService,
                                     TemplateRenderer templateRenderer,
                                     ProviderRegistry providerRegistry) {
        this.templateService = templateService;
        this.templateRenderer = templateRenderer;
        this.providerRegistry = providerRegistry;
    }

    /**
     * Fetch a shared template by its share token.
     * Returns the rendered code output, template name, and provider — nothing else.
     *
     * 404 if: token is unknown, template has been deleted, or sharing was revoked.
     */
    @GetMapping("/{shareToken}")
    public ResponseEntity<SharedTemplateResponse> getShared(@PathVariable UUID shareToken) {
        Optional<Template> opt = templateService.findByShareToken(shareToken);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Template template = opt.get();

        // Verify provider still exists (provider could have been removed)
        if (providerRegistry.findById(template.getProviderId()).isEmpty()) {
            log.warn("[SharedTemplateController] Provider '{}' not found for shared template id={}",
                    template.getProviderId(), template.getId());
            return ResponseEntity.notFound().build();
        }

        try {
            String generatedCode = renderTemplate(template);
            return ResponseEntity.ok(new SharedTemplateResponse(
                    template.getName(),
                    template.getProviderId(),
                    generatedCode
            ));
        } catch (Exception e) {
            log.error("[SharedTemplateController] Render failed for share token {}: {}",
                    shareToken, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Build the FreeMarker model from stored formState and render the template.
     * Mirrors the model-building logic in ProviderController — both must stay in sync
     * with how formState is structured on the frontend.
     */
    @SuppressWarnings("unchecked")
    private String renderTemplate(Template template) throws Exception {
        Map<String, Object> sectionsModel = new LinkedHashMap<>();

        if (template.getFormState() != null) {
            for (Map.Entry<String, Object> entry : template.getFormState().entrySet()) {
                if (entry.getValue() instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> rawSection = (Map<String, Object>) entry.getValue();
                    Map<String, Object> sectionMap = new HashMap<>();
                    sectionMap.put("enabled", rawSection.getOrDefault("enabled", true));
                    Object instances = rawSection.get("instances");
                    sectionMap.put("instances", instances instanceof List ? instances : List.of());
                    sectionsModel.put(entry.getKey(), sectionMap);
                }
            }
        }

        Map<String, Object> model = new HashMap<>();
        model.put("sections", sectionsModel);

        return templateRenderer.render(template.getProviderId(), model);
    }

    // ─── Response DTO ────────────────────────────────────────────────────────

    /**
     * Public-safe response — no formState, no userId, no shareToken.
     * Only what the recipient needs to view the generated output.
     */
    public record SharedTemplateResponse(
            String name,
            String providerId,
            String generatedCode
    ) {}
}