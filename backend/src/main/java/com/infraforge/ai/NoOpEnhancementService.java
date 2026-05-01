package com.infraforge.ai;

import com.infraforge.model.ProviderSchema;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * No-op AI provider — always returns empty suggestions.
 *
 * Used as fallback when:
 *   - The user has not configured an AI provider
 *   - The stored provider name does not match any registered implementation
 *   - AI features are intentionally disabled
 *
 * This is always registered as a Spring bean. The EnhancementServiceRouter
 * falls back to this when no matching provider is found for the user's config.
 */
@Component
public class NoOpEnhancementService implements EnhancementService {

    @Override
    public Map<String, String> suggest(
            ProviderSchema schema,
            String description,
            String apiKey,
            String model) {
        return Map.of();
    }

    @Override
    public String getProviderName() {
        return "noop";
    }
}