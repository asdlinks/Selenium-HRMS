package com.mywehr.pages.settings;

import com.mywehr.config.ConfigManager;
import com.mywehr.enums.AppModule;
import com.mywehr.enums.SettingsPanel;
import com.mywehr.enums.WaitStrategy;
import com.mywehr.pages.base.BasePage;
import com.mywehr.utils.ElementUtils;
import com.mywehr.utils.Log;
import com.mywehr.utils.MuiUtils;
import com.mywehr.utils.WaitUtils;
import org.openqa.selenium.By;

import java.time.Duration;
import java.util.List;

/**
 * Settings.
 *
 * One route, many panels. The left rail is built from the signed-in user's
 * permissions, so which entries exist is the RBAC assertion here - an
 * Administrator sees twelve, HR sees four.
 *
 * TWO THINGS THAT BITE HERE
 * -------------------------
 *  1. The rail entry's text lives in a SPAN, but the click handler is on an
 *     ancestor row. Clicking the span does nothing, so every activation
 *     resolves the clickable ancestor first.
 *
 *  2. The rail label stays on screen whether or not the panel opened, so
 *     "is the label visible?" cannot verify a panel opened. Verification goes
 *     through the panel's own content heading instead, which differs from the
 *     rail label for half of them ("Account Security" opens "Password
 *     Management").
 */
public class SettingsPage extends BasePage {

    private static final By RAIL_ENTRIES =
            By.xpath("//main//*[self::li or contains(@class,'MuiListItemButton-root')]"
                    + "//span[normalize-space(text())]");

    /** Heading rendered by the currently open panel, on the right-hand side. */
    private static final By PANEL_HEADING =
            By.cssSelector("main h6, main .MuiTypography-h6, main h5");

    @Override
    public String landingMarker() {
        return AppModule.SETTINGS.landingMarker();
    }

    @Override
    public String route() {
        return AppModule.SETTINGS.route();
    }

    // ------------------------------------------------------ panel visibility

    public boolean hasPanel(SettingsPanel panel) {
        return ElementUtils.isPresent(railLabelLocator(panel.label()));
    }

    public boolean hasPanel(String panelLabel) {
        return ElementUtils.isPresent(railLabelLocator(panelLabel));
    }

    /** Every rail label currently rendered, in order. */
    public List<String> visiblePanelLabels() {
        return ElementUtils.findAll(RAIL_ENTRIES).stream()
                .map(org.openqa.selenium.WebElement::getText)
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .distinct()
                .toList();
    }

    /** Group headers (ORGANIZATION, ATTENDANCE, PAYROLL, SECURITY, ADVANCED). */
    public boolean hasGroup(String groupName) {
        return displaysTextImmediately(groupName);
    }

    // -------------------------------------------------------------- opening

    /**
     * Activates a rail entry and waits for the panel to actually render.
     *
     * For an entry that navigates away (Audit &amp; Compliance) the caller
     * should use {@link #activateNavigationEntry(SettingsPanel)} instead.
     */
    public SettingsPage openPanel(SettingsPanel panel) {
        Log.step("Opening the '" + panel.label() + "' settings panel");

        if (panel.isNavigationLink()) {
            return activateNavigationEntry(panel);
        }

        // Activated with a scripted click, and retried once.
        //
        // The rail rows are plain divs carrying a React onClick. For entries
        // low in the rail - the ADVANCED group - a native click lands while
        // the row is still settling from scrollIntoView and is absorbed
        // without the handler firing: the row takes the selected highlight but
        // the right-hand panel never changes. A scripted click dispatches the
        // event straight at the element, which React's synthetic handler
        // receives regardless of where the row happens to be on screen.
        for (int attempt = 1; attempt <= 2; attempt++) {
            ElementUtils.clickViaScript(
                    ElementUtils.find(clickableRailEntry(panel.label()), WaitStrategy.PRESENT));
            if (waitForPanelHeading(panel.contentHeading())) {
                break;
            }
            Log.warn("The '" + panel.label() + "' panel did not open on attempt "
                    + attempt + " - retrying");
        }

        WaitUtils.waitForDataToSettle();
        // Long panels render below the fold; scrolling up keeps the heading in
        // view so assertions read what they expect to read.
        ElementUtils.scrollToTop();
        return this;
    }

