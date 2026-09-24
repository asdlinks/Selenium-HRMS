package com.mywehr.pages.timeleave;

import com.mywehr.enums.AppModule;
import com.mywehr.pages.base.BasePage;
import com.mywehr.utils.ElementUtils;
import com.mywehr.utils.Log;
import com.mywehr.utils.MuiUtils;
import com.mywehr.utils.WaitUtils;
import org.openqa.selenium.By;

import java.time.Duration;

/**
 * Holiday Calendar.
 *
 * The holiday list is one of the clearest inter-module signals in the product:
 * adding a holiday here changes the dashboard's "Next Holiday" KPI and the
 * Upcoming Holidays widget, and marks the day off across attendance.
 */
public class HolidayCalendarPage extends BasePage {

    public static final String KPI_HOLIDAYS_THIS_YEAR = "Holidays in";
    public static final String KPI_LOCATIONS = "Locations";

    private static final By ADD_HOLIDAY_BUTTON = MuiUtils.buttonByText("Add Holiday");
    private static final By LOCATION_FILTER = MuiUtils.selectByLabel("Location");

    // Scoped to the dialog - the calendar page behind it has its own "Location"
    // filter, and an unscoped label match would reach past the backdrop.
    private static final By DIALOG_HOLIDAY_NAME =
            MuiUtils.inputByLabelIn(MuiUtils.IN_DIALOG, "Holiday Name");
    private static final By DIALOG_DATE =
            MuiUtils.inputByLabelIn(MuiUtils.IN_DIALOG, "Date");
    private static final By DIALOG_LOCATION =
            MuiUtils.selectByLabelIn(MuiUtils.IN_DIALOG, "Location");

    private static final String SAVE_BUTTON = "Save Holiday";
    private static final String CANCEL_BUTTON = "Cancel";

    @Override
    public String landingMarker() {
        return AppModule.HOLIDAY_CALENDAR.landingMarker();
    }

    @Override
    public String route() {
        return AppModule.HOLIDAY_CALENDAR.route();
    }

    // ------------------------------------------------------------- reading

    public boolean canAddHoliday() {
        return ElementUtils.isDisplayed(ADD_HOLIDAY_BUTTON);
    }

    public boolean hasLocationFilter() {
        return ElementUtils.isDisplayed(LOCATION_FILTER);
    }

    /**
     * Number of holidays this year, from the KPI card.
     *
     * The card renders 0 as a placeholder until the holiday fetch resolves,
     * and that placeholder can hold steady long enough to look settled. So a
     * positive figure is accepted as soon as it appears, and 0 is only
     * reported once the card has had the full wait to change.
     */
    public int holidayCountThisYear() {
        String label = KPI_HOLIDAYS_THIS_YEAR + " " + com.mywehr.utils.DateUtils.currentYear();
        try {
            return WaitUtils.fluently(d -> {
                int count = readKpiCard(label);
                return count > 0 ? count : null;
            }, Duration.ofSeconds(15), "Holiday KPI stayed at zero");
        } catch (org.openqa.selenium.TimeoutException e) {
            return readKpiCard(label);
        }
    }

    public boolean hasHoliday(String name) {
        return ElementUtils.waitForPageText(name, Duration.ofSeconds(12));
    }

    public boolean hasHolidayImmediately(String name) {
        return ElementUtils.pageContainsText(name);
    }

    /**
     * Deletes every holiday an earlier automation run left behind (named
     * "&lt;prefix&gt; Holiday &lt;digits&gt;"). A leftover dated sooner than a new
     * holiday keeps the Dashboard's next-holiday widget pointing at itself.
     */
    public void deleteLeftoverAutomationHolidays(String prefix) {
        open();
        // The cards render after the page shell; reading at once finds none.
        // With no leftovers this costs the full wait, which is the rare case.
        if (!ElementUtils.waitForPageText(prefix + " Holiday", Duration.ofSeconds(10))) {
            return;
        }
        @SuppressWarnings("unchecked")
        java.util.List<String> leftovers = (java.util.List<String>)
                ((org.openqa.selenium.JavascriptExecutor) driver()).executeScript(
                """
                const pattern = new RegExp('^' + arguments[0] + ' Holiday \\\\d+$');
                const names = [...document.querySelectorAll('main div,main span,main p,main h6')]
                    .filter(e => e.children.length === 0)
                    .map(e => (e.textContent || '').trim())
                    .filter(t => pattern.test(t));
                return [...new Set(names)];
                """, prefix);
        for (String name : leftovers) {
            Log.info("Removing leftover automation holiday '" + name + "'");
            if (!deleteIfPresent(name)) {
                Log.warn("Holiday delete is not working - leaving the remaining leftovers in place");
                return;
            }
        }
    }

