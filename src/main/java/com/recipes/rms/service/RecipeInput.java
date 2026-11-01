package com.recipes.rms.service;

import com.recipes.rms.domain.Recipe;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create/update payload shared by the REST API (JSON body) and the web UI (form binding). */
public class RecipeInput {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 150, message = "Title must be 3-150 characters")
    private String title;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;

    @NotBlank(message = "Cuisine is required")
    @Size(max = 60, message = "Cuisine must be at most 60 characters")
    private String cuisine;

    @NotBlank(message = "Category is required")
    @Size(max = 60, message = "Category must be at most 60 characters")
    private String category;

    @NotBlank(message = "Ingredients are required")
    private String ingredients;

    @NotBlank(message = "Instructions are required")
    private String instructions;

    @Min(value = 0, message = "Prep time cannot be negative")
    @Max(value = 1440, message = "Prep time must be at most 1440 minutes")
    private Integer prepMinutes;

    @Min(value = 0, message = "Cook time cannot be negative")
    @Max(value = 1440, message = "Cook time must be at most 1440 minutes")
    private Integer cookMinutes;

    @Min(value = 1, message = "Servings must be at least 1")
    @Max(value = 100, message = "Servings must be at most 100")
    private Integer servings;

    /** Optional optimistic-lock check: when present it must match the stored version. */
    private Long version;

    public static RecipeInput from(Recipe recipe) {
        RecipeInput input = new RecipeInput();
        input.title = recipe.getTitle();
        input.description = recipe.getDescription();
        input.cuisine = recipe.getCuisine();
        input.category = recipe.getCategory();
        input.ingredients = recipe.getIngredients();
        input.instructions = recipe.getInstructions();
        input.prepMinutes = recipe.getPrepMinutes();
        input.cookMinutes = recipe.getCookMinutes();
        input.servings = recipe.getServings();
        input.version = recipe.getVersion();
        return input;
    }

    void applyTo(Recipe recipe) {
        recipe.setTitle(title.trim());
        recipe.setDescription(description == null || description.isBlank() ? null : description.trim());
        recipe.setCuisine(cuisine.trim());
        recipe.setCategory(category.trim());
        recipe.setIngredients(ingredients.trim());
        recipe.setInstructions(instructions.trim());
        recipe.setPrepMinutes(prepMinutes);
        recipe.setCookMinutes(cookMinutes);
        recipe.setServings(servings);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCuisine() {
        return cuisine;
    }

    public void setCuisine(String cuisine) {
        this.cuisine = cuisine;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public Integer getPrepMinutes() {
        return prepMinutes;
    }

    public void setPrepMinutes(Integer prepMinutes) {
        this.prepMinutes = prepMinutes;
    }

    public Integer getCookMinutes() {
        return cookMinutes;
    }

    public void setCookMinutes(Integer cookMinutes) {
        this.cookMinutes = cookMinutes;
    }

    public Integer getServings() {
        return servings;
    }

    public void setServings(Integer servings) {
        this.servings = servings;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
