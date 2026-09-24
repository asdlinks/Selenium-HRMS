package com.mywehr.pages.timeleave;

import com.mywehr.config.ConfigManager;
import com.mywehr.enums.AppModule;
import com.mywehr.enums.LeaveStatus;
import com.mywehr.enums.LeaveType;
import com.mywehr.pages.base.BasePage;
import com.mywehr.pages.components.SideNavComponent;
import com.mywehr.utils.ElementUtils;
import com.mywehr.utils.Log;
import com.mywehr.utils.MuiUtils;
import com.mywehr.utils.WaitUtils;
import org.openqa.selenium.By;

import java.time.Duration;

/**
 * Leave Management.
 *
 * Three things a reader should know:
 *
 *  - The balance cards ("Casual 10/10") always show the SIGNED-IN user's own
 *    entitlement, never the target employee's, even when an administrator is
 *    recording leave on someone else's behalf.
 *  - The status tabs carry live counts - "All (6)", "Pending (1)" - which is
 *    what makes tab-versus-grid consistency assertable.
 *  - Approve / Reject buttons only render on rows that are still Pending.
 */
public class LeavesPage extends BasePage {

    private static final By APPLY_LEAVE_BUTTON = MuiUtils.buttonByText("Apply for Leave");
    private static final By RECENT_REQUESTS_HEADING =
            By.xpath("//*[normalize-space(text())='Recent Requests']");

    private final SideNavComponent sideNav = new SideNavComponent();

    @Override
    public String landingMarker() {
        return AppModule.LEAVES.landingMarker();
    }

    @Override
    public String route() {
        return AppModule.LEAVES.route();
    }

    public SideNavComponent sideNav() {
        return sideNav;
    }

    // ------------------------------------------------------- balance cards

    /**
     * Remaining days for a leave type, read from the "10/10" balance card.
     * Returns -1 when the card is absent.
     */
    public int remainingBalance(LeaveType leaveType) {
        return readBalance(leaveType, 1);
    }

    /** Total entitlement - the denominator of the balance card. */
    public int totalEntitlement(LeaveType leaveType) {
        return readBalance(leaveType, 2);
    }

    /**
     * Reads one side of the "remaining/total" figure on a balance card.
     *
     * The card label is matched CASE-INSENSITIVELY on purpose: the DOM holds
     * "casual" in lowercase and MUI renders it uppercase through CSS
     * text-transform. Matching the rendered casing finds nothing.
     *
     * @param group 1 for the remaining days, 2 for the total entitlement
     */
    private int readBalance(LeaveType leaveType, int group) {
        // The cards mount only once the balance fetch resolves, so a read
        // taken straight after navigation finds nothing. Poll until they do.
        try {
            return WaitUtils.fluently(d -> {
                int value = readBalanceOnce(leaveType, group);
                return value < 0 ? null : value;
            }, Duration.ofSeconds(15), "Balance card for " + leaveType + " never rendered");
        } catch (org.openqa.selenium.TimeoutException e) {
            return -1;
        }
    }

    private int readBalanceOnce(LeaveType leaveType, int group) {
        Object result = ((org.openqa.selenium.JavascriptExecutor) driver()).executeScript(
                """
                const label = arguments[0].toLowerCase();
                const group = arguments[1];
                const leaf = [...document.querySelectorAll('main div,main span,main p')]
                    .find(e => e.children.length === 0
                            && (e.textContent || '').trim().toLowerCase() === label);
                if (!leaf) return null;
                let node = leaf;
                for (let hop = 0; hop < 4 && node; hop++) {
                    node = node.parentElement;
                    if (!node) break;
                    const match = (node.innerText || '').match(/(\\d+)\\s*\\/\\s*(\\d+)/);
                    if (match) return match[group];
                }
                return null;
                """, leaveType.gridLabel(), group);
        return result == null ? -1 : Integer.parseInt(result.toString());
    }

    public boolean allBalanceCardsDisplayed() {
        return remainingBalance(LeaveType.CASUAL) >= 0
                && remainingBalance(LeaveType.SICK) >= 0
                && remainingBalance(LeaveType.PAID) >= 0;
    }

    // ---------------------------------------------------------- status tabs

    /**
     * The status "tabs" are MUI Chips, each a span.MuiChip-label reading
     * "Pending (1)" inside a clickable wrapper.
     *
     * The locator is anchored to that label span and matched on its own text.
     * An earlier, looser locator matched any ancestor CONTAINING the text,
     * which resolved to the strip wrapping all five chips - every status then
     * reported the same number, and the tabs appeared not to reconcile.
     */
    private static By statusChipLabel(LeaveStatus status) {
        return By.xpath("//span[contains(@class,'MuiChip-label')]"
                + "[starts-with(normalize-space(.),"
                + MuiUtils.escapeForXPath(status.label() + " (") + ")]");
    }

