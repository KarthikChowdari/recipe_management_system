package com.recipes.rms.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {

    private static final By USERNAME = By.id("username");
    private static final By PASSWORD = By.id("password");
    private static final By SUBMIT = By.id("login-button");
    private static final By ERROR = By.id("login-error");

    public LoginPage(WebDriver driver) {
        super(driver);
        visible(USERNAME);
    }

    public static LoginPage open(WebDriver driver, String baseUrl) {
        driver.get(baseUrl + "/login");
        return new LoginPage(driver);
    }

    public RecipeListPage loginAs(String username, String password) {
        submit(username, password);
        return new RecipeListPage(driver);
    }

    public LoginPage loginExpectingFailure(String username, String password) {
        submit(username, password);
        visible(ERROR);
        return this;
    }

    public String errorMessage() {
        return text(ERROR);
    }

    private void submit(String username, String password) {
        type(USERNAME, username);
        type(PASSWORD, password);
        click(SUBMIT);
    }
}
