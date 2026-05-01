package com.infraforge.ai;

import com.infraforge.model.ProviderSchema;
import org.springframework.stereotype.Component;

/**
 * Builds the structured prompt sent to AI providers for field suggestions.
 *
 * The prompt is provider-agnostic — the same prompt works across all five providers.
 * It instructs the model to:
 *   - Return ONLY a JSON object (no markdown, no explanation)
 *   - Only suggest values for fields where it has confidence
 *   - Respect field types and use only valid option values for select fields
 *
 * Prompt injection mitigation:
 *   - Only schema-defined content (labels, options, aiHints) is included as context
 *   - The user description is included last and clearly delimited
 *   - Description length is validated upstream (max 2000 chars)
 */
@Component
public class PromptBuilder {

    /**
     * Build the full prompt for a suggest request.
     *
     * @param schema      the provider schema — defines sections, fields, options, hints
     * @param description the user's natural language input
     * @return the complete prompt string ready to send to any AI provider
     */
    public String build(ProviderSchema schema, String description) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are an Infrastructure as Code configuration assistant for InfraForge.\n");
        sb.append("Analyze the user's description and suggest appropriate values for a ")
          .append(schema.getLabel()).append(" template.\n\n");

        sb.append("Available sections and fields:\n");
        sb.append("─────────────────────────────\n");

        for (ProviderSchema.Section section : schema.getSections()) {
            sb.append("\nSection: ").append(section.getLabel())
              .append(" (id: ").append(section.getId()).append(")");

            if (section.getAiHint() != null && !section.getAiHint().isBlank()) {
                sb.append("\n  Context: ").append(section.getAiHint());
            }
            sb.append("\n");

            for (ProviderSchema.Field field : section.getFields()) {
                sb.append("  - ").append(field.getId())
                  .append(" | ").append(field.getLabel())
                  .append(" | type: ").append(field.getType());

                if (field.getOptions() != null && !field.getOptions().isEmpty()) {
                    sb.append(" | options: [")
                      .append(String.join(", ", field.getOptions()))
                      .append("]");
                }

                if (field.getAiHint() != null && !field.getAiHint().isBlank()) {
                    sb.append(" | hint: ").append(field.getAiHint());
                }

                sb.append("\n");
            }
        }

        sb.append("\n─────────────────────────────\n");
        sb.append("User description: \"").append(description).append("\"\n\n");

        sb.append("Instructions:\n");
        sb.append("1. Return ONLY a valid JSON object — no markdown, no code blocks, no explanation.\n");
        sb.append("2. Keys must be field IDs exactly as listed above.\n");
        sb.append("3. Only include fields where you have a confident suggestion.\n");
        sb.append("4. For select fields, use ONLY values from the provided options list.\n");
        sb.append("5. For toggle fields, use only \"true\" or \"false\".\n");
        sb.append("6. If the description does not relate to this provider, return {}.\n\n");
        sb.append("Example: {\"provider_region\": \"us-east-1\", \"instance_type\": \"t3.medium\"}");

        return sb.toString();
    }
}