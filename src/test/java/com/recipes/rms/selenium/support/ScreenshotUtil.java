package com.recipes.rms.selenium.support;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/** Saves browser screenshots (and the page HTML) to {@code target/screenshots/}. */
public final class ScreenshotUtil {

    private static final Logger log = LoggerFactory.getLogger(ScreenshotUtil.class);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private ScreenshotUtil() {
    }

    public static Path screenshotDir() {
        return Paths.get(System.getProperty("selenium.screenshotDir", "target/screenshots"));
    }

    /**
     * Captures the current browser window.
     *
     * @param name base file name, e.g. {@code CreateRecipeSeleniumTest_createsDraft}
     * @return the PNG path, or empty if the driver could not take a screenshot
     */
    public static Optional<Path> capture(WebDriver driver, String name) {
        if (!(driver instanceof TakesScreenshot camera)) {
            return Optional.empty();
        }
        String base = sanitize(name) + "_" + LocalDateTime.now().format(STAMP);
        try {
            Path dir = Files.createDirectories(screenshotDir());
            Path png = dir.resolve(base + ".png");
            Files.write(png, camera.getScreenshotAs(OutputType.BYTES));
            Files.writeString(dir.resolve(base + ".html"),
                    "<!-- URL: " + driver.getCurrentUrl() + " -->\n" + driver.getPageSource(),
                    StandardCharsets.UTF_8);
            log.warn("Saved failure screenshot: {}", png.toAbsolutePath());
            return Optional.of(png);
        } catch (IOException | RuntimeException ex) {
            log.error("Could not save screenshot '{}': {}", base, ex.getMessage());
            return Optional.empty();
        }
    }

    static String sanitize(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
