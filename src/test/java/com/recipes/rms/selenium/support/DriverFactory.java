package com.recipes.rms.selenium.support;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;

/**
 * Creates Chrome sessions for the end-to-end tests.
 *
 * <p>System properties:
 * <ul>
 *   <li>{@code selenium.headless} (default {@code true}) - set to {@code false} to watch the browser</li>
 *   <li>{@code selenium.chromeBinary} - optional path to a Chrome/Chromium binary</li>
 * </ul>
 */
public final class DriverFactory {

    private static final Logger log = LoggerFactory.getLogger(DriverFactory.class);
    private static volatile boolean driverResolved;

    private DriverFactory() {
    }

    public static WebDriver createChrome() {
        resolveDriverBinary();

        ChromeOptions options = new ChromeOptions();
        if (Boolean.parseBoolean(System.getProperty("selenium.headless", "true"))) {
            options.addArguments("--headless=new");
        }
        options.addArguments(
                "--window-size=1366,900",
                "--no-sandbox",               // required when running as root in containers/CI
                "--disable-dev-shm-usage",    // avoid crashes with a small /dev/shm
                "--disable-gpu",
                "--disable-extensions",
                "--remote-allow-origins=*",
                // After logging in with a demo password Chrome shows "save password" / "password found in a
                // data breach" bubbles that steal keyboard and mouse focus; switch those features off.
                "--disable-features=PasswordLeakDetection,PasswordCheck,PasswordManagerOnboarding",
                "--disable-save-password-bubble",
                "--no-first-run",
                "--no-default-browser-check");
        options.setExperimentalOption("prefs", Map.of(
                "credentials_enable_service", false,
                "profile.password_manager_enabled", false,
                "profile.password_manager_leak_detection", false));
        String binary = System.getProperty("selenium.chromeBinary");
        if (binary != null && !binary.isBlank()) {
            options.setBinary(binary);
        }

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        driver.manage().timeouts().implicitlyWait(Duration.ZERO); // explicit waits only
        return driver;
    }

    /**
     * Downloads a chromedriver matching the installed Chrome via WebDriverManager. If that fails
     * (offline agent, proxy), Selenium's built-in Selenium Manager resolves the driver instead.
     */
    private static synchronized void resolveDriverBinary() {
        if (driverResolved) {
            return;
        }
        try {
            WebDriverManager.chromedriver().setup();
        } catch (RuntimeException ex) {
            log.warn("WebDriverManager could not resolve chromedriver ({}); falling back to Selenium Manager",
                    ex.getMessage());
        }
        driverResolved = true;
    }
}
