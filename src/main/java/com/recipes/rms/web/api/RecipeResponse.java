package com.recipes.rms.web.api;

import com.recipes.rms.domain.Recipe;
import com.recipes.rms.domain.RecipeStatus;

import java.time.LocalDateTime;

public record RecipeResponse(
        Long id,
        String title,
        String description,
        String cuisine,
        String category,
        String ingredients,
        String instructions,
        Integer prepMinutes,
        Integer cookMinutes,
        Integer servings,
        RecipeStatus status,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long version) {

    public static RecipeResponse from(Recipe r) {
        return new RecipeResponse(r.getId(), r.getTitle(), r.getDescription(), r.getCuisine(), r.getCategory(),
                r.getIngredients(), r.getInstructions(), r.getPrepMinutes(), r.getCookMinutes(), r.getServings(),
                r.getStatus(), r.getCreatedBy(), r.getCreatedAt(), r.getUpdatedAt(), r.getVersion());
    }
}
