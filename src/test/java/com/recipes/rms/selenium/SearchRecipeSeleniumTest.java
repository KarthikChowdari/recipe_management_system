package com.recipes.rms.selenium;

import com.recipes.rms.selenium.pages.RecipeDetailPage;
import com.recipes.rms.selenium.pages.RecipeListPage;
import com.recipes.rms.selenium.support.BaseSeleniumTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Journey 2: searching for an existing recipe")
class SearchRecipeSeleniumTest extends BaseSeleniumTest {

    @Test
    @DisplayName("partial, case-insensitive title search finds a seeded recipe and opens it")
    void searchByTitle() {
        RecipeListPage results = loginAs(REVIEWER, REVIEWER_PASSWORD).searchByTitle("carbon");

        assertThat(results.titles()).containsExactly("Spaghetti Carbonara");
        assertThat(results.selectedTitleFilter()).isEqualTo("carbon");

        RecipeDetailPage detail = results.open("Spaghetti Carbonara");
        assertThat(detail.cuisine()).isEqualTo("Italian");
        assertThat(detail.status()).isEqualTo("Published");
    }

    @Test
    @DisplayName("cuisine + status filters narrow the list; reset clears them")
    void filterByCuisineAndStatus() {
        RecipeListPage list = loginAs(AUTHOR, AUTHOR_PASSWORD);
        int unfiltered = list.titles().size();

        RecipeListPage italianPublished = list.search("", "Italian", null, "Published");
        assertThat(italianPublished.titles()).contains("Margherita Pizza", "Spaghetti Carbonara")
                .doesNotContain("Tiramisu");
        assertThat(italianPublished.cuisines()).containsOnly("Italian");
        assertThat(italianPublished.statuses()).containsOnly("Published");

        RecipeListPage reset = italianPublished.reset();
        assertThat(reset.selectedTitleFilter()).isEmpty();
        assertThat(reset.titles()).hasSize(unfiltered);
    }

    @Test
    @DisplayName("a search with no match shows the empty state")
    void noMatches() {
        RecipeListPage results = loginAs(AUTHOR, AUTHOR_PASSWORD).searchByTitle("no-such-recipe-xyz");

        assertThat(results.showsNoResults()).isTrue();
        assertThat(results.titles()).isEmpty();
    }
}
