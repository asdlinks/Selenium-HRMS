package com.mywehr.pages.dashboard;

import com.mywehr.enums.AppModule;
import com.mywehr.pages.base.BasePage;
import com.mywehr.pages.components.HeaderComponent;
import com.mywehr.utils.ElementUtils;
import com.mywehr.utils.MuiUtils;
import org.openqa.selenium.By;

/**
 * The landing page after sign-in, and the hub for inter-module verification.
 *
 * Its widgets are fed by every other module - workforce headcount from
 * Employees, pending approvals from Leave, the next holiday from the Holiday
 * Calendar - which makes it the natural place to prove that a change made in
 * one module actually propagated.
 */
public class DashboardPage extends BasePage {

    // --- KPI card labels ---
    public static final String KPI_TOTAL_WORKFORCE = "Total Workforce";
    public static final String KPI_CURRENTLY_AWAY = "Currently Away";
    public static final String KPI_PENDING_REQUESTS = "Pending Requests";
    public static final String KPI_NEXT_HOLIDAY = "Next Holiday";

    // --- widgets ---
    // Matched on the element's OWN text node, not its subtree. Using "." here
    // would also match every ancestor of the greeting, and getText() on one of
    // those returns the whole dashboard.
    private static final By GREETING =
            By.xpath("//*[starts-with(normalize-space(text()),'Welcome,')]");
    private static final By PENDING_APPROVALS_WIDGET =
            By.xpath("//*[normalize-space(text())='Pending Approvals']");
    private static final By CELEBRATIONS_WIDGET =
            By.xpath("//*[normalize-space(text())='Celebrations']");
    private static final By UPCOMING_HOLIDAYS_WIDGET =
            By.xpath("//*[normalize-space(text())='Upcoming Holidays']");
    private static final By ANNOUNCEMENTS_WIDGET =
            By.xpath("//*[normalize-space(text())='Company Announcements']");
    private static final By DEPARTMENT_HEADCOUNT_WIDGET =
            By.xpath("//*[normalize-space(text())='Department Headcount']");
    private static final By GLOBAL_TIME_OFF_WIDGET =
            By.xpath("//*[normalize-space(text())='Global Time Off']");
    private static final By RECENT_ACTIVITY_WIDGET =
            By.xpath("//*[normalize-space(text())='Recent Activity']");
    private static final By QUICK_ACTIONS_WIDGET =
            By.xpath("//*[normalize-space(text())='Quick Actions']");

    private final HeaderComponent header = new HeaderComponent();

    @Override
    public String landingMarker() {
        return AppModule.DASHBOARD.landingMarker();
    }

    @Override
    public String route() {
        return AppModule.DASHBOARD.route();
    }

    public HeaderComponent header() {
        return header;
    }

    // ------------------------------------------------------------- greeting

    /** Full greeting line, e.g. "Welcome, Surag". */
    public String greeting() {
        if (!ElementUtils.isPresent(GREETING)) {
            return "";
        }
        // First line only - the greeting node sits directly above the KPI strip
        // and a rendering change could pull sibling text into the same block.
        return ElementUtils.getText(GREETING).lines().findFirst().orElse("").trim();
    }

    /** Just the name, so tests can assert which persona's session is live. */
    public String signedInUserName() {
        String greeting = greeting();
        int comma = greeting.indexOf(',');
        return comma < 0 ? "" : greeting.substring(comma + 1).trim();
    }

    // ---------------------------------------------------------------- KPIs

    public int totalWorkforce() {
        return readSettledKpiCard(KPI_TOTAL_WORKFORCE);
    }

    public int currentlyAway() {
        return readKpiCard(KPI_CURRENTLY_AWAY);
    }

    public int pendingRequests() {
        return readKpiCard(KPI_PENDING_REQUESTS);
    }

    /** Name of the next holiday as shown on the KPI strip. */
    public String nextHolidayName() {
        By value = By.xpath("//*[normalize-space(text())='Next Holiday']"
                + "/following::*[string-length(normalize-space(text()))>0][1]");
        return ElementUtils.isPresent(value)
                ? ElementUtils.find(value, com.mywehr.enums.WaitStrategy.PRESENT).getText().trim()
                : "";
    }

    // -------------------------------------------------------------- widgets

    public boolean hasPendingApprovalsWidget() {
        return ElementUtils.isDisplayed(PENDING_APPROVALS_WIDGET);
    }

    public boolean hasCelebrationsWidget() {
        return ElementUtils.isDisplayed(CELEBRATIONS_WIDGET);
    }

    public boolean hasUpcomingHolidaysWidget() {
        return ElementUtils.isDisplayed(UPCOMING_HOLIDAYS_WIDGET);
    }

    public boolean hasAnnouncementsWidget() {
        return ElementUtils.isDisplayed(ANNOUNCEMENTS_WIDGET);
    }

    public boolean hasDepartmentHeadcountWidget() {
        return ElementUtils.isDisplayed(DEPARTMENT_HEADCOUNT_WIDGET);
    }

    public boolean hasGlobalTimeOffWidget() {
        return ElementUtils.isDisplayed(GLOBAL_TIME_OFF_WIDGET);
    }

    public boolean hasRecentActivityWidget() {
        return ElementUtils.isDisplayed(RECENT_ACTIVITY_WIDGET);
    }

    public boolean hasQuickActionsWidget() {
        return ElementUtils.isDisplayed(QUICK_ACTIONS_WIDGET);
    }

    /** Every analytics widget the dashboard is expected to render. */
    public boolean allCoreWidgetsRendered() {
        return hasPendingApprovalsWidget()
                && hasUpcomingHolidaysWidget()
                && hasDepartmentHeadcountWidget()
                && hasGlobalTimeOffWidget()
                && hasQuickActionsWidget();
    }

    /**
     * True when the Department Headcount chart plots the named department.
     * Used to prove a department created in another module reached the
     * dashboard's analytics.
     */
    public boolean departmentHeadcountIncludes(String departmentName) {
        return displaysText(departmentName);
    }

    /** True when the named employee has a card in Pending Approvals. */
    public boolean pendingApprovalsInclude(String employeeName) {
        By card = By.xpath("//*[normalize-space(text())='Pending Approvals']"
                + "/ancestor::div[3]//*[contains(normalize-space(.),"
                + MuiUtils.escapeForXPath(employeeName) + ")]");
        return ElementUtils.isPresent(card);
    }

    // -------------------------------------------------------- quick actions

    public void clickQuickAction(String label) {
        ElementUtils.click(By.xpath("//*[normalize-space(text())="
                + MuiUtils.escapeForXPath(label) + "]"));
        waitForSettled();
    }
}
