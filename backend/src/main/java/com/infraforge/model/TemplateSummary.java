package com.infraforge.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Lightweight summary returned by GET /api/v1/templates.
 * Does not include formState — only what is needed to render the library list.
 * Full formState is loaded separately when the user opens a template.
 */
public class TemplateSummary {

    private final UUID id;
    private final String name;
    private final String providerId;
    private final String description;
    private final List<String> tags;
    private final OffsetDateTime updatedAt;

    public TemplateSummary(Template t) {
        this.id = t.getId();
        this.name = t.getName();
        this.providerId = t.getProviderId();
        this.description = t.getDescription();
        this.tags = t.getTags();
        this.updatedAt = t.getUpdatedAt();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getProviderId() { return providerId; }
    public String getDescription() { return description; }
    public List<String> getTags() { return tags; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}