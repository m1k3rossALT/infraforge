package com.infraforge.ai.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infraforge.ai.EnhancementService;
import com.infraforge.ai.PromptBuilder;
import com.infraforge.model.ProviderSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base for providers that use the OpenAI-compatible chat completions API.
 * OpenAI, Mistral, and Groq all share the same request/response format.
 *
 * Subclasses provide:
 *   - getBaseUrl()    — provider's API base URL
 *   - getDefaultModel() — default model if the user has not overridden
 *   - getProviderName() — identifier stored in users.ai_provider
 *
 * Adding a new OpenAI-compatible provider requires only a new subclass — no logic changes here.
 */
abstract class OpenAiCompatibleEnhancementService implements EnhancementService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleEnhancementService.class);

    protected final PromptBuilder promptBuilder;
    protected final ObjectMapper  objectMapper;
    protected final RestClient    restClient;

    protected OpenAiCompatibleEnhancementService(PromptBuilder promptBuilder,
                                                   ObjectMapper objectMapper,
                                                   RestClient.Builder restClientBuilder) {
        this.promptBuilder = promptBuilder;
        this.objectMapper  = objectMapper;
        this.restClient    = restClientBuilder.baseUrl(getBaseUrl()).build();
    }

    protected abstract String getBaseUrl();
    protected abstract String getDefaultModel();

    @Override
    public Map<String, String> suggest(ProviderSchema schema, String description,
                                        String apiKey, String model) {
        String resolvedModel = (model != null && !model.isBlank()) ? model : getDefaultModel();
        String prompt = promptBuilder.build(schema, description);

        try {
            Map<String, Object> requestBody = Map.of(
                "model", resolvedModel,
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                ),
                "response_format", Map.of("type", "json_object"),
                "max_tokens", 1024
            );

            OpenAiResponse response = restClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(OpenAiResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                log.warn("[{}] Empty response", getProviderName());
                return Map.of();
            }

            String text = response.choices().get(0).message().content();
            return parseJsonResponse(text);

        } catch (Exception e) {
            log.warn("[{}] Suggest call failed: {}", getProviderName(), e.getMessage());
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseJsonResponse(String text) {
        try {
            String cleaned = stripMarkdown(text);
            Map<?, ?> raw = objectMapper.readValue(cleaned, Map.class);
            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                if (entry.getKey() instanceof String k) {
                    result.put(k, entry.getValue() != null ? entry.getValue().toString() : "");
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("[{}] Failed to parse AI JSON response: {}", getProviderName(), e.getMessage());
            return Map.of();
        }
    }

    private String stripMarkdown(String text) {
        String t = text.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```(json)?\\s*", "").replaceFirst("```\\s*$", "").trim();
        }
        return t;
    }

    // ─── Response DTOs ────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OpenAiResponse(List<Choice> choices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String content) {}
}