    /** Activates a rail entry that routes away from /settings. */
    public SettingsPage activateNavigationEntry(SettingsPanel panel) {
        Log.step("Activating the '" + panel.label() + "' entry (navigates to "
                + panel.navigatesTo() + ")");
        ElementUtils.clickViaScript(
                ElementUtils.find(clickableRailEntry(panel.label()), WaitStrategy.PRESENT));
        WaitUtils.waitForAppShell();
        WaitUtils.waitForDataToSettle();
        return this;
    }

    /** Heading of the panel currently displayed on the right. */
    public String openPanelHeading() {
        return ElementUtils.isPresent(PANEL_HEADING) ? ElementUtils.getText(PANEL_HEADING) : "";
    }

    /**
     * True once the named panel is genuinely on screen.
     *
     * Compares the rendered panel heading rather than searching page text -
     * a text search would match the rail label and pass even if the click
     * never took effect.
     */
    public boolean isPanelOpen(SettingsPanel panel) {
        if (panel.isNavigationLink()) {
            return currentPath().startsWith(panel.navigatesTo());
        }
        return waitForPanelHeading(panel.contentHeading());
    }

    private boolean waitForPanelHeading(String expectedHeading) {
        try {
            WaitUtils.fluently(d -> openPanelHeading().equals(expectedHeading),
                    ConfigManager.explicitWait(),
                    "The panel heading never became '" + expectedHeading + "'");
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            Log.warn("Panel heading is '" + openPanelHeading()
                    + "', expected '" + expectedHeading + "'");
            return false;
        }
    }

    // ------------------------------------------- Roles & Permissions panel

    /** Role names listed on the Roles &amp; Permissions panel. */
    public boolean listsRole(String roleName) {
        return ElementUtils.waitForPageText(roleName, Duration.ofSeconds(12));
    }

    /** Expands a role accordion to reveal its permission grants. */
    public SettingsPage expandRole(String roleName) {
        Log.step("Expanding the '" + roleName + "' role");
        ElementUtils.click(By.xpath("//*[contains(@class,'MuiAccordionSummary-root')]"
                + "[contains(normalize-space(.)," + MuiUtils.escapeForXPath(roleName) + ")]"));
        WaitUtils.waitForDataToSettle();
        return this;
    }

    /** User count chip beside a role, e.g. 1 from "HR Administrator 1 user". */
    public int userCountFor(String roleName) {
        Object result = ((org.openqa.selenium.JavascriptExecutor) driver()).executeScript(
                """
                const role = arguments[0];
                const leaf = [...document.querySelectorAll('main div,main span,main p,main h6')]
                    .find(e => e.children.length === 0
                            && (e.textContent || '').trim() === role);
                if (!leaf) return null;
                let node = leaf;
                for (let hop = 0; hop < 3 && node; hop++) {
                    node = node.parentElement;
                    if (!node) break;
                    const match = (node.innerText || '').match(/(\\d+)\\s+users?/);
                    if (match) return match[1];
                }
                return null;
                """, roleName);
        return result == null ? -1 : Integer.parseInt(result.toString());
    }

    // ------------------------------------------------------------- locators

    /** The span carrying the rail label - used for presence checks. */
    private By railLabelLocator(String label) {
        return By.xpath("//main//*[normalize-space(text())="
                + MuiUtils.escapeForXPath(label) + "]");
    }

    /**
     * The clickable row that owns the rail label.
     *
     * The label itself is an inert span; MUI binds the handler to the list-item
     * row above it. Falls back to the label when no such ancestor exists, so a
     * markup change degrades instead of throwing.
     */
    private By clickableRailEntry(String label) {
        String escaped = MuiUtils.escapeForXPath(label);
        By clickableAncestor = By.xpath(
                "//main//*[normalize-space(text())=" + escaped + "]"
                        + "/ancestor-or-self::*[self::li or self::button or @role='button'"
                        + " or contains(@class,'MuiListItemButton-root')][1]");
        return ElementUtils.isPresent(clickableAncestor)
                ? clickableAncestor
                : railLabelLocator(label);
    }

    /** Exposed so tests can report what a persona actually sees. */
    public String currentPanelHeadingOrBlank() {
        return ElementUtils.isDisplayed(PANEL_HEADING)
                ? ElementUtils.find(PANEL_HEADING, WaitStrategy.VISIBLE).getText().trim()
                : "";
    }
}
