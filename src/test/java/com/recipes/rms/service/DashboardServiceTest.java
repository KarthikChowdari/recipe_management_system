package com.recipes.rms.service;

import com.recipes.rms.domain.RecipeStatus;
import com.recipes.rms.repository.NameCount;
import com.recipes.rms.repository.RecipeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    RecipeRepository recipeRepository;

    @InjectMocks
    DashboardService dashboardService;

    @Test
    void fillsMissingStatusesWithZeroAndComputesTotals() {
        when(recipeRepository.countByStatus()).thenReturn(List.of(
                new Object[]{RecipeStatus.DRAFT, 3L},
                new Object[]{RecipeStatus.PUBLISHED, 1L}));
        when(recipeRepository.topCategories(any())).thenReturn(List.of(new NameCount("Soup", 4)));
        when(recipeRepository.topCuisines(any())).thenReturn(List.of(new NameCount("French", 4)));

        DashboardSummary summary = dashboardService.summary();

        assertThat(summary.totalRecipes()).isEqualTo(4);
        assertThat(summary.countByStatus()).containsKeys(RecipeStatus.values());
        assertThat(summary.count(RecipeStatus.PENDING_REVIEW)).isZero();
        assertThat(summary.publishedPercent()).isEqualTo(25);
        assertThat(summary.topCategories()).extracting(NameCount::name).containsExactly("Soup");
    }

    @Test
    void emptyCatalogue() {
        when(recipeRepository.countByStatus()).thenReturn(List.of());

        DashboardSummary summary = dashboardService.summary();

        assertThat(summary.totalRecipes()).isZero();
        assertThat(summary.publishedPercent()).isZero();
    }
}
