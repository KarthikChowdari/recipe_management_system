package com.recipes.rms.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Optional;

public class RecipeDetailPage extends BasePage {

    private static final By TITLE = By.id("recipe-title");

    public RecipeDetailPage(WebDriver driver) {
        super(driver);
        visible(TITLE);
    }

    public String title() {
        return text(TITLE);
    }

    public String status() {
        return text(By.id("recipe-status"));
    }

    public String cuisine() {
        return text(By.id("recipe-cuisine"));
    }

    public String category() {
        return text(By.id("recipe-category"));
    }

    public String servings() {
        return text(By.id("recipe-servings"));
    }

    public String author() {
        return text(By.id("recipe-author"));
    }

    public String ingredients() {
        return text(By.id("recipe-ingredients"));
    }

    public Optional<String> successMessage() {
        return flashSuccess();
    }

    public Optional<String> errorMessage() {
        return flashError();
    }

    public boolean canEdit() {
        return isPresent(By.id("edit-button"));
    }

    public RecipeFormPage edit() {
        click(By.id("edit-button"));
        return new RecipeFormPage(driver);
    }

    /** Workflow actions offered to the current user, e.g. {@code [SUBMIT]} or {@code [APPROVE, REJECT]}. */
    public List<String> availableActions() {
        return driver.findElements(By.cssSelector("#workflow-panel button[name=action]")).stream()
                .map(button -> button.getAttribute("value"))
                .toList();
    }

    public RecipeDetailPage perform(String action, String comment) {
        if (comment != null) {
            type(By.id("transition-comment"), comment);
        }
        click(By.id("action-" + action));
        return new RecipeDetailPage(driver);
    }

    public List<String> historyActions() {
        return driver.findElements(By.cssSelector("#history-table td.history-action")).stream()
                .map(WebElement::getText).map(String::trim).toList();
    }
}
