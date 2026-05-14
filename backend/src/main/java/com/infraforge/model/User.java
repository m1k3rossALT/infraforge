package com.infraforge.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Authenticated user.
 *
 * Extensibility hooks:
 *   - role enum supports future RBAC (VIEWER / EDITOR / ADMIN)
 *   - organizationId is nullable — reserved for Phase 5b team workspaces
 *   - enabled flag allows soft-disable without deleting the account
 *   - aiProvider + aiApiKeyEnc + aiModel support Phase 4 BYOK AI suggestions
 *
 * Phase 6 note: add subscriptionStatus column here when Stripe integration is built.
 */
@Entity
@Table(name = "users")
public class User {

    public enum Role { VIEWER, EDITOR, ADMIN }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.EDITOR;

    /** Reserved for future org/team features */
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ─── AI settings (Phase 4 — BYOK) ────────────────────────────────────────

    /**
     * AI provider identifier chosen by the user.
     * Valid values: "gemini", "openai", "anthropic", "mistral", "groq"
     * Null = no AI configured.
     */
    @Column(name = "ai_provider", length = 32)
    private String aiProvider;

    /**
     * AES-256-GCM encrypted API key for the chosen provider.
     * Format: Base64(IV[12 bytes] + ciphertext + tag)
     * Null = no key stored.
     */
    @Column(name = "ai_api_key_enc", columnDefinition = "text")
    private String aiApiKeyEnc;

    /**
     * Optional model override. Null = use the provider's default model defined in code.
     * Stored as-is — not validated here, validated when making the API call.
     */
    @Column(name = "ai_model", length = 64)
    private String aiModel;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public String getAiProvider() { return aiProvider; }
    public void setAiProvider(String aiProvider) { this.aiProvider = aiProvider; }

    public String getAiApiKeyEnc() { return aiApiKeyEnc; }
    public void setAiApiKeyEnc(String aiApiKeyEnc) { this.aiApiKeyEnc = aiApiKeyEnc; }

    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }
}