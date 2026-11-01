package com.recipes.rms.service;

import com.recipes.rms.domain.RecipeStatus;
import com.recipes.rms.repository.NameCount;

import java.util.List;
import java.util.Map;

/**
 * Catalogue statistics. {@code countByStatus} always contains every status (zero when empty),
 * in workflow order.
 */
public record DashboardSummary(long totalRecipes,
                               Map<RecipeStatus, Long> countByStatus,
                               List<NameCount> topCategories,
                               List<NameCount> topCuisines) {

    public long count(RecipeStatus status) {
        return countByStatus.getOrDefault(status, 0L);
    }

    /** Share of recipes that are published, 0-100. */
    public int publishedPercent() {
        return totalRecipes == 0 ? 0 : (int) Math.round(100.0 * count(RecipeStatus.PUBLISHED) / totalRecipes);
    }
}
