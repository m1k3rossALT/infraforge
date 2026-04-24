package com.infraforge.model;

import java.util.List;
import java.util.Map;

/**
 * Request body for POST /api/providers/{id}/generate.
 *
 * sections maps section ID → SectionData (enabled flag + list of field-value instances).
 * A non-repeatable section always has exactly one instance.
 * A repeatable section may have 1..maxInstances instances.
 */
public class GenerateRequest {

    private Map<String, SectionData> sections;

    public Map<String, SectionData> getSections() { return sections; }
    public void setSections(Map<String, SectionData> sections) { this.sections = sections; }

    public static class SectionData {
        private boolean enabled;
        private List<Map<String, String>> instances;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public List<Map<String, String>> getInstances() { return instances; }
        public void setInstances(List<Map<String, String>> instances) { this.instances = instances; }
    }
}
