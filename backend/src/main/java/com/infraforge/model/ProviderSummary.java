package com.infraforge.model;

/**
 * Lightweight summary returned by GET /api/providers.
 * Only what the frontend needs to render the tab bar.
 */
public class ProviderSummary {

    private final String id;
    private final String label;

    public ProviderSummary(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
}
