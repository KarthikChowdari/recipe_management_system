package com.recipes.rms.domain;

public enum RecipeStatus {
    DRAFT("Draft"),
    PENDING_REVIEW("Pending review"),
    PUBLISHED("Published");

    private final String label;

    RecipeStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
