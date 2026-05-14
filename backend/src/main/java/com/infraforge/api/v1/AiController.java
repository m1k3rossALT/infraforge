package com.infraforge.api.v1;

import com.infraforge.ai.AiRateLimiterService;
import com.infraforge.ai.EnhancementService;
import com.infraforge.ai.EnhancementServiceRouter;
import com.infraforge.engine.ProviderRegistry;
import com.infraforge.model.ProviderSchema;
import com.infraforge.model.User;
import com.infraforge.model.UserRepository;
import com.infraforge.security.CurrentUser;
import com.infraforge.service.AiKeyEncryptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI-related endpoints — all require authentication.
 *
 * Suggest:
 *   POST /api/v1/ai/suggest/{providerId}  — generate field suggestions from a description
 *
 * Settings (user's BYOK configuration):
 *   GET    /api/v1/ai/settings            — current AI config (never returns raw key)
 *   PUT    /api/v1/ai/settings            — save/update AI provider + key
 *   DELETE /api/v1/ai/settings            — remove AI config
 *
 * Providers:
 *   GET    /api/v1/ai/providers           — list available AI provider names (for frontend dropdown)
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final ProviderRegistry         providerRegistry;
    private final EnhancementServiceRouter router;
    private final AiRateLimiterService     rateLimiter;
    private final AiKeyEncryptionService   encryptionService;
    private final UserRepository           userRepository;

    public AiController(ProviderRegistry providerRegistry,
                         EnhancementServiceRouter router,
                         AiRateLimiterService rateLimiter,
                         AiKeyEncryptionService encryptionService,
                         UserRepository userRepository) {
        this.providerRegistry  = providerRegistry;
        this.router            = router;
        this.rateLimiter       = rateLimiter;
        this.encryptionService = encryptionService;
        this.userRepository    = userRepository;
    }

    // ─── Suggest ─────────────────────────────────────────────────────────────

    /**
     * Generate field value suggestions for a provider based on a natural language description.
     *
     * Flow:
     *   1. Validate rate limit for current user
     *   2. Load current user's AI settings
     *   3. Decrypt stored API key
     *   4. Route to correct provider implementation
     *   5. Build reverse lookup (fieldId → sectionId) from schema
     *   6. Call AI provider, get flat fieldId → value map
     *   7. Group by sectionId and return nested suggestions
     *
     * Returns 200 with empty suggestions if:
     *   - User has no AI provider configured (not an error — frontend handles this)
     *   - AI call fails transiently (graceful degradation)
     */
    @PostMapping("/suggest/{providerId}")
    public ResponseEntity<?> suggest(
            @PathVariable String providerId,
            @Valid @RequestBody SuggestRequest request) {

        UUID userId = CurrentUser.id().orElseThrow();

        // Rate limit check
        if (!rateLimiter.tryConsume(userId)) {
            return ResponseEntity.status(429)
                .body(Map.of("message", "Rate limit exceeded. Maximum 10 suggestions per minute."));
        }

        // Load provider schema
        Optional<ProviderSchema> schemaOpt = providerRegistry.findById(providerId);
        if (schemaOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ProviderSchema schema = schemaOpt.get();

        // Load user AI settings
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        if (user.getAiProvider() == null || user.getAiApiKeyEnc() == null) {
            // No AI configured — return empty suggestions gracefully
            return ResponseEntity.ok(new SuggestResponse(Map.of(), null));
        }

        // Decrypt key
        String apiKey;
        try {
            apiKey = encryptionService.decrypt(user.getAiApiKeyEnc());
        } catch (Exception e) {
            log.warn("[AiController] Failed to decrypt API key for user {}: {}", userId, e.getMessage());
            return ResponseEntity.ok(new SuggestResponse(Map.of(), user.getAiProvider()));
        }

        // Get provider impl and call AI
        EnhancementService service = router.route(user.getAiProvider());
        Map<String, String> flatSuggestions = service.suggest(
            schema, request.description(), apiKey, user.getAiModel()
        );

        // Group flat suggestions by sectionId using reverse lookup
        Map<String, Map<String, String>> nested = groupBySectionId(schema, flatSuggestions);

        log.info("[AiController] Suggest: provider={}, fields suggested={}, user={}",
            user.getAiProvider(), flatSuggestions.size(), userId);

        return ResponseEntity.ok(new SuggestResponse(nested, user.getAiProvider()));
    }

    // ─── Settings ─────────────────────────────────────────────────────────────

    /** Returns current AI configuration. The API key is never returned — only hasApiKey flag. */
    @GetMapping("/settings")
    public ResponseEntity<AiSettingsResponse> getSettings() {
        UUID userId = CurrentUser.id().orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(new AiSettingsResponse(
            user.getAiProvider(),
            user.getAiApiKeyEnc() != null,
            user.getAiModel()
        ));
    }

    /** Save or update the user's AI provider and API key. */
    @PutMapping("/settings")
    public ResponseEntity<AiSettingsResponse> saveSettings(
            @Valid @RequestBody AiSettingsRequest request) {

        UUID userId = CurrentUser.id().orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();

        user.setAiProvider(request.aiProvider().toLowerCase());
        user.setAiApiKeyEnc(encryptionService.encrypt(request.apiKey()));
        user.setAiModel(request.model() != null && !request.model().isBlank() ? request.model() : null);

        userRepository.save(user);
        log.info("[AiController] AI settings saved for user={}, provider={}", userId, request.aiProvider());

        return ResponseEntity.ok(new AiSettingsResponse(user.getAiProvider(), true, user.getAiModel()));
    }

    /** Remove the user's AI configuration. */
    @DeleteMapping("/settings")
    public ResponseEntity<Void> deleteSettings() {
        UUID userId = CurrentUser.id().orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        user.setAiProvider(null);
        user.setAiApiKeyEnc(null);
        user.setAiModel(null);
        userRepository.save(user);
        log.info("[AiController] AI settings removed for user={}", userId);
        return ResponseEntity.noContent().build();
    }

    /** List available AI provider names — used to populate the frontend settings dropdown. */
    @GetMapping("/providers")
    public ResponseEntity<List<String>> listAiProviders() {
        return ResponseEntity.ok(router.registeredProviders());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Group flat fieldId → value suggestions into sectionId → (fieldId → value).
     * Fields not found in the schema are silently dropped.
     */
    private Map<String, Map<String, String>> groupBySectionId(
            ProviderSchema schema, Map<String, String> flatSuggestions) {

        // Build reverse lookup: fieldId → sectionId
        Map<String, String> fieldToSection = new LinkedHashMap<>();
        for (ProviderSchema.Section section : schema.getSections()) {
            for (ProviderSchema.Field field : section.getFields()) {
                fieldToSection.put(field.getId(), section.getId());
            }
        }

        // Group suggestions by section
        Map<String, Map<String, String>> nested = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : flatSuggestions.entrySet()) {
            String sectionId = fieldToSection.get(entry.getKey());
            if (sectionId != null) {
                nested.computeIfAbsent(sectionId, k -> new LinkedHashMap<>())
                      .put(entry.getKey(), entry.getValue());
            }
        }
        return nested;
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    record SuggestRequest(
        @NotBlank(message = "description must not be blank")
        @Size(max = 2000, message = "description must not exceed 2000 characters")
        String description
    ) {}

    record SuggestResponse(
        Map<String, Map<String, String>> suggestions,
        String provider  // which AI provider was used; null if not configured
    ) {}

    record AiSettingsRequest(
        @NotBlank(message = "aiProvider must not be blank")
        String aiProvider,

        @NotBlank(message = "apiKey must not be blank")
        @Size(max = 512, message = "apiKey is too long")
        String apiKey,

        String model  // optional — null or blank = use provider default
    ) {}

    record AiSettingsResponse(
        String  aiProvider,
        boolean hasApiKey,
        String  aiModel
    ) {}
}