    /** Name shown on the "Next Holiday" KPI card. */
    public String nextHolidayName() {
        return contentText().lines()
                .dropWhile(line -> !line.trim().equals("Next Holiday"))
                .skip(1)
                .filter(line -> !line.isBlank())
                .findFirst()
                .map(String::trim)
                .orElse("");
    }

    // ------------------------------------------------------------ creating

    public HolidayCalendarPage openAddHolidayDialog() {
        Log.step("Opening the Add New Holiday dialog");
        MuiUtils.openDialogFrom(ADD_HOLIDAY_BUTTON);
        return this;
    }

    public String addHolidayDialogTitle() {
        return ElementUtils.getText(MuiUtils.DIALOG_TITLE);
    }

    public HolidayCalendarPage enterHolidayName(String name) {
        ElementUtils.type(DIALOG_HOLIDAY_NAME, name);
        return this;
    }

    public HolidayCalendarPage enterHolidayDate(String mmddyyyy) {
        ElementUtils.typeIntoDateField(DIALOG_DATE, mmddyyyy);
        return this;
    }

    public HolidayCalendarPage saveHoliday() {
        if (!MuiUtils.clickDialogButtonAndWaitForClose(SAVE_BUTTON)) {
            throw new IllegalStateException(
                    "The holiday was not saved - the dialog is still open. "
                            + "Validation messages: " + validationMessages());
        }
        WaitUtils.waitForDataToSettle();
        return this;
    }

    public HolidayCalendarPage cancelHolidayDialog() {
        MuiUtils.clickDialogButtonAndWaitForClose(CANCEL_BUTTON);
        return this;
    }

    /** Full create flow. */
    public HolidayCalendarPage addHoliday(String name, String mmddyyyy) {
        Log.step("Adding holiday '" + name + "' on " + mmddyyyy);
        openAddHolidayDialog();
        enterHolidayName(name);
        enterHolidayDate(mmddyyyy);
        return saveHoliday();
    }

    /** Inline validation messages raised by the dialog. */
    public java.util.List<String> validationMessages() {
        WaitUtils.sleepQuietly(Duration.ofMillis(600));
        return ElementUtils.findAll(MuiUtils.FIELD_ERROR).stream()
                .map(org.openqa.selenium.WebElement::getText)
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .toList();
    }

    // ------------------------------------------------------------ deleting

    /**
     * Best-effort cleanup - never throws, so it cannot mask a real failure.
     *
     * The delete control is resolved structurally from the holiday's own text
     * node rather than by an ancestor XPath: {@code ancestor::div[.//button][1]}
     * can match the whole page section, whose last button is not this row's.
     * Deleting the wrong holiday during cleanup would be considerably worse
     * than failing to clean up at all.
     */
    public boolean deleteIfPresent(String holidayName) {
        try {
            open();
            // Waiting, not immediate: the list renders after the page shell,
            // and an immediate read skipped cleanup and leaked holidays that
            // then outranked later runs' holidays on the Dashboard widget.
            if (!hasHoliday(holidayName)) {
                return true;
            }

            Object control = ((org.openqa.selenium.JavascriptExecutor) driver()).executeScript(
                    """
                    const name = arguments[0];
                    const leaf = [...document.querySelectorAll('main div,main span,main p,main h6')]
                        .find(e => e.children.length === 0
                                && (e.textContent || '').trim() === name);
                    if (!leaf) return null;
                    let node = leaf;
                    for (let hop = 0; hop < 5 && node; hop++) {
                        node = node.parentElement;
                        if (!node) break;
                        const buttons = [...node.querySelectorAll('button')];
                        if (buttons.length) return buttons[buttons.length - 1];
                    }
                    return null;
                    """, holidayName);

            if (control == null) {
                Log.warn("No delete control found for the holiday '" + holidayName + "'");
                return false;
            }

            ElementUtils.click((org.openqa.selenium.WebElement) control);
            if (WaitUtils.isVisibleWithin(MuiUtils.DIALOG, Duration.ofSeconds(4))) {
                MuiUtils.confirmDestructiveAction();
            }

            // Verify rather than assume: on 2026-09-24 the delete control was
            // observed doing nothing at all (no request, no dialog, no DOM
            // change), and an unverified "cleaned up" hid the leak for days.
            open();
            if (hasHoliday(holidayName)) {
                Log.warn("The holiday '" + holidayName + "' is still listed after delete - "
                        + "the application did not remove it");
                return false;
            }
            Log.info("Cleaned up holiday '" + holidayName + "'");
            return true;
        } catch (RuntimeException e) {
            Log.warn("Could not clean up holiday '" + holidayName + "': " + e.getMessage());
            return false;
        }
    }
}
