package com.mywehr.utils;

import com.mywehr.config.ConfigManager;
import com.mywehr.driver.DriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * Wait helpers tuned for this specific SPA.
 *
 * Two app behaviours drive everything here:
 *
 *  1. A full page load paints a "Loading Mywe HRMS..." splash before React
 *     mounts. Asserting on content before that clears gives false negatives,
 *     so every navigation funnels through {@link #waitForAppShell()}.
 *
 *  2. Route changes are client-side. The URL updates immediately while the new
 *     view is still fetching, so "URL changed" is never sufficient proof that
 *     a page loaded - content must be asserted too.
 */
public final class WaitUtils {

    /** Text shown by the pre-React bootstrap splash. */
    private static final String SPLASH_TEXT = "Loading Mywe HRMS";

    private WaitUtils() {
    }

    private static WebDriver driver() {
        return DriverManager.get();
    }

    public static WebDriverWait webDriverWait() {
        return webDriverWait(ConfigManager.explicitWait());
    }

    public static WebDriverWait webDriverWait(Duration timeout) {
        WebDriverWait wait = new WebDriverWait(driver(), timeout, ConfigManager.pollingInterval());
        wait.ignoring(StaleElementReferenceException.class);
        return wait;
    }

    /**
     * FluentWait that tolerates the two exceptions a re-rendering React tree
     * throws most often while a view settles.
     */
    public static <T> T fluently(Function<WebDriver, T> condition, Duration timeout, String description) {
        return new FluentWait<>(driver())
                .withTimeout(timeout)
                .pollingEvery(ConfigManager.pollingInterval())
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class)
                .withMessage(description)
                .until(condition);
    }

    // ----------------------------------------------------------- app readiness

    /**
     * Blocks until document.readyState is complete AND the bootstrap splash has
     * been replaced by the real application shell.
     */
    public static void waitForAppShell() {
        waitForDocumentReady();
        waitForSplashToClear();
    }

    public static void waitForDocumentReady() {
        try {
            fluently(d -> "complete".equals(
                            ((JavascriptExecutor) d).executeScript("return document.readyState")),
                    ConfigManager.pageLoadTimeout(),
                    "document.readyState never reached 'complete'");
        } catch (TimeoutException e) {
            Log.warn("document.readyState did not reach 'complete' - continuing anyway");
        }
    }

    public static void waitForSplashToClear() {
        try {
            fluently(d -> {
                        String body = d.findElement(By.tagName("body")).getText();
                        return !body.contains(SPLASH_TEXT);
                    },
                    ConfigManager.spaSettleWait(),
                    "Bootstrap splash '" + SPLASH_TEXT + "' never cleared");
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    "The application never finished booting - the '" + SPLASH_TEXT
                            + "' splash was still on screen after "
                            + ConfigManager.spaSettleWait().getSeconds() + "s.", e);
        }
    }

    /**
     * Waits for MUI skeleton placeholders and progress bars to disappear.
     * Used after triggering a data fetch inside an already-mounted view.
     */
    public static void waitForDataToSettle() {
        By loaders = By.cssSelector(".MuiSkeleton-root, .MuiCircularProgress-root, .MuiLinearProgress-root");
        Duration timeout = ConfigManager.getDuration("data.settle.wait");
        try {
            fluently(d -> d.findElements(loaders).stream().noneMatch(WebElement::isDisplayed),
                    timeout, "Loading indicators never cleared");
        } catch (TimeoutException e) {
            // Deliberately non-fatal and deliberately short. This is an
            // optimisation that lets the following assertion read settled
            // content, not a correctness gate - the assertions do their own
            // polling. Some charts keep a shimmer mounted for good, and paying
            // the full explicit wait here on every navigation would add minutes
            // to the suite without catching anything.
            Log.warn("Loading indicators still visible after "
                    + timeout.getSeconds() + "s - continuing");
        }
    }

    // ------------------------------------------------------------- conditions

    public static WebElement waitForVisible(By locator) {
        return webDriverWait().until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForPresent(By locator) {
        return webDriverWait().until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    public static WebElement waitForClickable(By locator) {
        return webDriverWait().until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static boolean waitForInvisible(By locator) {
        return webDriverWait().until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public static List<WebElement> waitForAllVisible(By locator) {
        return webDriverWait().until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    public static boolean waitForTextPresent(By locator, String text) {
        return webDriverWait().until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    public static boolean waitForUrlContains(String fragment) {
        return webDriverWait().until(ExpectedConditions.urlContains(fragment));
    }

    /**
     * True when the element shows up inside the timeout, false when it does
     * not. Never throws - this is the primitive negative assertions are built
     * on, where "absent" is the expected outcome rather than a failure.
     */
    public static boolean isVisibleWithin(By locator, Duration timeout) {
        try {
            new WebDriverWait(driver(), timeout, ConfigManager.pollingInterval())
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * True when the element goes away inside the timeout, false when it does
     * not. The non-throwing counterpart of {@link #waitForInvisible(By)}, for
     * callers that want to handle the timeout themselves.
     */
    public static boolean waitForInvisibleQuietly(By locator, Duration timeout) {
        try {
            new WebDriverWait(driver(), timeout, ConfigManager.pollingInterval())
                    .until(ExpectedConditions.invisibilityOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    public static void sleepQuietly(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
