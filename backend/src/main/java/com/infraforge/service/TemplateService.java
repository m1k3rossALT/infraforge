package com.infraforge.service;

import com.infraforge.model.Template;
import com.infraforge.model.TemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Business logic layer for saved templates.
 * Controllers call this — never the repository directly.
 * Future enhancements (e.g. user scoping, search, versioning) go here.
 */
@Service
@Transactional
public class TemplateService {

    private static final Logger log = LoggerFactory.getLogger(TemplateService.class);

    private final TemplateRepository repository;

    public TemplateService(TemplateRepository repository) {
        this.repository = repository;
    }

    /** List all templates, newest first */
    @Transactional(readOnly = true)
    public List<Template> listAll() {
        return repository.findAllByOrderByUpdatedAtDesc();
    }

    /** List templates for a specific provider, newest first */
    @Transactional(readOnly = true)
    public List<Template> listByProvider(String providerId) {
        return repository.findByProviderIdOrderByUpdatedAtDesc(providerId);
    }

    /** Find a single template by ID */
    @Transactional(readOnly = true)
    public Optional<Template> findById(UUID id) {
        return repository.findById(id);
    }

    /** Save a new template or update an existing one by ID */
    public Template save(UUID id, String name, String providerId,
                         Map<String, Object> formState, String description, List<String> tags) {

        Template template = id != null
                ? repository.findById(id).orElse(new Template())
                : new Template();

        template.setName(name);
        template.setProviderId(providerId);
        template.setFormState(formState);
        template.setDescription(description);
        template.setTags(tags);

        Template saved = repository.save(template);
        log.info("[TemplateService] Saved template '{}' (id={}, provider={})",
                saved.getName(), saved.getId(), saved.getProviderId());
        return saved;
    }

    /** Delete a template by ID — returns false if not found */
    public boolean delete(UUID id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        log.info("[TemplateService] Deleted template id={}", id);
        return true;
    }

    /** Duplicate a template — creates a new record with "Copy of <name>" */
    public Template duplicate(UUID id) {
        Template original = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));

        Template copy = new Template();
        copy.setName("Copy of " + original.getName());
        copy.setProviderId(original.getProviderId());
        copy.setFormState(original.getFormState());
        copy.setDescription(original.getDescription());
        copy.setTags(original.getTags());

        Template saved = repository.save(copy);
        log.info("[TemplateService] Duplicated template '{}' → '{}' (id={})",
                original.getName(), saved.getName(), saved.getId());
        return saved;
    }
}