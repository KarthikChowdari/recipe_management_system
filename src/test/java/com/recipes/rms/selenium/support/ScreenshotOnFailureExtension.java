package com.recipes.rms.selenium.support;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.WebDriver;

/**
 * Takes a screenshot when a test throws (assertion failure, timeout, missing element).
 *
 * <p>Uses {@link AfterTestExecutionCallback} rather than {@code TestWatcher} because it runs
 * <em>before</em> {@code @AfterEach}, i.e. while the browser session is still open.
 */
public class ScreenshotOnFailureExtension implements AfterTestExecutionCallback {

    /** Implemented by test classes so the extension can reach the live driver. */
    public interface WebDriverHolder {
        WebDriver getDriver();
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        if (context.getExecutionException().isEmpty()) {
            return;
        }
        Object instance = context.getRequiredTestInstance();
        if (instance instanceof WebDriverHolder holder && holder.getDriver() != null) {
            String name = context.getRequiredTestClass().getSimpleName() + "_" + context.getRequiredTestMethod().getName();
            ScreenshotUtil.capture(holder.getDriver(), name);
        }
    }
}
