package com.recipes.rms.repository;

import com.recipes.rms.domain.RecipeStatus;

/** Optional search filters; blank values are ignored. Bound from query parameters in the API and UI. */
public record RecipeSearchCriteria(String title, String cuisine, String category, RecipeStatus status) {

    public static RecipeSearchCriteria empty() {
        return new RecipeSearchCriteria(null, null, null, null);
    }

    public boolean isEmpty() {
        return isBlank(title) && isBlank(cuisine) && isBlank(category) && status == null;
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
