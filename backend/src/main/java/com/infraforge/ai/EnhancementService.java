package com.infraforge.ai;

import com.infraforge.model.ProviderSchema;

import java.util.Map;

/**
 * Contract for AI-powered form field suggestions.
 *
 * Each AI provider (Gemini, OpenAI, Anthropic, Mistral, Groq) implements this interface.
 * The NoOpEnhancementService implementation is always available as a safe fallback.
 *
 * Implementations must:
 *   - Return a flat map of fieldId → suggested value
 *   - Only include fields where the AI has a confident suggestion
 *   - Never throw on transient API errors — catch and return empty map with logged warning
 *   - Be stateless — all per-request context passed as parameters
 *
 * The caller (AiController) is responsible for:
 *   - Decrypting the user's stored API key before calling suggest()
 *   - Building the reverse lookup (fieldId → sectionId) to produce the nested response
 *   - Rate limiting
 *
 * Phase 6 hook: when platform-managed AI is added, inject the platform key at call time
 * instead of requiring the user to provide one. No interface change needed.
 */
public interface EnhancementService {

    /**
     * Generate field value suggestions based on a natural language description.
     *
     * @param schema      full provider schema — used to build the prompt context
     * @param description user's natural language description of what they want to build
     * @param apiKey      the user's plaintext API key for this provider
     * @param model       model override — null means use the provider's default
     * @return flat map of fieldId → suggested value; empty map if no suggestions or on error
     */
    Map<String, String> suggest(
        ProviderSchema schema,
        String description,
        String apiKey,
        String model
    );

    /**
     * Provider identifier — matches the value stored in users.ai_provider.
     * Valid values: "gemini", "openai", "anthropic", "mistral", "groq", "noop"
     */
    String getProviderName();
}