package com.mywehr.utils;

import com.mywehr.driver.DriverManager;
import com.mywehr.enums.WaitStrategy;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.time.Duration;
import java.util.List;

/**
 * Thin, intent-revealing wrapper over raw WebDriver calls.
 *
 * Every interaction routes through here so that waiting, scrolling and the
 * JS-click fallback are applied uniformly. MUI overlays (drawers, dialog
 * backdrops, sticky headers) intercept clicks often enough that retrying
 * through JavaScript is the difference between a stable suite and a flaky one.
 */
public final class ElementUtils {

    private ElementUtils() {
    }

    private static WebDriver driver() {
        return DriverManager.get();
    }

    private static JavascriptExecutor js() {
        return (JavascriptExecutor) driver();
    }

    // ------------------------------------------------------------- finding

    public static WebElement find(By locator, WaitStrategy strategy) {
        return switch (strategy) {
            case VISIBLE -> WaitUtils.waitForVisible(locator);
            case CLICKABLE -> WaitUtils.waitForClickable(locator);
            case PRESENT -> WaitUtils.waitForPresent(locator);
            case INVISIBLE -> throw new IllegalArgumentException(
                    "INVISIBLE cannot return an element - use WaitUtils.waitForInvisible()");
            case NONE -> driver().findElement(locator);
        };
    }

    public static WebElement find(By locator) {
        return find(locator, WaitStrategy.VISIBLE);
    }

    public static List<WebElement> findAll(By locator) {
        return driver().findElements(locator);
    }

    public static boolean isDisplayed(By locator) {
        try {
            return driver().findElement(locator).isDisplayed();
        } catch (NoSuchElementException | org.openqa.selenium.StaleElementReferenceException e) {
            return false;
        }
    }

    public static boolean isPresent(By locator) {
        return !driver().findElements(locator).isEmpty();
    }

    public static int count(By locator) {
        return driver().findElements(locator).size();
    }

    // ------------------------------------------------------------- clicking

    public static void click(By locator) {
        WebElement element = find(locator, WaitStrategy.CLICKABLE);
        click(element);
    }

    public static void click(WebElement element) {
        scrollIntoView(element);
        try {
            element.click();
        } catch (ElementClickInterceptedException e) {
            // A MUI backdrop, sticky app bar or a toast animating out is sitting
            // over the target. A scripted click bypasses hit-testing entirely.
            Log.warn("Native click intercepted - falling back to a scripted click");
            clickViaScript(element);
        }
    }

    /**
     * Scripted click that emulates the full event sequence a real pointer
     * produces: pointerdown, mousedown, pointerup, mouseup, click.
     *
     * WHY NOT element.click()
     * -----------------------
     * A bare element.click() dispatches ONLY a click event, and several MUI
     * components never see it. MUI's Select opens its menu from onMouseDown,
     * not onClick, so element.click() on a dropdown does nothing at all - no
     * error, no menu, and a test that then waits for the listbox fails
     * twenty-five seconds later for a reason that looks nothing like the
     * cause. Dispatching the whole sequence makes the scripted path behave
     * like the native one for every component in this app.
     */
    public static void clickViaScript(WebElement element) {
        js().executeScript(
                """
                const element = arguments[0];
                const rect = element.getBoundingClientRect();
                const x = rect.left + rect.width / 2;
                const y = rect.top + rect.height / 2;
                const options = {
                    bubbles: true, cancelable: true, composed: true,
                    view: window, button: 0, buttons: 1,
                    clientX: x, clientY: y
                };
                element.dispatchEvent(new PointerEvent('pointerdown', options));
                element.dispatchEvent(new MouseEvent('mousedown', options));
                element.dispatchEvent(new PointerEvent('pointerup',
                    { ...options, buttons: 0 }));
                element.dispatchEvent(new MouseEvent('mouseup',
                    { ...options, buttons: 0 }));
                element.dispatchEvent(new MouseEvent('click',
                    { ...options, buttons: 0 }));
                """, element);
    }

    public static void clickViaScript(By locator) {
        clickViaScript(find(locator, WaitStrategy.PRESENT));
    }

    // -------------------------------------------------------------- typing

    /**
     * Clears and types into a controlled React input, then verifies it took.
     *
     * Two problems this works around, both specific to controlled components:
     *
     *  1. clear() alone is unreliable - it can empty the DOM value without
     *     raising an input event, so React re-renders the old value straight
     *     back. A select-all + delete raises real key events instead.
     *
     *  2. The click that focuses the field is sometimes absorbed mid-render,
     *     and every subsequent keystroke goes nowhere. Silently typing into
     *     the void turns into a confusing assertion failure several steps
     *     later, so the value is read back and, if it did not stick, set
     *     through React's own value setter.
     */
    public static void type(By locator, String text) {
        WebElement element = find(locator, WaitStrategy.VISIBLE);
        scrollIntoView(element);
        String value = text == null ? "" : text;

        try {
            element.click();
            element.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            if (!value.isEmpty()) {
                element.sendKeys(value);
            }
        } catch (ElementNotInteractableException e) {
            // Intercepted: something is over the field. Not interactable: the
            // field is on screen but refusing keystrokes - a MUI Autocomplete
            // inside a drawer that is still animating in does exactly this.
            // Either way the value can still be delivered through React below.
            Log.warn("Could not type into the field natively (" + e.getClass().getSimpleName()
                    + ") - setting its value directly");
        }

        if (!value.equals(currentValueOf(element))) {
            Log.warn("Typed text did not register on the field - "
                    + "falling back to a React-safe value assignment");
            setValueViaReact(element, value);
        }
    }

