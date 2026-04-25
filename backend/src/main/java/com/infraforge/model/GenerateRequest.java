package com.infraforge.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * Request body for POST /api/providers/{id}/generate.
 *
 * sections maps section ID to SectionData (enabled flag + list of field-value instances).
 * A non-repeatable section always has exactly one instance.
 * A repeatable section may have 1..maxInstances instances.
 */
public class GenerateRequest {

    @NotNull(message = "sections must not be null")
    @Size(max = 50, message = "Too many sections — maximum 50 allowed")
    @Valid
    private Map<String, SectionData> sections;

    public Map<String, SectionData> getSections() { return sections; }
    public void setSections(Map<String, SectionData> sections) { this.sections = sections; }

    public static class SectionData {

        private boolean enabled;

        @Size(max = 30, message = "Too many instances — maximum 30 per section")
        @Valid
        private List<@Valid Map<@Size(max = 100) String, @Size(max = 10000, message = "Field value too large") String>> instances;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public List<Map<String, String>> getInstances() { return instances; }
        public void setInstances(List<Map<String, String>> instances) { this.instances = instances; }
    }
}
