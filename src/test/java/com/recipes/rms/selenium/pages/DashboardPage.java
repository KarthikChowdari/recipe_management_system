package com.recipes.rms.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.LinkedHashMap;
import java.util.Map;

public class DashboardPage extends BasePage {

    public DashboardPage(WebDriver driver) {
        super(driver);
        visible(By.id("total-recipes"));
    }

    public static DashboardPage open(WebDriver driver, String baseUrl) {
        driver.get(baseUrl + "/dashboard");
        return new DashboardPage(driver);
    }

    public long totalRecipes() {
        return Long.parseLong(text(By.id("total-recipes")));
    }

    /** Count for a status name, e.g. {@code DRAFT}. */
    public long count(String status) {
        return Long.parseLong(text(By.id("count-" + status)));
    }

    public Map<String, Long> topCategories() {
        return ranking("#top-categories li");
    }

    public Map<String, Long> topCuisines() {
        return ranking("#top-cuisines li");
    }

    public RecipeListPage openStatus(String status) {
        click(By.id("count-" + status));
        return new RecipeListPage(driver);
    }

    private Map<String, Long> ranking(String itemSelector) {
        Map<String, Long> result = new LinkedHashMap<>();
        driver.findElements(By.cssSelector(itemSelector)).forEach(item -> result.put(
                item.findElement(By.cssSelector(".name")).getText().trim(),
                Long.parseLong(item.findElement(By.cssSelector(".count")).getText().trim())));
        return result;
    }
}
