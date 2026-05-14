package com.infraforge.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infraforge.ai.PromptBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Groq provider — uses Groq's OpenAI-compatible API with Llama models.
 * Default model: llama-3.3-70b-versatile (very fast inference, free tier available)
 *
 * Note: Groq's free tier has generous limits — good default choice for users
 * who want to try the AI feature with minimal cost.
 */
@Component
public class GroqEnhancementService extends OpenAiCompatibleEnhancementService {

    public GroqEnhancementService(PromptBuilder promptBuilder, ObjectMapper objectMapper,
                                   RestClient.Builder restClientBuilder) {
        super(promptBuilder, objectMapper, restClientBuilder);
    }

    @Override protected String getBaseUrl()      { return "https://api.groq.com/openai"; }
    @Override protected String getDefaultModel() { return "llama-3.3-70b-versatile"; }
    @Override public    String getProviderName() { return "groq"; }
}