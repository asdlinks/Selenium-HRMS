package com.mywehr.utils;

import com.mywehr.config.ConfigManager;
import com.mywehr.driver.DriverManager;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/** Screenshot capture for the HTML report and for on-disk failure evidence. */
public final class ScreenshotUtils {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private ScreenshotUtils() {
    }

    /**
     * Base64 PNG for embedding directly in the Extent report, so a report can
     * be mailed as a single file with its evidence intact.
     */
    public static String captureAsBase64() {
        if (!DriverManager.isInitialised()) {
            return null;
        }
        try {
            return ((TakesScreenshot) DriverManager.get()).getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            Log.warn("Could not capture screenshot: " + e.getMessage());
            return null;
        }
    }

    /** Writes a PNG to the screenshot directory and returns its absolute path. */
    public static String captureToFile(String testName) {
        if (!DriverManager.isInitialised()) {
            return null;
        }
        try {
            File source = ((TakesScreenshot) DriverManager.get()).getScreenshotAs(OutputType.FILE);
            String fileName = testName.replaceAll("[^a-zA-Z0-9_-]", "_")
                    + "_" + LocalDateTime.now().format(STAMP) + ".png";
            File target = new File(ConfigManager.get("screenshot.dir"), fileName);
            FileUtils.copyFile(source, target);
            Log.info("Screenshot saved: " + target.getAbsolutePath());
            return target.getAbsolutePath();
        } catch (IOException | RuntimeException e) {
            Log.warn("Could not save screenshot: " + e.getMessage());
            return null;
        }
    }

    public static byte[] decode(String base64) {
        return Base64.getDecoder().decode(base64);
    }
}
