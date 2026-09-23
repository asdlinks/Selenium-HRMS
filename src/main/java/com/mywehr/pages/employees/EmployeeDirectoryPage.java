package com.mywehr.pages.employees;

import com.mywehr.enums.AppModule;
import com.mywehr.pages.base.BasePage;
import com.mywehr.pages.components.SideNavComponent;
import com.mywehr.utils.ElementUtils;
import com.mywehr.utils.Log;
import com.mywehr.utils.MuiUtils;
import com.mywehr.utils.WaitUtils;
import org.openqa.selenium.By;

import java.time.Duration;

/** Employee Directory - the org-wide people list with KPI strip and filters. */
public class EmployeeDirectoryPage extends BasePage {

    public static final String KPI_TOTAL_EMPLOYEES = "Total Employees";
    public static final String KPI_DEPARTMENTS = "Departments";
    public static final String KPI_MANAGERS = "Managers";
    public static final String KPI_NEW_JOINERS = "New Joiners (This Month)";

    private static final By ADD_EMPLOYEE_BUTTON = MuiUtils.buttonByText("Add New Employee");
    private static final By EXPORT_BUTTON = MuiUtils.buttonByText("Export");
    private static final By NAME_SEARCH = By.cssSelector("input[placeholder*='Search by name']");

    private static final By DEPARTMENT_FILTER = MuiUtils.selectByLabel("Department");
    private static final By BRANCH_FILTER = MuiUtils.selectByLabel("Branch");
    private static final By DESIGNATION_FILTER = MuiUtils.selectByLabel("Designation");
    private static final By EMPLOYMENT_TYPE_FILTER = MuiUtils.selectByLabel("Employment Type");
    private static final By ROLE_FILTER = MuiUtils.selectByLabel("Role");

    private final SideNavComponent sideNav = new SideNavComponent();

    @Override
    public String landingMarker() {
        return AppModule.EMPLOYEE_DIRECTORY.landingMarker();
    }

    @Override
    public String route() {
        return AppModule.EMPLOYEE_DIRECTORY.route();
    }

    public SideNavComponent sideNav() {
        return sideNav;
    }

    // ------------------------------------------------------------- KPI strip

    public int totalEmployees() {
        return readSettledKpiCard(KPI_TOTAL_EMPLOYEES);
    }

    public int departmentCount() {
        return readSettledKpiCard(KPI_DEPARTMENTS);
    }

    public int managerCount() {
        return readKpiCard(KPI_MANAGERS);
    }

    public int newJoinersThisMonth() {
        return readKpiCard(KPI_NEW_JOINERS);
    }

    // ------------------------------------------------------------- controls

    public boolean canAddEmployee() {
        return ElementUtils.isDisplayed(ADD_EMPLOYEE_BUTTON);
    }

    public boolean canExport() {
        return ElementUtils.isDisplayed(EXPORT_BUTTON);
    }

    public boolean allFiltersDisplayed() {
        return ElementUtils.isDisplayed(DEPARTMENT_FILTER)
                && ElementUtils.isDisplayed(BRANCH_FILTER)
                && ElementUtils.isDisplayed(DESIGNATION_FILTER)
                && ElementUtils.isDisplayed(EMPLOYMENT_TYPE_FILTER)
                && ElementUtils.isDisplayed(ROLE_FILTER);
    }

    public RegisterEmployeeDialog openRegisterEmployeeDialog() {
        Log.step("Opening the Register New Employee dialog");
        MuiUtils.openDialogFrom(ADD_EMPLOYEE_BUTTON);
        return new RegisterEmployeeDialog();
    }

    // ------------------------------------------------- subscription capacity

    /** Banner the app shows when the tenant's plan seat limit is reached. */
    // Matched on the element's own text node so it resolves to the message
    // itself rather than every ancestor, whatever component renders it.
    private static final By PLAN_LIMIT_BANNER =
            By.xpath("//*[contains(text(),'employee limit')]");

    /**
     * True when the tenant cannot take another employee.
     *
     * The plan caps headcount, and on a shared test tenant that ceiling gets
     * reached eventually - every creation test consumes a seat and the app
     * offers no bulk cleanup. Without this check the symptom is
     * indistinguishable from a broken form: the dialog closes, nothing is
     * created, and the test reports "the employee is not listed". Detecting
     * the cap lets the suite say what is actually wrong.
     */
    public boolean isAtEmployeeLimit() {
        return ElementUtils.isPresent(PLAN_LIMIT_BANNER);
    }