    /** Count shown on a status chip, e.g. 6 from "All (6)". */
    public int tabCount(LeaveStatus status) {
        By chip = statusChipLabel(status);
        if (!ElementUtils.isPresent(chip)) {
            Log.warn("No status chip found for '" + status.label() + "'");
            return -1;
        }
        String text = ElementUtils.find(chip, com.mywehr.enums.WaitStrategy.PRESENT).getText();
        int open = text.indexOf('(');
        int close = text.indexOf(')', open);
        if (open < 0 || close < 0) {
            return -1;
        }
        return Integer.parseInt(text.substring(open + 1, close).replaceAll("[^0-9]", ""));
    }

    /**
     * Polls a status tab until it reports the expected count.
     *
     * The tab counts are refetched after an approval or a new request, so a
     * single read straight after the mutation races that refresh.
     */
    public boolean waitForTabCount(LeaveStatus status, int expectedCount) {
        try {
            WaitUtils.fluently(d -> tabCount(status) == expectedCount,
                    com.mywehr.config.ConfigManager.explicitWait(),
                    "The '" + status.label() + "' tab never reached " + expectedCount);
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            Log.warn("The '" + status.label() + "' tab stayed at " + tabCount(status)
                    + ", expected " + expectedCount);
            return false;
        }
    }

    /**
     * Switches the grid to a status.
     *
     * The clickable surface is the chip wrapper, not the inert label span, and
     * the filter is confirmed by waiting for the grid to actually hold the
     * number of rows the chip advertises - clicking a chip that silently does
     * nothing would otherwise leave the next assertion reading the unfiltered
     * grid and failing for the wrong reason.
     */
    public LeavesPage selectStatusTab(LeaveStatus status) {
        Log.step("Switching to the '" + status.label() + "' leave tab");
        int expectedRows = tabCount(status);

        By chip = By.xpath("//span[contains(@class,'MuiChip-label')]"
                + "[starts-with(normalize-space(.),"
                + MuiUtils.escapeForXPath(status.label() + " (") + ")]"
                + "/ancestor-or-self::*[contains(@class,'MuiChip-root')"
                + " or @role='button' or self::button][1]");

        ElementUtils.click(chip);
        waitForGridOrEmptyState();

        if (expectedRows >= 0 && !waitForGridTotal(expectedRows)) {
            Log.warn("The grid did not filter on the native click - "
                    + "replaying the full pointer sequence");
            ElementUtils.clickViaScript(chip);
            waitForGridOrEmptyState();
            waitForGridTotal(expectedRows);
        }
        return this;
    }

