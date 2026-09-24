package com.mywehr.utils;

import com.mywehr.driver.DriverManager;
import com.mywehr.enums.WaitStrategy;
import com.mywehr.exceptions.FrameworkException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * Locator strategies for this application's Material-UI component set.
 *
 * WHY THIS CLASS EXISTS
 * ---------------------
 * The app ships no data-testid attributes, and MUI generates input ids at
 * render time (id="_r_f_", id="_r_g_"...). Those ids change between renders
 * and between builds, so binding tests to them guarantees breakage.
 *
 * Three stable strategies are used instead, in order of preference:
 *
 *   1. The name attribute, where the app sets one. The login form and the
 *      Register-New-Employee dialog both do (tenantCode, email, password,
 *      date_of_birth, employee_id ...), and it is the most robust hook here.
 *
 *   2. The visible label, resolved through the MuiFormControl wrapper that
 *      MUI always renders around a label + input pair. Labels are what a user
 *      reads, so a label-driven locator breaks only when the UI genuinely
 *      changes - which is exactly when a test should break.
 *
 *   3. ARIA roles (combobox, option, dialog, tab) for portalled widgets.
 *
 * PORTALS
 * -------
 * MUI renders menus, dialogs and autocomplete dropdowns into a portal at the
 * end of <body>, NOT inside the form they belong to. Any locator scoped to a
 * form will silently miss them, so the option helpers below deliberately
 * search from the document root.
 */
public final class MuiUtils {

    // -------------------------------------------------------- shared locators

    public static final By DIALOG = By.cssSelector(".MuiDialog-root");
    public static final By DIALOG_TITLE = By.cssSelector(".MuiDialogTitle-root");
    public static final By DRAWER = By.cssSelector(".MuiDrawer-root .MuiDrawer-paper");
    public static final By BACKDROP = By.cssSelector(".MuiBackdrop-root");
    public static final By LISTBOX = By.cssSelector("[role='listbox'], .MuiAutocomplete-popper");
    public static final By OPTION = By.cssSelector("[role='option']");
    public static final By MENU_ITEM = By.cssSelector("[role='menuitem']");
    public static final By FIELD_ERROR = By.cssSelector(".MuiFormHelperText-root.Mui-error");
    public static final By ALERT = By.cssSelector(".MuiAlert-root, [role='alert']");

    public static final By DATA_GRID = By.cssSelector(".MuiDataGrid-root");
    public static final By DATA_GRID_ROW = By.cssSelector(".MuiDataGrid-row");
    public static final By DATA_GRID_OVERLAY = By.cssSelector(".MuiDataGrid-overlay");

    private MuiUtils() {
    }

    // ------------------------------------------------ label-driven locators

    /**
     * Input resolved from its visible label.
     *
     * MUI renders:
     *   div.MuiFormControl-root
     *     label            "Full Name *"
     *     div.MuiInputBase-root
     *       input
     *
     * so walking up to the FormControl and back down to the control is stable
     * regardless of the generated id. The label is matched on its leading text
     * so that the required-field asterisk does not have to be spelled out.
     */
    public static By inputByLabel(String label) {
        return By.xpath(formControlXPath(label) + "//input");
    }

    public static By textAreaByLabel(String label) {
        return By.xpath(formControlXPath(label) + "//textarea[not(@aria-hidden='true')]");
    }

    /** The clickable combobox surface of a MUI Select / Autocomplete. */
    public static By selectByLabel(String label) {
        return By.xpath(formControlXPath(label) + "//*[@role='combobox']");
    }

    /** Inline validation message rendered under a labelled field. */
    public static By errorTextByLabel(String label) {
        return By.xpath(formControlXPath(label)
                + "//p[contains(@class,'MuiFormHelperText-root') and contains(@class,'Mui-error')]");
    }

    private static String formControlXPath(String label) {
        String normalised = escapeForXPath(label);
        return "//label[starts-with(normalize-space(.)," + normalised + ")]"
                + "/ancestor::div[contains(@class,'MuiFormControl-root')][1]";
    }

    // ------------------------------------------------- scoped field locators

    /** XPath prefix restricting a match to the open dialog. */
    public static final String IN_DIALOG = "//div[contains(@class,'MuiDialog-root')]";

    /** XPath prefix restricting a match to the open drawer. */
    public static final String IN_DRAWER = "//div[contains(@class,'MuiDrawer-paper')]";

