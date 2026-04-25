package com.infraforge.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * Request body for POST /api/v1/templates and PUT /api/v1/templates/{id}.
 */
public class SaveTemplateRequest {

    @NotBlank(message = "Template name is required")
    @Size(max = 255, message = "Name must be 255 characters or fewer")
    private String name;

    @NotBlank(message = "Provider ID is required")
    @Size(max = 64, message = "Provider ID must be 64 characters or fewer")
    private String providerId;

    @NotNull(message = "Form state is required")
    private Map<String, Object> formState;

    @Size(max = 1000, message = "Description must be 1000 characters or fewer")
    private String description;

    private List<@Size(max = 50) String> tags;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public Map<String, Object> getFormState() { return formState; }
    public void setFormState(Map<String, Object> formState) { this.formState = formState; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}