    /** Polls the grid's pagination footer until it reports the expected total. */
    private boolean waitForGridTotal(int expectedTotal) {
        try {
            WaitUtils.fluently(d -> totalRequestCount() == expectedTotal,
                    java.time.Duration.ofSeconds(12),
                    "The grid never showed " + expectedTotal + " rows");
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }

    // ---------------------------------------------------------------- grid

    public boolean isGridDisplayed() {
        return ElementUtils.isDisplayed(MuiUtils.DATA_GRID);
    }

    public boolean hasRecentRequestsSection() {
        return ElementUtils.isDisplayed(RECENT_REQUESTS_HEADING);
    }

    public int visibleRowCount() {
        return waitForGridOrEmptyState() ? 0 : MuiUtils.gridRowCount();
    }

    /** Total across all pages, read from the pagination footer. */
    public int totalRequestCount() {
        return waitForGridOrEmptyState() ? 0 : MuiUtils.gridTotalFromPagination();
    }

    /** The message shown in place of the grid when a tab has no requests. */
    private static final By EMPTY_STATE = By.xpath(
            "//main//*[starts-with(normalize-space(text()),'No ')]"
                    + "[contains(normalize-space(text()),'found')]");

    /**
     * Waits for the request list to render in either of its two forms.
     *
     * A tab with no requests renders a "No ... found" message instead of a
     * data grid, so waiting for the grid alone times out on every empty tab -
     * the Pending tab is usually empty on this tenant.
     *
     * @return true when the empty state is shown, false when the grid is
     */
    private boolean waitForGridOrEmptyState() {
        // Returns a label rather than a Boolean: a wait treats Boolean.FALSE as
        // "not yet", so "the grid is here, it is not empty" would never resolve.
        String rendered = WaitUtils.fluently(d -> {
            if (d.findElements(MuiUtils.DATA_GRID).stream().anyMatch(e -> e.isDisplayed())) {
                return "grid";
            }
            return d.findElements(EMPTY_STATE).isEmpty() ? null : "empty";
        }, ConfigManager.explicitWait(), "Neither the request grid nor its empty state rendered");
        WaitUtils.waitForDataToSettle();
        return "empty".equals(rendered);
    }

    public boolean hasRequestFor(String employeeName) {
        return MuiUtils.gridHasRowContaining(employeeName);
    }

    /**
     * Status of the most recent request for an employee.
     *
     * Reads the whole row rather than a single cell: the grid virtualises
     * columns, so a status cell can be outside the rendered window while the
     * row itself is present.
     */
    public String statusOf(String employeeName) {
        By row = MuiUtils.gridRowContaining(employeeName);
        if (!ElementUtils.isPresent(row)) {
            return "";
        }
        String rowText = ElementUtils.find(row, com.mywehr.enums.WaitStrategy.PRESENT).getText();
        for (LeaveStatus status : LeaveStatus.values()) {
            if (status != LeaveStatus.ALL && rowText.contains(status.label())) {
                return status.label();
            }
        }
        return "";
    }

    /** True when a row for this employee shows the given status. */
    public boolean hasRequestWithStatus(String employeeName, LeaveStatus status) {
        By row = By.xpath("//div[contains(@class,'MuiDataGrid-row')]"
                + "[contains(normalize-space(.)," + MuiUtils.escapeForXPath(employeeName) + ")]"
                + "[contains(normalize-space(.)," + MuiUtils.escapeForXPath(status.label()) + ")]");
        return WaitUtils.isVisibleWithin(row, Duration.ofSeconds(12));
    }

    /** True when a row matches both the employee and a reason/date fragment. */
    public boolean hasRequestMatching(String employeeName, String fragment) {
        By row = By.xpath("//div[contains(@class,'MuiDataGrid-row')]"
                + "[contains(normalize-space(.)," + MuiUtils.escapeForXPath(employeeName) + ")]"
                + "[contains(normalize-space(.)," + MuiUtils.escapeForXPath(fragment) + ")]");
        return WaitUtils.isVisibleWithin(row, Duration.ofSeconds(12));
    }

    /**
     * True when one row carries the employee, every fragment and the status.
     *
     * Used to pin down one specific request - an employee can have many rows,
     * and "sales1 is Approved" is true of some older request on any given day.
     */
    public boolean hasRequestWithStatus(String employeeName, LeaveStatus status,
                                        String... fragments) {
        StringBuilder xpath = new StringBuilder("//div[contains(@class,'MuiDataGrid-row')]")
                .append("[contains(normalize-space(.),")
                .append(MuiUtils.escapeForXPath(employeeName)).append(")]")
                .append("[contains(normalize-space(.),")
                .append(MuiUtils.escapeForXPath(status.label())).append(")]");
        for (String fragment : fragments) {
            xpath.append("[contains(normalize-space(.),")
                    .append(MuiUtils.escapeForXPath(fragment)).append(")]");
        }
        return WaitUtils.isVisibleWithin(By.xpath(xpath.toString()), Duration.ofSeconds(12));
    }

    // ----------------------------------------------------------- approvals

    public boolean canApprove() {
        return ElementUtils.isDisplayed(MuiUtils.buttonByText("Approve"));
    }

    public boolean canReject() {
        return ElementUtils.isDisplayed(MuiUtils.buttonByText("Reject"));
    }

    /**
     * Approves the pending request on the row belonging to an employee.
     * Scoped to the row so a multi-row grid cannot approve the wrong request.
     */
    public LeavesPage approveRequestFor(String employeeName) {
        Log.step("Approving the leave request for " + employeeName);
        clickRowAction(employeeName, "Approve");
        return this;
    }

    public LeavesPage rejectRequestFor(String employeeName) {
        Log.step("Rejecting the leave request for " + employeeName);
        clickRowAction(employeeName, "Reject");
        return this;
    }

    private void clickRowAction(String employeeName, String action) {
        By actionButton = By.xpath("//div[contains(@class,'MuiDataGrid-row')]"
                + "[contains(normalize-space(.)," + MuiUtils.escapeForXPath(employeeName) + ")]"
                + "//button[normalize-space(.)=" + MuiUtils.escapeForXPath(action) + "]");
        ElementUtils.click(actionButton);

        // The action may raise a confirmation dialog depending on configuration.
        if (WaitUtils.isVisibleWithin(MuiUtils.DIALOG, Duration.ofSeconds(3))) {
            MuiUtils.clickDialogButton(action);
            MuiUtils.waitForDialogToClose();
        }
        WaitUtils.waitForDataToSettle();
    }

    // -------------------------------------------------------- apply drawer

    public boolean canApplyForLeave() {
        return ElementUtils.isDisplayed(APPLY_LEAVE_BUTTON);
    }

    public ApplyLeaveDrawer openApplyLeaveDrawer() {
        Log.step("Opening the Apply for Leave drawer");
        By recordLeave = MuiUtils.buttonByText("Record Leave");

        ElementUtils.click(APPLY_LEAVE_BUTTON);
        if (!WaitUtils.isVisibleWithin(recordLeave, java.time.Duration.ofSeconds(6))) {
            Log.warn("The leave drawer did not open on the native click - "
                    + "replaying the full pointer sequence");
            ElementUtils.clickViaScript(APPLY_LEAVE_BUTTON);
            WaitUtils.waitForVisible(recordLeave);
        }

        // The drawer slides in. Its fields are already in the DOM and already
        // "visible" to Selenium while the transform is still running, but they
        // refuse keystrokes until it settles, which surfaces as
        // ElementNotInteractableException on the first field touched.
        WaitUtils.sleepQuietly(java.time.Duration.ofMillis(700));
        return new ApplyLeaveDrawer();
    }
}
