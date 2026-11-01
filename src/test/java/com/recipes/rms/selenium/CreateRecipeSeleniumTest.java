package com.recipes.rms.selenium;

import com.recipes.rms.selenium.pages.RecipeDetailPage;
import com.recipes.rms.selenium.pages.RecipeFormPage;
import com.recipes.rms.selenium.pages.RecipeListPage;
import com.recipes.rms.selenium.support.BaseSeleniumTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Journey 1: creating a new recipe")
class CreateRecipeSeleniumTest extends BaseSeleniumTest {

    @Test
    @DisplayName("author creates a recipe; it is saved as a draft and appears in the list")
    void authorCreatesRecipe() {
        String title = unique("Sourdough Bread");

        RecipeDetailPage detail = loginAs(AUTHOR, AUTHOR_PASSWORD)
                .clickNewRecipe()
                .fillValid(title, "Bakery", "Bread")
                .save();

        assertThat(detail.title()).isEqualTo(title);
        assertThat(detail.status()).isEqualTo("Draft");
        assertThat(detail.author()).isEqualTo(AUTHOR);
        assertThat(detail.cuisine()).isEqualTo("Bakery");
        assertThat(detail.servings()).isEqualTo("4");
        assertThat(detail.ingredients()).contains("2 cups flour");
        assertThat(detail.successMessage()).hasValueSatisfying(msg -> assertThat(msg).contains("created as draft"));
        assertThat(detail.availableActions()).containsExactly("SUBMIT");

        RecipeListPage list = detail.clickRecipes().searchByTitle(title);
        assertThat(list.titles()).containsExactly(title);
    }

    @Test
    @DisplayName("missing required fields are reported and nothing is saved")
    void validationErrors() {
        RecipeFormPage form = loginAs(AUTHOR, AUTHOR_PASSWORD)
                .clickNewRecipe()
                .title("ab")
                .saveExpectingErrors();

        assertThat(form.fieldErrors())
                .contains("Title must be 3-150 characters", "Cuisine is required", "Category is required",
                        "Ingredients are required", "Instructions are required");
        assertThat(form.heading()).isEqualTo("New recipe");
    }
}
