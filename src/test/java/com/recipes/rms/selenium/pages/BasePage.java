package com.recipes.rms.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/** Shared helpers for page objects: explicit waits and form input. */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    protected WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    protected void type(By locator, String text) {
        WebElement element = visible(locator);
        element.clear();
        if (text != null) {
            element.sendKeys(text);
        }
    }

    protected void select(By locator, String visibleTextOrEmpty) {
        Select select = new Select(visible(locator));
        if (visibleTextOrEmpty == null || visibleTextOrEmpty.isEmpty()) {
            select.selectByIndex(0);
        } else {
            select.selectByVisibleText(visibleTextOrEmpty);
        }
    }

    protected String text(By locator) {
        return visible(locator).getText().trim();
    }

    protected boolean isPresent(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    protected Optional<String> flashSuccess() {
        List<WebElement> flash = driver.findElements(By.id("flash-success"));
        return flash.isEmpty() ? Optional.empty() : Optional.of(flash.get(0).getText().trim());
    }

    protected Optional<String> flashError() {
        List<WebElement> flash = driver.findElements(By.id("flash-error"));
        return flash.isEmpty() ? Optional.empty() : Optional.of(flash.get(0).getText().trim());
    }

    public String currentUser() {
        return text(By.id("current-user"));
    }

    public RecipeFormPage clickNewRecipe() {
        click(By.id("nav-new"));
        return new RecipeFormPage(driver);
    }

    public DashboardPage clickDashboard() {
        click(By.id("nav-dashboard"));
        return new DashboardPage(driver);
    }

    public RecipeListPage clickRecipes() {
        click(By.id("nav-recipes"));
        return new RecipeListPage(driver);
    }
}
