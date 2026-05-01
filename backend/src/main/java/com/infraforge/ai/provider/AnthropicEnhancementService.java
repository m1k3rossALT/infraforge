package com.infraforge.ai.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infraforge.ai.EnhancementService;
import com.infraforge.ai.PromptBuilder;
import com.infraforge.model.ProviderSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Anthropic Claude provider — uses the Anthropic Messages API.
 *
 * API: api.anthropic.com/v1/messages
 * Auth: x-api-key header + anthropic-version header
 * Default model: claude-haiku-4-5-20251001 (fast, low cost, good at structured output)
 *
 * Note: Anthropic does not have a dedicated JSON response mode.
 * The prompt explicitly instructs the model to return only JSON.
 */
@Component
public class AnthropicEnhancementService implements EnhancementService {

    private static final Logger log = LoggerFactory.getLogger(AnthropicEnhancementService.class);

    private static final String BASE_URL          = "https://api.anthropic.com";
    private static final String DEFAULT_MODEL     = "claude-haiku-4-5-20251001";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final PromptBuilder promptBuilder;
    private final ObjectMapper  objectMapper;
    private final RestClient    restClient;

    public AnthropicEnhancementService(PromptBuilder promptBuilder, ObjectMapper objectMapper,
                                        RestClient.Builder restClientBuilder) {
        this.promptBuilder = promptBuilder;
        this.objectMapper  = objectMapper;
        this.restClient    = restClientBuilder.baseUrl(BASE_URL).build();
    }

    @Override
    public Map<String, String> suggest(ProviderSchema schema, String description,
                                        String apiKey, String model) {
        String resolvedModel = (model != null && !model.isBlank()) ? model : DEFAULT_MODEL;
        String prompt = promptBuilder.build(schema, description);

        try {
            Map<String, Object> requestBody = Map.of(
                "model", resolvedModel,
                "max_tokens", 1024,
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                )
            );

            AnthropicResponse response = restClient.post()
                .uri("/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(AnthropicResponse.class);

            if (response == null || response.content() == null || response.content().isEmpty()) {
                log.warn("[AnthropicEnhancementService] Empty response");
                return Map.of();
            }

            String text = response.content().get(0).text();
            return parseJsonResponse(text);

        } catch (Exception e) {
            log.warn("[AnthropicEnhancementService] Suggest call failed: {}", e.getMessage());
            return Map.of();
        }
    }

    @Override
    public String getProviderName() { return "anthropic"; }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseJsonResponse(String text) {
        try {
            String t = text.trim();
            if (t.startsWith("```")) {
                t = t.replaceFirst("^```(json)?\\s*", "").replaceFirst("```\\s*$", "").trim();
            }
            Map<?, ?> raw = objectMapper.readValue(t, Map.class);
            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                if (entry.getKey() instanceof String k) {
                    result.put(k, entry.getValue() != null ? entry.getValue().toString() : "");
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("[AnthropicEnhancementService] Failed to parse JSON response: {}", e.getMessage());
            return Map.of();
        }
    }

    // ─── Response DTOs ────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AnthropicResponse(List<ContentBlock> content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ContentBlock(String type, String text) {}
}