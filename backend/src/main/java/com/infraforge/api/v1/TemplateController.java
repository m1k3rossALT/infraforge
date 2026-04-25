package com.infraforge.api.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infraforge.engine.ProviderRegistry;
import com.infraforge.model.SaveTemplateRequest;
import com.infraforge.model.Template;
import com.infraforge.model.TemplateSummary;
import com.infraforge.service.TemplateService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private static final Logger log = LoggerFactory.getLogger(TemplateController.class);

    // File extension → provider ID mapping for import detection
    private static final Map<String, String> EXT_TO_PROVIDER = Map.of(
        "tf",          "terraform",
        "yml",         "ansible",
        "yaml",        "ansible",
        "vagrantfile", "vagrant"
    );

    private final TemplateService templateService;
    private final ProviderRegistry providerRegistry;
    private final ObjectMapper objectMapper;

    public TemplateController(TemplateService templateService,
                               ProviderRegistry providerRegistry,
                               ObjectMapper objectMapper) {
        this.templateService = templateService;
        this.providerRegistry = providerRegistry;
        this.objectMapper = objectMapper;
    }

    /** List all templates — returns summaries only (no formState) */
    @GetMapping
    public List<TemplateSummary> listAll(@RequestParam(required = false) String providerId) {
        List<Template> templates = providerId != null
                ? templateService.listByProvider(providerId)
                : templateService.listAll();
        return templates.stream().map(TemplateSummary::new).collect(Collectors.toList());
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
        Template saved = templateService.save(null, request.getName(), request.getProviderId(),
                request.getFormState(), request.getDescription(), request.getTags());
        return ResponseEntity.created(URI.create("/api/v1/templates/" + saved.getId())).body(saved);
    }

    /** Update an existing template (auto-save) */
    @PutMapping("/{id}")
    public ResponseEntity<Template> update(@PathVariable UUID id,
                                            @Valid @RequestBody SaveTemplateRequest request) {
        if (templateService.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        Template saved = templateService.save(id, request.getName(), request.getProviderId(),
                request.getFormState(), request.getDescription(), request.getTags());
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
            return ResponseEntity.created(URI.create("/api/v1/templates/" + copy.getId())).body(copy);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Export a template as a zip file.
     * The zip contains:
     *   - main.<ext>        the generated template file (raw formState serialized as JSON for now)
     *   - metadata.json     template name, provider, dates, tags
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable UUID id) {
        Optional<Template> opt = templateService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Template template = opt.get();
        String ext = providerRegistry.findById(template.getProviderId())
                .map(s -> s.getFileExtension().replace(".", ""))
                .orElse("txt");

        try {
            // Build metadata.json
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("id", template.getId().toString());
            metadata.put("name", template.getName());
            metadata.put("providerId", template.getProviderId());
            metadata.put("description", template.getDescription());
            metadata.put("tags", template.getTags());
            metadata.put("generatedAt", template.getGeneratedAt() != null
                    ? template.getGeneratedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null);
            metadata.put("exportedAt", java.time.OffsetDateTime.now()
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

            String metadataJson = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(metadata);
            String formStateJson = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(template.getFormState());

            // Build zip in memory
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(baos)) {
                // form_state as the template file (raw JSON — actual rendering is done by the UI)
                String templateFilename = sanitizeFilename(template.getName()) + "." + ext;
                zip.putNextEntry(new ZipEntry(templateFilename));
                zip.write(formStateJson.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();

                // metadata
                zip.putNextEntry(new ZipEntry("metadata.json"));
                zip.write(metadataJson.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }

            String zipFilename = sanitizeFilename(template.getName()) + ".zip";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFilename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(baos.toByteArray());

        } catch (Exception e) {
            log.error("[TemplateController] Export failed for id={}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Import a template file.
     * Detects provider from file extension, stores raw content as a new template.
     * Full form pre-fill from parsed file content is a Phase 4+ enhancement.
     */
    @PostMapping("/import")
    public ResponseEntity<Template> importTemplate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String originalFilename = file.getOriginalFilename() != null
                ? file.getOriginalFilename().toLowerCase() : "";

        // Detect provider from extension
        String detectedProvider = detectProvider(originalFilename);
        if (detectedProvider == null) {
            log.warn("[TemplateController] Import rejected — unrecognised file type: {}", originalFilename);
            return ResponseEntity.badRequest().build();
        }

        try {
            String rawContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            String templateName = name != null && !name.isBlank()
                    ? name
                    : originalFilename.isEmpty() ? "Imported template" : originalFilename;

            // Store raw content in formState under a reserved key
            // The frontend can display this as a reference alongside the form
            Map<String, Object> formState = new LinkedHashMap<>();
            formState.put("_imported", true);
            formState.put("_rawContent", rawContent);
            formState.put("_sourceFile", originalFilename);

            Template saved = templateService.save(null, templateName, detectedProvider,
                    formState, "Imported from " + originalFilename, null);

            log.info("[TemplateController] Imported '{}' as provider '{}' (id={})",
                    originalFilename, detectedProvider, saved.getId());

            return ResponseEntity.created(URI.create("/api/v1/templates/" + saved.getId()))
                    .body(saved);

        } catch (Exception e) {
            log.error("[TemplateController] Import failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String detectProvider(String filename) {
        if (filename.equals("vagrantfile")) return "vagrant";
        int dot = filename.lastIndexOf('.');
        if (dot == -1) return null;
        String ext = filename.substring(dot + 1);
        return EXT_TO_PROVIDER.get(ext);
    }

    private String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}