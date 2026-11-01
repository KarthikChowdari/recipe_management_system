package com.recipes.rms.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

public class RecipeListPage extends BasePage {

    private static final By SEARCH_FORM = By.id("search-form");
    private static final By TITLE = By.id("search-title");
    private static final By CUISINE = By.id("search-cuisine");
    private static final By CATEGORY = By.id("search-category");
    private static final By STATUS = By.id("search-status");
    private static final By SEARCH = By.id("search-button");
    private static final By RESET = By.id("reset-button");
    private static final By ROWS = By.cssSelector("#recipe-table tr.recipe-row");
    private static final By NO_RESULTS = By.id("no-results");

    public RecipeListPage(WebDriver driver) {
        super(driver);
        visible(SEARCH_FORM);
    }

    public RecipeListPage search(String title, String cuisine, String category, String statusLabel) {
        type(TITLE, title);
        select(CUISINE, cuisine);
        select(CATEGORY, category);
        select(STATUS, statusLabel);
        WebElement oldForm = driver.findElement(SEARCH_FORM);
        click(SEARCH);
        wait.until(ExpectedConditions.stalenessOf(oldForm));
        return new RecipeListPage(driver);
    }

    public RecipeListPage searchByTitle(String title) {
        return search(title, null, null, null);
    }

    public RecipeListPage reset() {
        click(RESET);
        return new RecipeListPage(driver);
    }

    public List<String> titles() {
        return column("td.recipe-title");
    }

    public List<String> cuisines() {
        return column("td.recipe-cuisine");
    }

    public List<String> statuses() {
        return column("td.recipe-status");
    }

    public boolean showsNoResults() {
        return isPresent(NO_RESULTS);
    }

    public String selectedTitleFilter() {
        return visible(TITLE).getAttribute("value");
    }

    public RecipeDetailPage open(String title) {
        click(By.linkText(title));
        return new RecipeDetailPage(driver);
    }

    private List<String> column(String cellSelector) {
        return driver.findElements(ROWS).stream()
                .map(row -> row.findElement(By.cssSelector(cellSelector)).getText().trim())
                .toList();
    }
}