    /**
     * Field locators scoped to an overlay.
     *
     * NOT OPTIONAL - these prevent a real and very confusing bug.
     *
     * An unscoped label locator searches the whole document, and this app
     * renders the SAME labels in two places at once: the Employee Directory
     * has Department / Branch / Designation / Employment Type / Role filters,
     * and the Register New Employee dialog that opens on top of it has fields
     * with exactly those labels. The page's filter comes first in the DOM, so
     * an unscoped locator resolves to the control BEHIND the dialog.
     *
     * The failure that produces is thoroughly misleading: the backdrop blocks
     * the native click, the scripted fallback then successfully operates the
     * hidden filter, the directory underneath re-renders, the dialog remounts,
     * and every value already typed into the form is lost. The test finally
     * fails with "Full name is required" on a form it believes it filled in.
     *
     * The Holiday dialog has the same clash on "Location".
     */
    public static By inputByLabelIn(String scopeXPath, String label) {
        return By.xpath(scopeXPath + formControlXPath(label) + "//input");
    }

    public static By textAreaByLabelIn(String scopeXPath, String label) {
        return By.xpath(scopeXPath + formControlXPath(label)
                + "//textarea[not(@aria-hidden='true')]");
    }

    public static By selectByLabelIn(String scopeXPath, String label) {
        return By.xpath(scopeXPath + formControlXPath(label)
                + "//*[@role='combobox']");
    }

    // ----------------------------------------------------- attribute locators

    public static By inputByName(String name) {
        return By.cssSelector("input[name='" + name + "']");
    }

    // -------------------------------------------------------------- buttons

    /** Button matched on its exact visible text. */
    public static By buttonByText(String text) {
        return By.xpath("//button[normalize-space(.)=" + escapeForXPath(text) + "]");
    }

    /** Button whose text merely contains the fragment - for buttons with badges. */
    public static By buttonContainingText(String text) {
        return By.xpath("//button[contains(normalize-space(.)," + escapeForXPath(text) + ")]");
    }

    public static By tabByText(String text) {
        return By.xpath("//*[@role='tab'][contains(normalize-space(.)," + escapeForXPath(text) + ")]");
    }

    /**
     * Control matched on its accessible name OR its visible text.
     *
     * The employee directory's row actions are icon-only buttons - an eye, a
     * pencil, a key, a bin - whose meaning lives entirely in aria-label/title.
     * A text-only locator finds nothing and the test reports "HR cannot edit
     * employees", which is a wrong answer rather than a failed lookup. The
     * same control may be captioned elsewhere in the app, so both are matched.
     */
    public static By controlByAccessibleName(String name) {
        String escaped = escapeForXPath(name);
        return By.xpath("//*[self::button or @role='button']"
                + "[@aria-label=" + escaped
                + " or @title=" + escaped
                + " or normalize-space(.)=" + escaped
                + " or .//*[@aria-label=" + escaped + " or @title=" + escaped + "]]");
    }

    // ------------------------------------------------ check-in reminder

    /**
     * Closes the "Hello, you haven't checked in yet today" reminder if shown.
     *
     * The app floats this card over every page on a persona's first sign-in
     * of the day, so it appears in the first run each morning and never in a
     * rerun later that day - a textbook source of "fails only sometimes". It
     * swallows clicks aimed at whatever is underneath it. Only its close
     * button is clicked: the card body itself navigates to Daily Check-In.
     *
     * @return true when a reminder was found and closed
     */
    public static boolean dismissCheckInReminderIfShown() {
        Object closed = ((org.openqa.selenium.JavascriptExecutor) DriverManager.get()).executeScript(
                """
                const text = [...document.querySelectorAll('body *')]
                    .find(e => e.children.length === 0
                            && /checked in yet/i.test(e.textContent || ''));
                if (!text) return false;
                let card = text.parentElement;
                while (card && !card.querySelector('button')) card = card.parentElement;
                if (!card || card === document.body) return false;
                card.querySelector('button').click();
                return true;
                """);
        if (Boolean.TRUE.equals(closed)) {
            Log.warn("Closed the daily check-in reminder that was covering the page");
            WaitUtils.sleepQuietly(Duration.ofMillis(400));
            return true;
        }
        return false;
    }

    // ------------------------------------------------------ select handling

