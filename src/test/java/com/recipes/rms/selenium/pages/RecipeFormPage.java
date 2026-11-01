package com.recipes.rms.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class RecipeFormPage extends BasePage {

    private static final By FORM = By.id("recipe-form");
    private static final By SAVE = By.id("save-button");

    public RecipeFormPage(WebDriver driver) {
        super(driver);
        visible(FORM);
    }

    public String heading() {
        return text(By.id("form-heading"));
    }

    public RecipeFormPage title(String value) {
        type(By.id("title"), value);
        return this;
    }

    public RecipeFormPage description(String value) {
        type(By.id("description"), value);
        return this;
    }

    public RecipeFormPage cuisine(String value) {
        type(By.id("cuisine"), value);
        return this;
    }

    public RecipeFormPage category(String value) {
        type(By.id("category"), value);
        return this;
    }

    public RecipeFormPage ingredients(String value) {
        type(By.id("ingredients"), value);
        return this;
    }

    public RecipeFormPage instructions(String value) {
        type(By.id("instructions"), value);
        return this;
    }

    public RecipeFormPage prepMinutes(int value) {
        type(By.id("prepMinutes"), String.valueOf(value));
        return this;
    }

    public RecipeFormPage cookMinutes(int value) {
        type(By.id("cookMinutes"), String.valueOf(value));
        return this;
    }

    public RecipeFormPage servings(int value) {
        type(By.id("servings"), String.valueOf(value));
        return this;
    }

    /** Fills every required field with sensible values. */
    public RecipeFormPage fillValid(String title, String cuisine, String category) {
        return title(title)
                .description("Created by an automated Selenium journey")
                .cuisine(cuisine)
                .category(category)
                .ingredients("2 cups flour\n1 cup water\nPinch of salt")
                .instructions("1. Mix everything.\n2. Knead for 10 minutes.\n3. Bake at 200C for 25 minutes.")
                .prepMinutes(15)
                .cookMinutes(25)
                .servings(4);
    }

    public RecipeDetailPage save() {
        click(SAVE);
        return new RecipeDetailPage(driver);
    }

    public RecipeFormPage saveExpectingErrors() {
        click(SAVE);
        visible(By.id("form-errors"));
        return new RecipeFormPage(driver);
    }

    public List<String> fieldErrors() {
        return driver.findElements(By.cssSelector(".field-error")).stream()
                .map(WebElement::getText).map(String::trim).toList();
    }
}
