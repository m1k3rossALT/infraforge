package com.infraforge.model;

import java.util.Map;

/**
 * Request body for POST /api/providers/{id}/generate.
 * values maps field IDs to the user-supplied strings.
 */
public class GenerateRequest {

    private Map<String, String> values;

    public Map<String, String> getValues() { return values; }
    public void setValues(Map<String, String> values) { this.values = values; }
}
