package com.infraforge.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infraforge.ai.PromptBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Mistral AI provider — uses Mistral Chat Completions API (OpenAI-compatible format).
 * Default model: mistral-small-latest (cost-effective, good instruction following)
 */
@Component
public class MistralEnhancementService extends OpenAiCompatibleEnhancementService {

    public MistralEnhancementService(PromptBuilder promptBuilder, ObjectMapper objectMapper,
                                      RestClient.Builder restClientBuilder) {
        super(promptBuilder, objectMapper, restClientBuilder);
    }

    @Override protected String getBaseUrl()      { return "https://api.mistral.ai"; }
    @Override protected String getDefaultModel() { return "mistral-small-latest"; }
    @Override public    String getProviderName() { return "mistral"; }
}