    /**
     * Picks an option from a MUI Select.
     *
     * The dropdown is portalled, so the sequence is: click the combobox, wait
     * for the listbox to mount at body level, click the option by its text.
     */
    public static void selectOption(By comboboxLocator, String optionText) {
        Log.step("Selecting '" + optionText + "' from dropdown");
        openDropdown(comboboxLocator);

        By option = By.xpath("//*[@role='option'][normalize-space(.)="
                + escapeForXPath(optionText) + "]");
        if (!ElementUtils.isPresent(option)) {
            List<String> available = availableOptions();
            closeDropdown();
            throw new FrameworkException(
                    "Option '" + optionText + "' is not in the dropdown. Available options: "
                            + available);
        }
        chooseOption(option, optionText);
    }

    /** Picks the first option whose text contains the fragment. */
    public static void selectOptionContaining(By comboboxLocator, String fragment) {
        Log.step("Selecting the option containing '" + fragment + "' from dropdown");
        openDropdown(comboboxLocator);
        By option = By.xpath("(//*[@role='option'][contains(normalize-space(.),"
                + escapeForXPath(fragment) + ")])[1]");
        if (!ElementUtils.isPresent(option)) {
            List<String> available = availableOptions();
            closeDropdown();
            throw new FrameworkException(
                    "No option containing '" + fragment + "' in the dropdown. Available options: "
                            + available);
        }
        chooseOption(option, fragment);
    }

    /**
     * Clicks a menu option and waits for the menu to dismiss.
     *
     * The menu closing is the only observable proof the option was taken; a
     * click that fails to register leaves it open, and every later locator
     * then has to fight a full-screen MUI backdrop. Retried once with the
     * full pointer sequence for the same reason dropdowns need it.
     */
    private static void chooseOption(By option, String description) {
        ElementUtils.click(option);
        if (WaitUtils.isVisibleWithin(LISTBOX, Duration.ofSeconds(3))) {
            Log.warn("The menu stayed open after choosing '" + description
                    + "' - replaying the full pointer sequence");
            if (ElementUtils.isPresent(option)) {
                ElementUtils.clickViaScript(option);
            }
        }
        if (!WaitUtils.waitForInvisibleQuietly(LISTBOX, Duration.ofSeconds(10))) {
            // Leaving an open menu behind would break the next interaction
            // far from here, so close it and report honestly.
            ElementUtils.pressEscape();
            throw new FrameworkException(
                    "The dropdown menu would not close after choosing '" + description + "'");
        }
    }

    /** Opens a dropdown, reads its options, closes it again. */
    public static List<String> readOptions(By comboboxLocator) {
        openDropdown(comboboxLocator);
        List<String> options = availableOptions();
        closeDropdown();
        return options;
    }

    /**
     * Dismisses an open menu without choosing anything.
     *
     * Escape alone is not dependable here - it is delivered to whatever holds
     * focus, and after a scripted open that is not always the menu. Clicking
     * the backdrop is what a user would do and what MUI always listens for, so
     * it is the fallback. An open menu left behind is not a cosmetic problem:
     * its full-screen backdrop intercepts every subsequent click on the page.
     */
    public static void closeDropdown() {
        if (!ElementUtils.isDisplayed(LISTBOX)) {
            return;
        }

        ElementUtils.pressEscape();
        if (WaitUtils.waitForInvisibleQuietly(LISTBOX, Duration.ofSeconds(3))) {
            return;
        }

        Log.warn("The menu ignored Escape - dismissing it through the backdrop");
        if (ElementUtils.isPresent(BACKDROP)) {
            ElementUtils.clickViaScript(ElementUtils.find(BACKDROP, WaitStrategy.PRESENT));
        }
        if (!WaitUtils.waitForInvisibleQuietly(LISTBOX, Duration.ofSeconds(5))) {
            throw new FrameworkException(
                    "An open dropdown menu could not be dismissed - its backdrop will "
                            + "intercept every later interaction on this page");
        }
    }

    /**
     * Opens a MUI Select and waits for its portalled menu.
     *
     * MUI binds the Select to onMouseDown rather than onClick, so a click that
     * does not carry the full pointer sequence leaves the menu shut. The
     * native click is tried first; the scripted fallback replays the whole
     * sequence, which is what actually opens it when the native click is
     * swallowed by an overlay.
     */
    public static void openDropdown(By comboboxLocator) {
        ElementUtils.click(comboboxLocator);
        if (WaitUtils.isVisibleWithin(LISTBOX, Duration.ofSeconds(5))) {
            return;
        }

        Log.warn("The dropdown did not open on the native click - "
                + "replaying the full pointer sequence");
        ElementUtils.clickViaScript(comboboxLocator);
        WaitUtils.waitForVisible(LISTBOX);
    }