    private static String currentValueOf(WebElement element) {
        String value = element.getDomProperty("value");
        return value == null ? "" : value;
    }

    /**
     * Sets an input's value the way React itself does.
     *
     * Assigning element.value directly is invisible to React: it caches the
     * previous value on the DOM node and skips the update. Calling the
     * prototype's native setter and then dispatching a bubbling input event is
     * what makes React's synthetic onChange fire and the component state
     * actually change.
     */
    public static void setValueViaReact(WebElement element, String value) {
        js().executeScript(
                """
                const element = arguments[0];
                const value = arguments[1];
                const prototype = element.tagName === 'TEXTAREA'
                    ? window.HTMLTextAreaElement.prototype
                    : window.HTMLInputElement.prototype;
                const setter = Object.getOwnPropertyDescriptor(prototype, 'value').set;
                setter.call(element, value);
                element.dispatchEvent(new Event('input', { bubbles: true }));
                element.dispatchEvent(new Event('change', { bubbles: true }));
                """, element, value);
    }

    public static void setValueViaReact(By locator, String value) {
        setValueViaReact(find(locator, WaitStrategy.VISIBLE), value);
    }

    /**
     * Sets a native date input.
     *
     * Chrome renders these as mm/dd/yyyy segments, so the happy path types
     * bare digits into the segments. If that does not land - the segment order
     * follows the browser locale, which a CI image may not share with a
     * developer's machine - the value is set directly, where a date input
     * requires the ISO form regardless of how it is displayed.
     *
     * @param mmddyyyy date formatted for the en-US browser locale
     */
    public static void typeIntoDateField(By locator, String mmddyyyy) {
        WebElement element = find(locator, WaitStrategy.VISIBLE);
        scrollIntoView(element);

        try {
            element.click();
            element.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            element.sendKeys(mmddyyyy.replace("/", ""));
        } catch (ElementNotInteractableException e) {
            Log.warn("Could not type into the date field natively ("
                    + e.getClass().getSimpleName() + ")");
        }

        String expectedIso = toIsoDate(mmddyyyy);
        if (!expectedIso.equals(currentValueOf(element))) {
            Log.warn("Date did not register as " + expectedIso
                    + " - setting the input value directly");
            setValueViaReact(element, expectedIso);
        }
    }

    /** Converts mm/dd/yyyy to the yyyy-MM-dd a date input stores internally. */
    private static String toIsoDate(String mmddyyyy) {
        String[] parts = mmddyyyy.split("/");
        if (parts.length != 3) {
            return mmddyyyy;
        }
        return parts[2] + "-" + parts[0] + "-" + parts[1];
    }

    public static String getText(By locator) {
        return find(locator, WaitStrategy.VISIBLE).getText().trim();
    }

    public static String getValue(By locator) {
        String value = find(locator, WaitStrategy.PRESENT).getDomProperty("value");
        return value == null ? "" : value.trim();
    }

    public static String getAttribute(By locator, String attribute) {
        return find(locator, WaitStrategy.PRESENT).getDomAttribute(attribute);
    }

    // ------------------------------------------------------------ scrolling

    public static void scrollIntoView(WebElement element) {
        js().executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center', behavior:'instant'});",
                element);
    }

    public static void scrollIntoView(By locator) {
        scrollIntoView(find(locator, WaitStrategy.PRESENT));
    }

    public static void scrollToTop() {
        js().executeScript("window.scrollTo(0, 0);");
    }

    public static void hover(By locator) {
        WebElement element = find(locator, WaitStrategy.VISIBLE);
        scrollIntoView(element);
        new Actions(driver()).moveToElement(element).perform();
    }

    public static void pressEscape() {
        new Actions(driver()).sendKeys(Keys.ESCAPE).perform();
    }

    // -------------------------------------------------------------- page text

    /** Visible text of the main content region, excluding the chrome. */
    public static String mainContentText() {
        By main = By.tagName("main");
        if (isPresent(main)) {
            return find(main, WaitStrategy.PRESENT).getText();
        }
        return driver().findElement(By.tagName("body")).getText();
    }

    public static String bodyText() {
        return driver().findElement(By.tagName("body")).getText();
    }

    public static boolean pageContainsText(String text) {
        return bodyText().contains(text);
    }

    /**
     * Polls for text rather than reading once - the SPA streams sections in,
     * so a single read right after navigation can miss late-arriving content.
     */
    public static boolean waitForPageText(String text, Duration timeout) {
        try {
            WaitUtils.fluently(d -> d.findElement(By.tagName("body")).getText().contains(text),
                    timeout, "Page never displayed the text: " + text);
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }
}
