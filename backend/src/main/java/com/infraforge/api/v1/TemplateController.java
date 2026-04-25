package com.infraforge.api.v1;

import com.infraforge.model.SaveTemplateRequest;
import com.infraforge.model.Template;
import com.infraforge.model.TemplateSummary;
import com.infraforge.service.TemplateService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST API v1 — Template management.
 * Versioned under /api/v1/ to allow non-breaking evolution.
 */
@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private static final Logger log = LoggerFactory.getLogger(TemplateController.class);

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    /** List all templates — returns summaries only (no formState) */
    @GetMapping
    public List<TemplateSummary> listAll(
            @RequestParam(required = false) String providerId) {

        List<Template> templates = providerId != null
                ? templateService.listByProvider(providerId)
                : templateService.listAll();

        return templates.stream()
                .map(TemplateSummary::new)
                .collect(Collectors.toList());
    }

    /** Load a single template — includes full formState */
    @GetMapping("/{id}")
    public ResponseEntity<Template> getById(@PathVariable UUID id) {
        return templateService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Create a new template */
    @PostMapping
    public ResponseEntity<Template> create(@Valid @RequestBody SaveTemplateRequest request) {
        Template saved = templateService.save(
                null,
                request.getName(),
                request.getProviderId(),
                request.getFormState(),
                request.getDescription(),
                request.getTags()
        );
        return ResponseEntity
                .created(URI.create("/api/v1/templates/" + saved.getId()))
                .body(saved);
    }

    /** Update an existing template (used by auto-save) */
    @PutMapping("/{id}")
    public ResponseEntity<Template> update(
            @PathVariable UUID id,
            @Valid @RequestBody SaveTemplateRequest request) {

        if (templateService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Template saved = templateService.save(
                id,
                request.getName(),
                request.getProviderId(),
                request.getFormState(),
                request.getDescription(),
                request.getTags()
        );
        return ResponseEntity.ok(saved);
    }

    /** Delete a template */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return templateService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    /** Duplicate a template */
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<Template> duplicate(@PathVariable UUID id) {
        try {
            Template copy = templateService.duplicate(id);
            return ResponseEntity
                    .created(URI.create("/api/v1/templates/" + copy.getId()))
                    .body(copy);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}