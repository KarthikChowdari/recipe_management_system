package com.recipes.rms.repository;

import com.recipes.rms.domain.Recipe;
import com.recipes.rms.domain.RecipeStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class RecipeSpecifications {

    private RecipeSpecifications() {
    }

    public static Specification<Recipe> matching(RecipeSearchCriteria criteria) {
        return Specification.allOf(
                titleContains(criteria.title()),
                equalsIgnoreCase("cuisine", criteria.cuisine()),
                equalsIgnoreCase("category", criteria.category()),
                hasStatus(criteria.status()));
    }

    static Specification<Recipe> titleContains(String title) {
        if (RecipeSearchCriteria.isBlank(title)) {
            return null;
        }
        String pattern = "%" + escapeLike(title.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern, '\\');
    }

    static Specification<Recipe> equalsIgnoreCase(String attribute, String value) {
        if (RecipeSearchCriteria.isBlank(value)) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(cb.lower(root.get(attribute)), normalized);
    }

    static Specification<Recipe> hasStatus(RecipeStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
