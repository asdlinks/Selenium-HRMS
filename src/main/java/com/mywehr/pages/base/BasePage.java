package com.mywehr.pages.base;

import com.mywehr.config.ConfigManager;
import com.mywehr.driver.DriverManager;
import com.mywehr.enums.AppModule;
import com.mywehr.utils.ElementUtils;
import com.mywehr.utils.Log;
import com.mywehr.utils.MuiUtils;
import com.mywehr.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.time.Duration;

/**
 * Shared behaviour for every page object.
 *
 * Page objects expose intent ("openHolidayCalendar", "approveLeaveFor") and
 * return other page objects or plain values. They never assert - assertions
 * belong to tests, so the same page object can serve a positive and a negative
 * test without contortion.
 */
public abstract class BasePage {

    protected static final By PAGE_HEADING = By.cssSelector("main h1, main h4, main .MuiTypography-h4");

    protected WebDriver driver() {
        return DriverManager.get();
    }

    /**
     * Text that proves this specific page rendered. Subclasses return the
     * heading or subtitle unique to them.
     */
    public abstract String landingMarker();

    /** Route this page lives at, e.g. {@code /employees}. */
    public abstract String route();

    // --------------------------------------------------------- navigation

    /** Hard-navigates to this page's route and waits for the shell to boot. */
    public void open() {
        String url = ConfigManager.urlFor(route());
        Log.step("Navigating to " + url);
        driver().get(url);
        WaitUtils.waitForAppShell();
        WaitUtils.waitForDataToSettle();
    }

    public static void navigateTo(AppModule module) {
        String url = ConfigManager.urlFor(module.route());
        Log.step("Navigating to " + module.name() + " (" + url + ")");
        DriverManager.get().get(url);
        WaitUtils.waitForAppShell();
        WaitUtils.waitForDataToSettle();
        MuiUtils.dismissCheckInReminderIfShown();
    }

    // ---------------------------------------------------------- page state

    public String currentUrl() {
        return driver().getCurrentUrl();
    }

    /** Path portion of the current URL, e.g. {@code /dashboard}. */
    public String currentPath() {
        String url = currentUrl();
        String base = ConfigManager.baseUrl();
        return url.startsWith(base) ? url.substring(base.length()) : url;
    }

    public String pageTitle() {
        return driver().getTitle();
    }

    public String heading() {
        return ElementUtils.isPresent(PAGE_HEADING) ? ElementUtils.getText(PAGE_HEADING) : "";
    }

    /**
     * True when this page's landing marker is on screen.
     *
     * Deliberately content-based rather than URL-based: in a client-side
     * routed app the URL updates before the view finishes fetching, so a URL
     * check alone would pass while the page is still blank.
     */
    public boolean isLoaded() {
        return ElementUtils.waitForPageText(landingMarker(), ConfigManager.explicitWait());
    }

    public boolean isLoadedWithin(Duration timeout) {
        return ElementUtils.waitForPageText(landingMarker(), timeout);
    }

    /** Visible text of the main region - used for coarse content assertions. */
    public String contentText() {
        return ElementUtils.mainContentText();
    }

    public boolean displaysText(String text) {
        return ElementUtils.waitForPageText(text, Duration.ofSeconds(10));
    }

    public boolean displaysTextImmediately(String text) {
        return ElementUtils.pageContainsText(text);
    }

    // ------------------------------------------------------------- helpers

    protected void clickButton(String buttonText) {
        Log.step("Clicking button: " + buttonText);
        ElementUtils.click(MuiUtils.buttonByText(buttonText));
    }

    protected boolean hasButton(String buttonText) {
        return ElementUtils.isDisplayed(MuiUtils.buttonByText(buttonText));
    }

    protected void waitForSettled() {
        WaitUtils.waitForDataToSettle();
    }

    /**
     * Reads the numeric value of one of the KPI cards at the top of a module
     * page ("Total Employees 5", "Departments 2", "Total Workforce 5" ...).
     *
     * The cards are plain nested divs with no class hook and no fixed
     * label/value nesting, so this walks up from the label to the nearest
     * ancestor that also contains a number and extracts it. Doing that in one
     * scripted pass is both faster and far less brittle than an XPath that has
     * to guess the card's exact shape.
     *
     * @return the card value, or -1 when no such card is on screen
     */
    public int readKpiCard(String cardLabel) {
        Object result = ((org.openqa.selenium.JavascriptExecutor) driver()).executeScript(
                """
                const label = arguments[0];
                const leaves = [...document.querySelectorAll('div,span,p,h6')]
                    .filter(e => e.children.length === 0
                              && (e.textContent || '').trim() === label);
                for (const leaf of leaves) {
                    let node = leaf;
                    for (let hop = 0; hop < 4 && node; hop++) {
                        node = node.parentElement;
                        if (!node) break;
                        const text = (node.innerText || '').replace(label, ' ');
                        const match = text.match(/\\b\\d[\\d,]*\\b/);
                        if (match) return match[0].replace(/,/g, '');
                    }
                }
                return null;
                """, cardLabel);

        if (result == null) {
            Log.warn("KPI card not found on screen: " + cardLabel);
            return -1;
        }
        return Integer.parseInt(result.toString());
    }

    /**
     * Reads a KPI card once it has stopped changing.
     *
     * The cards render with a placeholder and are filled in when their fetch
     * resolves, so a single read taken straight after navigation can capture
     * the placeholder. A baseline captured that way makes a later
     * "the count must not have changed" assertion fail against a number that
     * was never real. Two consecutive equal reads are taken as settled.
     */
    public int readSettledKpiCard(String cardLabel) {
        try {
            return WaitUtils.fluently(d -> {
                int first = readKpiCard(cardLabel);
                if (first < 0) {
                    return null;
                }
                WaitUtils.sleepQuietly(Duration.ofMillis(400));
                return first == readKpiCard(cardLabel) ? first : null;
            }, ConfigManager.explicitWait(), "KPI '" + cardLabel + "' never settled");
        } catch (org.openqa.selenium.TimeoutException e) {
            Log.warn("KPI '" + cardLabel + "' did not settle - using a single read");
            return readKpiCard(cardLabel);
        }
    }

    /**
     * Polls a KPI card until it reports the expected value.
     *
     * Counters are refetched after a mutation, so reading once immediately
     * after a save races the refresh.
     */
    public boolean waitForKpiCard(String cardLabel, int expectedValue) {
        try {
            WaitUtils.fluently(d -> readKpiCard(cardLabel) == expectedValue,
                    ConfigManager.explicitWait(),
                    "KPI '" + cardLabel + "' never reached " + expectedValue);
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            Log.warn("KPI '" + cardLabel + "' stayed at " + readKpiCard(cardLabel)
                    + ", expected " + expectedValue);
            return false;
        }
    }
}
