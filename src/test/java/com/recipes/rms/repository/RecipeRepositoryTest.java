package com.recipes.rms.repository;

import com.recipes.rms.domain.Recipe;
import com.recipes.rms.domain.RecipeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RecipeRepositoryTest {

    @Autowired
    RecipeRepository recipeRepository;

    @Autowired
    StatusHistoryRepository historyRepository;

    @BeforeEach
    void replaceSeedDataWithKnownFixtures() {
        historyRepository.deleteAll();
        recipeRepository.deleteAll();
        save("Margherita Pizza", "Italian", "Main Course", RecipeStatus.PUBLISHED);
        save("Pizza Bianca", "Italian", "Main Course", RecipeStatus.DRAFT);
        save("Tiramisu", "Italian", "Dessert", RecipeStatus.PENDING_REVIEW);
        save("Pad Thai", "Thai", "Main Course", RecipeStatus.PUBLISHED);
        save("100% Juice", "American", "Beverage", RecipeStatus.DRAFT);
    }

    private void save(String title, String cuisine, String category, RecipeStatus status) {
        Recipe r = new Recipe();
        r.setTitle(title);
        r.setCuisine(cuisine);
        r.setCategory(category);
        r.setIngredients("x");
        r.setInstructions("y");
        r.setStatus(status);
        r.setCreatedBy("author");
        recipeRepository.save(r);
    }

    private List<String> titles(RecipeSearchCriteria criteria) {
        return recipeRepository.findAll(RecipeSpecifications.matching(criteria)).stream()
                .map(Recipe::getTitle).sorted().toList();
    }

    @Test
    void emptyCriteriaReturnsEverything() {
        assertThat(titles(RecipeSearchCriteria.empty())).hasSize(5);
    }

    @Test
    void titleSearchIsCaseInsensitiveContains() {
        assertThat(titles(new RecipeSearchCriteria("PIZZA", null, null, null)))
                .containsExactly("Margherita Pizza", "Pizza Bianca");
    }

    @Test
    void likeWildcardsInInputAreTreatedLiterally() {
        assertThat(titles(new RecipeSearchCriteria("%", null, null, null))).containsExactly("100% Juice");
    }

    @Test
    void filtersCombineWithAnd() {
        assertThat(titles(new RecipeSearchCriteria("", "italian", "main course", RecipeStatus.PUBLISHED)))
                .containsExactly("Margherita Pizza");
        assertThat(titles(new RecipeSearchCriteria(null, "Thai", null, RecipeStatus.DRAFT))).isEmpty();
    }

    @Test
    void aggregatesForDashboard() {
        List<NameCount> categories = recipeRepository.topCategories(PageRequest.of(0, 2));
        assertThat(categories).containsExactly(new NameCount("Main Course", 3), new NameCount("Beverage", 1));

        assertThat(recipeRepository.topCuisines(PageRequest.of(0, 1)))
                .containsExactly(new NameCount("Italian", 3));

        assertThat(recipeRepository.countByStatus())
                .extracting(row -> row[0] + "=" + row[1])
                .containsExactlyInAnyOrder("PUBLISHED=2", "DRAFT=2", "PENDING_REVIEW=1");

        assertThat(recipeRepository.findDistinctCuisines()).containsExactly("American", "Italian", "Thai");
    }
}