    private static List<String> availableOptions() {
        return ElementUtils.findAll(OPTION).stream()
                .map(WebElement::getText)
                .map(String::trim)
                .toList();
    }

    /**
     * Types into a MUI Autocomplete and picks the first suggestion.
     * Used by "Target Employee" on the Apply-for-Leave drawer, which filters
     * server-side as you type.
     */
    public static void searchAndSelect(By autocompleteInput, String query) {
        Log.step("Autocomplete search for '" + query + "'");
        ElementUtils.type(autocompleteInput, query);
        WaitUtils.waitForVisible(LISTBOX);

        // Wait for an option that actually MATCHES the query, then click that
        // one - never "whichever option is first".
        //
        // Clicking the first option is a race with real consequences here:
        // focusing the field opens the list unfiltered, so for a moment option
        // one is simply the first employee in the tenant. Selecting it records
        // leave against the wrong person, the test then looks for a request
        // that was never created, and the failure points at the assertion
        // rather than at the selection that actually went wrong.
        By matching = By.xpath("//*[@role='option'][contains(normalize-space(.),"
                + escapeForXPath(query) + ")]");
        try {
            WaitUtils.fluently(d -> !d.findElements(matching).isEmpty(),
                    Duration.ofSeconds(12),
                    "No autocomplete option matched '" + query + "'");
        } catch (org.openqa.selenium.TimeoutException e) {
            List<String> available = availableOptions();
            closeDropdown();
            throw new FrameworkException(
                    "No autocomplete option matched '" + query + "'. Offered: " + available, e);
        }

        By firstMatch = By.xpath("(//*[@role='option'][contains(normalize-space(.),"
                + escapeForXPath(query) + ")])[1]");
        chooseOption(firstMatch, query);
    }

    // -------------------------------------------------------- dialog handling

    public static boolean isDialogOpen() {
        return ElementUtils.isDisplayed(DIALOG);
    }

    public static void waitForDialog() {
        WaitUtils.waitForVisible(DIALOG);
    }

    /**
     * Clicks a trigger and waits for its dialog, retrying with a scripted
     * click if the first attempt produced nothing.
     *
     * Several of this app's action buttons sit in headers that re-render as
     * their data resolves. A native click dispatched while that is happening
     * is occasionally absorbed - no exception, no dialog, and the test then
     * fails 25 seconds later waiting for a dialog that was never asked for.
     * The scripted retry dispatches the event straight at the element, which
     * React's handler receives regardless of layout timing.
     *
     * The native click is still tried first, deliberately: it is the only one
     * of the two that would catch a genuinely unclickable control.
     */
    public static void openDialogFrom(By trigger) {
        openDialogFrom(ElementUtils.find(trigger, WaitStrategy.CLICKABLE));
    }

    /** Same contract, for a trigger already resolved to an element. */
    public static void openDialogFrom(WebElement trigger) {
        ElementUtils.click(trigger);
        if (WaitUtils.isVisibleWithin(DIALOG, Duration.ofSeconds(6))) {
            return;
        }

        Log.warn("The dialog did not open on the native click - retrying with a scripted click");
        ElementUtils.clickViaScript(trigger);
        WaitUtils.waitForVisible(DIALOG);
    }

    public static void waitForDialogToClose() {
        WaitUtils.waitForInvisible(DIALOG);
    }

    public static String dialogText() {
        return ElementUtils.find(DIALOG, WaitStrategy.VISIBLE).getText();
    }

    /**
     * Clicks a button inside the open dialog.
     *
     * Scoped to the dialog on purpose: "Delete" and "Cancel" also exist on the
     * page underneath, and an unscoped locator picks whichever the DOM returns
     * first - a classic source of tests that pass for the wrong reason.
     */
    public static void clickDialogButton(String buttonText) {
        WebElement dialog = ElementUtils.find(DIALOG, WaitStrategy.VISIBLE);
        WebElement button = dialog.findElement(
                By.xpath(".//button[normalize-space(.)=" + escapeForXPath(buttonText) + "]"));
        ElementUtils.click(button);
    }