    /** The plan-limit message, or empty when the tenant still has capacity. */
    public String employeeLimitMessage() {
        return isAtEmployeeLimit()
                ? ElementUtils.find(PLAN_LIMIT_BANNER,
                        com.mywehr.enums.WaitStrategy.PRESENT).getText().trim()
                : "";
    }

    // -------------------------------------------------------------- search

    public EmployeeDirectoryPage searchByName(String name) {
        Log.step("Filtering the directory by name '" + name + "'");
        ElementUtils.type(NAME_SEARCH, name);
        // The list filters client-side on a debounce rather than a request.
        WaitUtils.sleepQuietly(Duration.ofMillis(900));
        return this;
    }

    public EmployeeDirectoryPage clearNameSearch() {
        ElementUtils.type(NAME_SEARCH, "");
        WaitUtils.sleepQuietly(Duration.ofMillis(900));
        return this;
    }

    public EmployeeDirectoryPage filterByDepartment(String department) {
        Log.step("Filtering the directory by department '" + department + "'");
        MuiUtils.selectOption(DEPARTMENT_FILTER, department);
        waitForSettled();
        return this;
    }

    /** Department options currently offered by the filter dropdown. */
    public java.util.List<String> availableDepartmentFilterOptions() {
        return MuiUtils.readOptions(DEPARTMENT_FILTER);
    }

    // ------------------------------------------------------------- results

    /**
     * True when a card or row for this employee is on screen RIGHT NOW.
     *
     * Note this only sees the department currently selected in the org-chart
     * view - use {@link #containsEmployee(String)} to ask whether the employee
     * exists in the directory at all.
     */
    public boolean showsEmployee(String nameOrEmail) {
        return ElementUtils.waitForPageText(nameOrEmail, Duration.ofSeconds(12));
    }

    /**
     * True when the employee appears under the given department.
     *
     * WHY THE DEPARTMENT IS REQUIRED
     * ------------------------------
     * The directory's default view is an org chart that renders ONE department
     * group at a time - HR is selected on load - and the name search narrows
     * within whatever group is showing. An employee created into Sales is
     * therefore genuinely off-screen even though the record saved perfectly,
     * which reads as "the employee was never created" and sends you hunting
     * for a bug that does not exist.
     *
     * So the group is selected first, then the name is searched inside it.
     * That is also exactly what a user would do.
     */
    public boolean containsEmployeeIn(String department, String nameOrEmail) {
        selectDepartmentGroup(department);
        searchByName(nameOrEmail);
        boolean found = ElementUtils.waitForPageText(nameOrEmail, Duration.ofSeconds(12));
        clearNameSearch();
        return found;
    }

    /**
     * Switches the org chart to a department group via its chip.
     *
     * Does nothing when the chip is absent - a tenant with a single department
     * renders no chips at all, and that is not an error.
     */
    public EmployeeDirectoryPage selectDepartmentGroup(String department) {
        By chip = By.xpath("//button[normalize-space(.)="
                + MuiUtils.escapeForXPath(department) + "]"
                + " | //*[@role='button'][normalize-space(.)="
                + MuiUtils.escapeForXPath(department) + "]");
        if (!ElementUtils.isPresent(chip)) {
            Log.warn("No department group chip for '" + department + "' - "
                    + "the directory may not be grouped");
            return this;
        }
        Log.step("Switching the directory to the '" + department + "' group");
        ElementUtils.click(chip);
        waitForSettled();
        return this;
    }

    public boolean showsEmployeeImmediately(String nameOrEmail) {
        return ElementUtils.pageContainsText(nameOrEmail);
    }

    /** Department group headings rendered over the employee cards. */
    public boolean showsDepartmentGroup(String department) {
        return displaysText(department);
    }

    /**
     * Row-level actions the current persona is offered.
     *
     * Presence, not visibility: the card actions are rendered for every row
     * but only revealed on hover, so a visibility check would report "HR
     * cannot edit employees" when the real answer is "the mouse is not over
     * the card". Whether the control exists in the row at all is what the
     * permission model decides, and what this is asking.
     */
    public boolean offersAction(String actionLabel) {
        return ElementUtils.isPresent(MuiUtils.controlByAccessibleName(actionLabel));
    }

    /** True when the action is not merely present but actually on screen. */
    public boolean actionIsVisible(String actionLabel) {
        return ElementUtils.isDisplayed(MuiUtils.controlByAccessibleName(actionLabel));
    }
}
