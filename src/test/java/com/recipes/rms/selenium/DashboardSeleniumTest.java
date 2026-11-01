package com.recipes.rms.selenium;

import com.recipes.rms.selenium.pages.DashboardPage;
import com.recipes.rms.selenium.pages.RecipeListPage;
import com.recipes.rms.selenium.support.BaseSeleniumTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Journey 4: viewing dashboard metrics")
class DashboardSeleniumTest extends BaseSeleniumTest {

    private static final String[] STATUSES = {"DRAFT", "PENDING_REVIEW", "PUBLISHED"};

    @Test
    @DisplayName("status counts add up, top categories/cuisines are ranked")
    void metricsAreConsistent() {
        DashboardPage dashboard = loginAs(REVIEWER, REVIEWER_PASSWORD).clickDashboard();

        long sum = 0;
        for (String status : STATUSES) {
            sum += dashboard.count(status);
        }
        assertThat(dashboard.totalRecipes()).isGreaterThanOrEqualTo(12).isEqualTo(sum);

        assertThat(dashboard.topCategories()).containsKey("Main Course").hasSizeLessThanOrEqualTo(5);
        assertThat(dashboard.topCategories().keySet().iterator().next()).isEqualTo("Main Course");
        assertThat(dashboard.topCuisines()).containsKeys("Italian", "Indian").hasSizeLessThanOrEqualTo(5);
        assertThat(List.copyOf(dashboard.topCuisines().values())).isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    @DisplayName("creating a recipe increments the total and the draft count")
    void dashboardReflectsNewRecipe() {
        DashboardPage before = loginAs(AUTHOR, AUTHOR_PASSWORD).clickDashboard();
        long total = before.totalRecipes();
        long drafts = before.count("DRAFT");

        before.clickNewRecipe().fillValid(unique("Dashboard Check"), "Japanese", "Snack").save();
        DashboardPage after = DashboardPage.open(driver, baseUrl());

        assertThat(after.totalRecipes()).isEqualTo(total + 1);
        assertThat(after.count("DRAFT")).isEqualTo(drafts + 1);
    }

    @Test
    @DisplayName("clicking a status count opens the filtered recipe list")
    void drillDownToStatus() {
        DashboardPage dashboard = loginAs(REVIEWER, REVIEWER_PASSWORD).clickDashboard();
        long published = dashboard.count("PUBLISHED");

        RecipeListPage list = dashboard.openStatus("PUBLISHED");

        assertThat(list.statuses()).isNotEmpty().containsOnly("Published");
        assertThat(list.titles()).hasSize((int) Math.min(published, 10)); // first page holds 10 rows
    }
}