    /**
     * Clicks a dialog button that is expected to dismiss the dialog, and
     * confirms it did.
     *
     * A submit that quietly fails to register leaves the dialog open, and the
     * caller then times out waiting for it to close. Retrying once with the
     * full pointer sequence distinguishes "the click did not land" from "the
     * form was rejected" - if the dialog is still open after the retry, the
     * app really did refuse the submission and the caller should inspect the
     * validation messages.
     *
     * @return true when the dialog closed
     */
    public static boolean clickDialogButtonAndWaitForClose(String buttonText) {
        clickDialogButton(buttonText);
        if (!ElementUtils.isDisplayed(DIALOG)
                || waitForDialogToCloseQuietly(Duration.ofSeconds(8))) {
            return true;
        }

        Log.warn("The dialog stayed open after '" + buttonText
                + "' - replaying the full pointer sequence");
        clickDialogButtonViaScript(buttonText);
        return waitForDialogToCloseQuietly(Duration.ofSeconds(10));
    }

    public static void clickDialogButtonViaScript(String buttonText) {
        WebElement dialog = ElementUtils.find(DIALOG, WaitStrategy.VISIBLE);
        WebElement button = dialog.findElement(
                By.xpath(".//button[normalize-space(.)=" + escapeForXPath(buttonText) + "]"));
        ElementUtils.clickViaScript(button);
    }

    private static boolean waitForDialogToCloseQuietly(Duration timeout) {
        try {
            WaitUtils.fluently(d -> d.findElements(DIALOG).stream()
                            .noneMatch(WebElement::isDisplayed),
                    timeout, "The dialog never closed");
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }

    /**
     * Confirms a destructive action.
     *
     * The app uses a MUI confirmation dialog rather than window.confirm(), so
     * no driver alert handling is involved.
     */
    public static void confirmDestructiveAction() {
        waitForDialog();
        clickDialogButton("Delete");
        waitForDialogToClose();
    }

    // ------------------------------------------------------ DataGrid handling

    public static void waitForGrid() {
        WaitUtils.waitForVisible(DATA_GRID);
        WaitUtils.waitForDataToSettle();
    }

    public static int gridRowCount() {
        return ElementUtils.count(DATA_GRID_ROW);
    }

    /** Row containing the given text in any cell. */
    public static By gridRowContaining(String text) {
        return By.xpath("//div[contains(@class,'MuiDataGrid-row')]"
                + "[.//div[contains(@class,'MuiDataGrid-cell')]"
                + "[contains(normalize-space(.)," + escapeForXPath(text) + ")]]");
    }

    /** A specific column cell within the row identified by rowText. */
    public static By gridCell(String rowText, String columnField) {
        return By.xpath("//div[contains(@class,'MuiDataGrid-row')]"
                + "[.//div[contains(@class,'MuiDataGrid-cell')]"
                + "[contains(normalize-space(.)," + escapeForXPath(rowText) + ")]]"
                + "//div[@data-field=" + escapeForXPath(columnField) + "]");
    }

    public static boolean gridHasRowContaining(String text) {
        return WaitUtils.isVisibleWithin(gridRowContaining(text), Duration.ofSeconds(10));
    }

    /**
     * Reads the total row count from the pagination footer ("1-6 of 6").
     * More reliable than counting DOM rows, which only holds the current page
     * and, for a virtualised grid, only the rendered window of it.
     */
    public static int gridTotalFromPagination() {
        By displayedRows = By.cssSelector(".MuiTablePagination-displayedRows");
        if (!ElementUtils.isPresent(displayedRows)) {
            return gridRowCount();
        }
        String text = ElementUtils.getText(displayedRows);
        String[] parts = text.split("of");
        if (parts.length < 2) {
            return gridRowCount();
        }
        return Integer.parseInt(parts[1].replaceAll("[^0-9]", "").trim());
    }

    // ------------------------------------------------------------- utilities

    /**
     * Makes a Java string safe as an XPath literal.
     *
     * XPath 1.0 has no escape character, so a value containing both quote
     * kinds has to be assembled with concat(). Role labels here really do
     * contain apostrophes and ampersands, so this is not theoretical.
     */
    public static String escapeForXPath(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }
        StringBuilder builder = new StringBuilder("concat(");
        String[] parts = value.split("'", -1);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                builder.append(", \"'\", ");
            }
            builder.append("'").append(parts[i]).append("'");
        }
        return builder.append(")").toString();
    }

    /** Dismisses any open overlay so the next interaction is not intercepted. */
    public static void dismissOverlays() {
        if (ElementUtils.isDisplayed(DIALOG) || ElementUtils.isDisplayed(DRAWER)) {
            ElementUtils.pressEscape();
            WaitUtils.sleepQuietly(Duration.ofMillis(400));
        }
        if (ElementUtils.isDisplayed(BACKDROP)) {
            DriverManager.get();
            ElementUtils.pressEscape();
        }
    }
}
