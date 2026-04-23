package com.infraforge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Represents the full schema for one IaC provider (e.g. Terraform).
 * Deserialized directly from providers/{id}/schema.json.
 * Adding a new provider = dropping a new folder with schema.json + template.ftl.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProviderSchema {

    private String id;
    private String label;
    private String version;
    private String fileExtension;
    private List<Section> sections;

    // --- Getters & Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getFileExtension() { return fileExtension; }
    public void setFileExtension(String fileExtension) { this.fileExtension = fileExtension; }

    public List<Section> getSections() { return sections; }
    public void setSections(List<Section> sections) { this.sections = sections; }

    // --- Nested types ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Section {
        private String id;
        private String label;
        private List<Field> fields;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public List<Field> getFields() { return fields; }
        public void setFields(List<Field> fields) { this.fields = fields; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Field {
        private String id;
        private String label;
        private String type;           // text | select | textarea
        private List<String> options;  // populated when type = select
        private boolean required;
        private String placeholder;
        private String help;
        private String defaultValue;
        // Reserved for future AI enhancement — ignored until Phase 4
        private String aiHint;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }

        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }

        public String getPlaceholder() { return placeholder; }
        public void setPlaceholder(String placeholder) { this.placeholder = placeholder; }

        public String getHelp() { return help; }
        public void setHelp(String help) { this.help = help; }

        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }

        public String getAiHint() { return aiHint; }
        public void setAiHint(String aiHint) { this.aiHint = aiHint; }
    }
}
