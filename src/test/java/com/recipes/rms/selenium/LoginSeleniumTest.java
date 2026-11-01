package com.recipes.rms.selenium;

import com.recipes.rms.selenium.pages.LoginPage;
import com.recipes.rms.selenium.pages.RecipeListPage;
import com.recipes.rms.selenium.support.BaseSeleniumTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Journey 0: signing in")
class LoginSeleniumTest extends BaseSeleniumTest {

    @Test
    @DisplayName("valid credentials land on the recipe list")
    void validLogin() {
        RecipeListPage list = loginAs(AUTHOR, AUTHOR_PASSWORD);

        assertThat(driver.getCurrentUrl()).endsWith("/recipes");
        assertThat(list.currentUser()).isEqualTo(AUTHOR);
        assertThat(list.titles()).isNotEmpty();
    }

    @Test
    @DisplayName("wrong password shows an error and stays on the login page")
    void invalidLogin() {
        LoginPage page = LoginPage.open(driver, baseUrl()).loginExpectingFailure(AUTHOR, "wrong-password");

        assertThat(page.errorMessage()).contains("Invalid username or password");
        assertThat(driver.getCurrentUrl()).contains("/login");
    }
}
