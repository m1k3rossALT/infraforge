package com.infraforge.service;

import com.infraforge.model.Template;
import com.infraforge.model.TemplateRepository;
import com.infraforge.model.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Business logic for saved templates.
 * All mutating operations accept an optional userId for ownership scoping.
 * Null userId = guest/unowned (backward compatible with Phase 3 templates).
 */
@Service
@Transactional
public class TemplateService {

    private static final Logger log = LoggerFactory.getLogger(TemplateService.class);

    private final TemplateRepository repository;
    private final UserRepository userRepository;

    public TemplateService(TemplateRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    /** List templates — if userId provided, returns only that user's templates */
    @Transactional(readOnly = true)
    public List<Template> listAll(UUID userId) {
        if (userId != null) {
            return repository.findByUserIdOrderByUpdatedAtDesc(userId);
        }
        return repository.findAllByOrderByUpdatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Template> listByProvider(String providerId, UUID userId) {
        if (userId != null) {
            return repository.findByProviderIdAndUserIdOrderByUpdatedAtDesc(providerId, userId);
        }
        return repository.findByProviderIdOrderByUpdatedAtDesc(providerId);
    }

    @Transactional(readOnly = true)
    public Optional<Template> findById(UUID id) {
        return repository.findById(id);
    }

    /** Find a template by its public share token — used by the public shared view endpoint */
    @Transactional(readOnly = true)
    public Optional<Template> findByShareToken(UUID shareToken) {
        return repository.findByShareToken(shareToken);
    }

    public Template save(UUID id, String name, String providerId,
                         Map<String, Object> formState, String description,
                         List<String> tags, UUID userId) {

        Template template = id != null
                ? repository.findById(id).orElse(new Template())
                : new Template();

        template.setName(name);
        template.setProviderId(providerId);
        template.setFormState(formState);
        template.setDescription(description);
        template.setTags(tags);

        // Set owner if provided and not already set
        if (userId != null && template.getUserId() == null) {
            template.setUserId(userId);
        }

        Template saved = repository.save(template);
        log.info("[TemplateService] Saved template '{}' (id={}, user={})",
                saved.getName(), saved.getId(), userId);
        return saved;
    }

    public boolean delete(UUID id, UUID userId) {
        Optional<Template> opt = repository.findById(id);
        if (opt.isEmpty()) return false;

        Template template = opt.get();
        if (userId != null && template.getUserId() != null
                && !template.getUserId().equals(userId)) {
            throw new SecurityException("Not authorised to delete this template");
        }

        repository.deleteById(id);
        log.info("[TemplateService] Deleted template id={}", id);
        return true;
    }

    public Template duplicate(UUID id, UUID userId) {
        Template original = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));

        Template copy = new Template();
        copy.setName("Copy of " + original.getName());
        copy.setProviderId(original.getProviderId());
        copy.setFormState(original.getFormState());
        copy.setDescription(original.getDescription());
        copy.setTags(original.getTags());
        copy.setUserId(userId);
        // share_token intentionally not copied — the duplicate is private by default

        Template saved = repository.save(copy);
        log.info("[TemplateService] Duplicated '{}' -> '{}' (id={})",
                original.getName(), saved.getName(), saved.getId());
        return saved;
    }

    /**
     * Generate a share token for the given template.
     * Idempotent — if the template already has a token, the existing one is returned.
     * Only the owner can share their template.
     *
     * @throws SecurityException    if caller does not own the template
     * @throws IllegalArgumentException if template is not found
     */
    public UUID generateShareToken(UUID templateId, UUID userId) {
        Template template = repository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

        if (template.getUserId() != null && !template.getUserId().equals(userId)) {
            throw new SecurityException("Not authorised to share this template");
        }

        if (template.getShareToken() != null) {
            return template.getShareToken(); // already shared — return existing token
        }

        UUID token = UUID.randomUUID();
        template.setShareToken(token);
        repository.save(template);
        log.info("[TemplateService] Share token generated for template id={}", templateId);
        return token;
    }

    /**
     * Revoke the share token for the given template.
     * After this call, the public shared URL returns 404.
     * Only the owner can revoke.
     *
     * @throws SecurityException    if caller does not own the template
     * @throws IllegalArgumentException if template is not found
     */
    public void revokeShareToken(UUID templateId, UUID userId) {
        Template template = repository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

        if (template.getUserId() != null && !template.getUserId().equals(userId)) {
            throw new SecurityException("Not authorised to revoke sharing for this template");
        }

        template.setShareToken(null);
        repository.save(template);
        log.info("[TemplateService] Share token revoked for template id={}", templateId);
    }
}