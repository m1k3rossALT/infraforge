package com.infraforge.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persisted IaC template saved by the user.
 *
 * form_state is stored as JSONB — a flexible map of sectionId → SectionData.
 * This mirrors the frontend FormState type exactly, so loading a template
 * restores the form to its exact saved state without any transformation.
 *
 * The JSONB column means adding new providers or new fields to existing
 * providers does not require a database migration — the JSON just grows.
 */
@Entity
@Table(name = "templates")
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "provider_id", nullable = false, length = 64)
    private String providerId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "form_state", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> formState;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private OffsetDateTime generatedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text[]")
    private List<String> tags;

    // Owner of this template — nullable for backward compatibility with pre-auth templates
    @Column(name = "user_id")
    private UUID userId;

    @PrePersist
    protected void onCreate() {
        generatedAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public Map<String, Object> getFormState() { return formState; }
    public void setFormState(Map<String, Object> formState) { this.formState = formState; }

    public OffsetDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(OffsetDateTime generatedAt) { this.generatedAt = generatedAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
}