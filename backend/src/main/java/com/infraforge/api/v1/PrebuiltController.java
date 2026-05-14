package com.infraforge.api.v1;

import com.infraforge.model.PrebuiltTemplate;
import com.infraforge.model.Template;
import com.infraforge.security.CurrentUser;
import com.infraforge.service.PrebuiltTemplateLoader;
import com.infraforge.service.TemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Prebuilt template endpoints.
 *
 * Public (no auth):
 *   GET  /api/v1/prebuilts        — list all prebuilts
 *   GET  /api/v1/prebuilts/{id}   — get a single prebuilt
 *
 * Auth required:
 *   POST /api/v1/prebuilts/{id}/fork — copy into the user's personal library
 *
 * Security note: browse/view endpoints are fully public — no auth needed to
 * explore prebuilts. The fork endpoint checks CurrentUser explicitly and
 * returns 401 if unauthenticated, since the SecurityConfig permits the whole
 * /api/v1/prebuilts/** path. This keeps browsing open while protecting writes.
 */
@RestController
@RequestMapping("/api/v1/prebuilts")
public class PrebuiltController {

    private static final Logger log = LoggerFactory.getLogger(PrebuiltController.class);

    private final PrebuiltTemplateLoader loader;
    private final TemplateService templateService;

    public PrebuiltController(PrebuiltTemplateLoader loader, TemplateService templateService) {
        this.loader = loader;
        this.templateService = templateService;
    }

    /** List all prebuilt templates — public, no auth required. */
    @GetMapping
    public List<PrebuiltTemplate> listAll(
            @RequestParam(required = false) String providerId) {

        List<PrebuiltTemplate> all = loader.findAll();
        if (providerId != null && !providerId.isBlank()) {
            return all.stream()
                .filter(p -> providerId.equalsIgnoreCase(p.getProviderId()))
                .toList();
        }
        return all;
    }

    /** Get a single prebuilt by ID — public, no auth required. */
    @GetMapping("/{id}")
    public ResponseEntity<PrebuiltTemplate> getById(@PathVariable String id) {
        return loader.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Fork a prebuilt into the user's personal template library.
     * Creates a normal saved Template owned by the current user.
     * The user can then edit and save it like any other template.
     *
     * Returns 401 if not authenticated.
     * Returns 404 if the prebuilt ID does not exist.
     */
    @PostMapping("/{id}/fork")
    public ResponseEntity<?> fork(@PathVariable String id) {
        Optional<UUID> userIdOpt = CurrentUser.id();
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(401)
                .body(Map.of("message", "Sign in to fork templates"));
        }

        Optional<PrebuiltTemplate> prebuiltOpt = loader.findById(id);
        if (prebuiltOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PrebuiltTemplate prebuilt = prebuiltOpt.get();
        UUID userId = userIdOpt.get();

        Template forked = templateService.save(
            null,
            prebuilt.getName(),
            prebuilt.getProviderId(),
            prebuilt.getFormState(),
            prebuilt.getDescription(),
            prebuilt.getTags(),
            userId
        );

        log.info("[PrebuiltController] Forked prebuilt '{}' for user={} -> template id={}",
            id, userId, forked.getId());

        return ResponseEntity.created(
            URI.create("/api/v1/templates/" + forked.getId())
        ).body(forked);
    }
}
