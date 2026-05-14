package com.infraforge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * A curated read-only template shipped with InfraForge.
 *
 * Prebuilts are loaded at startup from JSON files under resources/prebuilts/.
 * They are never stored in the database — they live in the codebase.
 * Adding a new prebuilt requires only a new JSON file and a restart.
 *
 * Users can fork a prebuilt into their personal library via
 * POST /api/v1/prebuilts/{id}/fork, which creates a normal saved Template.
 *
 * Phase 6 hook: if platform-managed templates are added (paid tier only),
 * add a 'tier' field here and gate fork in the controller.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrebuiltTemplate {

    /** Unique slug — used in the fork URL. Must match the JSON filename stem. */
    private String id;

    private String name;
    private String description;

    /** Must match a registered provider ID (terraform, kubernetes, dockerfile, etc.) */
    private String providerId;

    private List<String> tags;

    /**
     * Complete form state — same structure as SavedTemplate.formState.
     * Sections not in the schema are ignored at render time.
     */
    private Map<String, Object> formState;

    public String getId()                        { return id; }
    public void setId(String id)                 { this.id = id; }
    public String getName()                      { return name; }
    public void setName(String name)             { this.name = name; }
    public String getDescription()               { return description; }
    public void setDescription(String d)         { this.description = d; }
    public String getProviderId()                { return providerId; }
    public void setProviderId(String p)          { this.providerId = p; }
    public List<String> getTags()                { return tags; }
    public void setTags(List<String> tags)       { this.tags = tags; }
    public Map<String, Object> getFormState()    { return formState; }
    public void setFormState(Map<String, Object> f) { this.formState = f; }
}