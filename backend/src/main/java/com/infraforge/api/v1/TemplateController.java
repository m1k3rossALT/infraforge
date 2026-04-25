package com.infraforge.api.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infraforge.engine.ProviderRegistry;
import com.infraforge.model.SaveTemplateRequest;
import com.infraforge.model.Template;
import com.infraforge.model.TemplateSummary;
import com.infraforge.security.CurrentUser;
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

    @GetMapping
    public List<TemplateSummary> listAll(@RequestParam(required = false) String providerId) {
        UUID userId = CurrentUser.id().orElse(null);
        List<Template> templates = providerId != null
                ? templateService.listByProvider(providerId, userId)
                : templateService.listAll(userId);
        return templates.stream().map(TemplateSummary::new).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Template> getById(@PathVariable UUID id) {
        return templateService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Template> create(@Valid @RequestBody SaveTemplateRequest request) {
        UUID userId = CurrentUser.id().orElse(null);
        Template saved = templateService.save(null, request.getName(), request.getProviderId(),
                request.getFormState(), request.getDescription(), request.getTags(), userId);
        return ResponseEntity.created(URI.create("/api/v1/templates/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Template> update(@PathVariable UUID id,
                                            @Valid @RequestBody SaveTemplateRequest request) {
        if (templateService.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        UUID userId = CurrentUser.id().orElse(null);
        Template saved = templateService.save(id, request.getName(), request.getProviderId(),
                request.getFormState(), request.getDescription(), request.getTags(), userId);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID userId = CurrentUser.id().orElse(null);
        try {
            return templateService.delete(id, userId)
                    ? ResponseEntity.noContent().build()
                    : ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<Template> duplicate(@PathVariable UUID id) {
        UUID userId = CurrentUser.id().orElse(null);
        try {
            Template copy = templateService.duplicate(id, userId);
            return ResponseEntity.created(URI.create("/api/v1/templates/" + copy.getId())).body(copy);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable UUID id) {
        Optional<Template> opt = templateService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Template template = opt.get();
        String ext = providerRegistry.findById(template.getProviderId())
                .map(s -> s.getFileExtension().replace(".", ""))
                .orElse("txt");

        try {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("id", template.getId().toString());
            metadata.put("name", template.getName());
            metadata.put("providerId", template.getProviderId());
            metadata.put("description", template.getDescription());
            metadata.put("tags", template.getTags());
            metadata.put("exportedAt", java.time.OffsetDateTime.now()
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(baos)) {
                zip.putNextEntry(new ZipEntry(sanitizeFilename(template.getName()) + "." + ext));
                zip.write(objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsBytes(template.getFormState()));
                zip.closeEntry();

                zip.putNextEntry(new ZipEntry("metadata.json"));
                zip.write(objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsBytes(metadata));
                zip.closeEntry();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + sanitizeFilename(template.getName()) + ".zip\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(baos.toByteArray());

        } catch (Exception e) {
            log.error("[TemplateController] Export failed for id={}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/import")
    public ResponseEntity<Template> importTemplate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name) {

        if (file.isEmpty()) return ResponseEntity.badRequest().build();

        String originalFilename = Optional.ofNullable(file.getOriginalFilename())
                .map(String::toLowerCase).orElse("");
        String detectedProvider = detectProvider(originalFilename);
        if (detectedProvider == null) return ResponseEntity.badRequest().build();

        try {
            String rawContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            String templateName = (name != null && !name.isBlank()) ? name : originalFilename;

            Map<String, Object> formState = new LinkedHashMap<>();
            formState.put("_imported", true);
            formState.put("_rawContent", rawContent);
            formState.put("_sourceFile", originalFilename);

            UUID userId = CurrentUser.id().orElse(null);
            Template saved = templateService.save(null, templateName, detectedProvider,
                    formState, "Imported from " + originalFilename, null, userId);

            return ResponseEntity.created(URI.create("/api/v1/templates/" + saved.getId()))
                    .body(saved);
        } catch (Exception e) {
            log.error("[TemplateController] Import failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    private String detectProvider(String filename) {
        if (filename.equals("vagrantfile")) return "vagrant";
        int dot = filename.lastIndexOf('.');
        if (dot == -1) return null;
        return EXT_TO_PROVIDER.get(filename.substring(dot + 1));
    }

    private String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}