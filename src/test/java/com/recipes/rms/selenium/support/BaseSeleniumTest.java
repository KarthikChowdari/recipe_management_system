package com.recipes.rms.selenium.support;

import com.recipes.rms.selenium.pages.LoginPage;
import com.recipes.rms.selenium.pages.RecipeListPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.UUID;

/**
 * Starts the full application on a random port and opens a fresh Chrome session per test.
 * Tagged {@code selenium} so it only runs with {@code mvn test -Pselenium}.
 */
@Tag("selenium")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(ScreenshotOnFailureExtension.class)
public abstract class BaseSeleniumTest implements ScreenshotOnFailureExtension.WebDriverHolder {

    protected static final String AUTHOR = "author";
    protected static final String AUTHOR_PASSWORD = "author123";
    protected static final String REVIEWER = "reviewer";
    protected static final String REVIEWER_PASSWORD = "reviewer123";

    @LocalServerPort
    protected int port;

    protected WebDriver driver;

    @BeforeEach
    void openBrowser() {
        driver = DriverFactory.createChrome();
    }

    @AfterEach
    void closeBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Override
    public WebDriver getDriver() {
        return driver;
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    protected RecipeListPage loginAs(String username, String password) {
        return LoginPage.open(driver, baseUrl()).loginAs(username, password);
    }

    protected void logout() {
        driver.manage().deleteAllCookies();
    }

    /** Unique suffix so tests sharing the application database never collide. */
    protected static String unique(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }
}
