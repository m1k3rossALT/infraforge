package com.infraforge.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infraforge.ai.PromptBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * OpenAI provider — uses OpenAI Chat Completions API.
 * Default model: gpt-4o-mini (fast, affordable, strong at structured output)
 */
@Component
public class OpenAiEnhancementService extends OpenAiCompatibleEnhancementService {

    public OpenAiEnhancementService(PromptBuilder promptBuilder, ObjectMapper objectMapper,
                                     RestClient.Builder restClientBuilder) {
        super(promptBuilder, objectMapper, restClientBuilder);
    }

    @Override protected String getBaseUrl()      { return "https://api.openai.com"; }
    @Override protected String getDefaultModel() { return "gpt-4o-mini"; }
    @Override public    String getProviderName() { return "openai"; }
}