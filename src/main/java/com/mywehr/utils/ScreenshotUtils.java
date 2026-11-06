package com.mywehr.utils;

import com.mywehr.config.ConfigManager;
import com.mywehr.driver.DriverManager;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
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

    /**
     * Lets the screen finish loading, then writes a PNG to {@code target}.
     * "Finished loading" means document ready, MUI skeletons/spinners gone
     * (bounded by {@code settleWait}), then a short pause for MUI fade and
     * slide transitions. Every wait is non-fatal, so a permanent shimmer costs
     * a few seconds, not the test.
     *
     * Writing to disk rather than returning base64 keeps screenshots out of the
     * JVM heap - Extent holds every log entry in memory until flush, and a few
     * hundred embedded PNGs were enough to exhaust the machine.
     *
     * @return true if the file was written
     */
    public static boolean captureSettledToFile(File target, Duration settleWait) {
        if (!DriverManager.isInitialised()) {
            return false;
        }
        try {
            WaitUtils.waitForDocumentReady();
            WaitUtils.waitForDataToSettle(settleWait);
            WaitUtils.sleepQuietly(Duration.ofMillis(ConfigManager.getInt("screenshot.animation.millis")));
        } catch (RuntimeException e) {
            Log.warn("Screen did not settle before the screenshot: " + e.getMessage());
        }
        try {
            File source = ((TakesScreenshot) DriverManager.get()).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(source, target);
            return true;
        } catch (IOException | RuntimeException e) {
            Log.warn("Could not capture screenshot: " + e.getMessage());
            return false;
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
