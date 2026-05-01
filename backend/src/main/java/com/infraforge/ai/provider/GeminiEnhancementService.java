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

import java.util.List;
import java.util.Map;

/**
 * Gemini AI provider implementation.
 *
 * API: generativelanguage.googleapis.com/v1beta/models/{model}:generateContent
 * Auth: API key as query parameter
 * Default model: gemini-2.0-flash (fast, cost-effective for structured suggestions)
 *
 * Note: Gemini does not guarantee JSON output without responseMimeType.
 * We request JSON mime type AND instruct via prompt — belt and suspenders.
 */
@Component
public class GeminiEnhancementService implements EnhancementService {

    private static final Logger log = LoggerFactory.getLogger(GeminiEnhancementService.class);

    private static final String BASE_URL      = "https://generativelanguage.googleapis.com";
    private static final String DEFAULT_MODEL = "gemini-2.0-flash";

    private final PromptBuilder promptBuilder;
    private final ObjectMapper  objectMapper;
    private final RestClient    restClient;

    public GeminiEnhancementService(PromptBuilder promptBuilder, ObjectMapper objectMapper,
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
                "contents", List.of(
                    Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                    "responseMimeType", "application/json",
                    "maxOutputTokens", 1024
                )
            );

            GeminiResponse response = restClient.post()
                .uri("/v1beta/models/{model}:generateContent?key={key}", resolvedModel, apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(GeminiResponse.class);

            if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
                log.warn("[GeminiEnhancementService] Empty response from Gemini");
                return Map.of();
            }

            String text = response.candidates().get(0).content().parts().get(0).text();
            return parseJsonResponse(text);

        } catch (Exception e) {
            log.warn("[GeminiEnhancementService] Suggest call failed: {}", e.getMessage());
            return Map.of();
        }
    }

    @Override
    public String getProviderName() { return "gemini"; }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseJsonResponse(String text) {
        try {
            String cleaned = stripMarkdown(text);
            Map<?, ?> raw = objectMapper.readValue(cleaned, Map.class);
            // Only include string values — skip any non-string values the model may include
            Map<String, String> result = new java.util.LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                if (entry.getKey() instanceof String k && entry.getValue() instanceof String v) {
                    result.put(k, v);
                } else if (entry.getKey() instanceof String k && entry.getValue() != null) {
                    result.put(k, entry.getValue().toString());
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("[GeminiEnhancementService] Failed to parse AI JSON response: {}", e.getMessage());
            return Map.of();
        }
    }

    private String stripMarkdown(String text) {
        // Some models wrap JSON in ```json ... ``` despite instructions
        String t = text.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```(json)?\\s*", "").replaceFirst("```\\s*$", "").trim();
        }
        return t;
    }

    // ─── Response DTOs ────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeminiResponse(List<Candidate> candidates) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidate(Content content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Content(List<Part> parts) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Part(String text) {}
}