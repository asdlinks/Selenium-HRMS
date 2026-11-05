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

    /** Add New Employee now opens the six-step Employee Onboarding wizard. */
    public EmployeeOnboardingWizard openOnboardingWizard() {
        Log.step("Opening the Employee Onboarding wizard");
        MuiUtils.openDialogFrom(ADD_EMPLOYEE_BUTTON);
        return new EmployeeOnboardingWizard();
    }

    // ------------------------------------------------- per-employee actions

    /**
     * The row action (View Profile / Edit / Reset Password / Delete) on ONE
     * employee's card in the org tree, or null when no card shows that name.
     *
     * The tree nests cards (a manager's card contains their reports), so the
     * card is the smallest ancestor of an action button that holds exactly
     * one button of that kind AND an element whose own text is the name. The
     * name is matched exactly: "Priya Nair" must never resolve to
     * "Priya Nair Jr".
     */
    private org.openqa.selenium.WebElement cardAction(String fullName, String action) {
        return (org.openqa.selenium.WebElement) ((org.openqa.selenium.JavascriptExecutor)
                com.mywehr.driver.DriverManager.get()).executeScript(
                """
                const name = arguments[0], action = arguments[1];
                const selector = '[aria-label="' + action + '"], [title="' + action + '"]';
                for (const button of document.querySelectorAll(selector)) {
                    let card = button.parentElement;
                    while (card && card !== document.body
                            && card.querySelectorAll(selector).length === 1) {
                        const named = [...card.querySelectorAll('*')].some(e =>
                            e.children.length === 0 && e.textContent.trim() === name);
                        if (named) return button;
                        card = card.parentElement;
                    }
                }
                return null;
                """, fullName, action);
    }

    /** Narrows the directory to the name and reports whether its card is shown. */
    public boolean hasEmployeeNamed(String fullName) {
        searchByName(fullName);
        boolean found = WaitUtils.fluentlyQuietly(d -> cardAction(fullName, "Edit") != null,
                Duration.ofSeconds(10));
        clearNameSearch();
        return found;
    }

    public EditEmployeeDialog openEditFor(String fullName) {
        Log.step("Opening Edit for '" + fullName + "'");
        searchByName(fullName);
        org.openqa.selenium.WebElement edit = requireCardAction(fullName, "Edit");
        ElementUtils.clickViaScript(edit);
        MuiUtils.waitForDialog();
        WaitUtils.waitForDataToSettle();
        return new EditEmployeeDialog();
    }

    /** Opens the employee's profile page and returns its visible text. */
    public String openProfileOf(String fullName) {
        Log.step("Opening the profile of '" + fullName + "'");
        searchByName(fullName);
        ElementUtils.clickViaScript(requireCardAction(fullName, "View Profile"));
        WaitUtils.waitForUrlContains("/profile/");
        ElementUtils.waitForPageText("Employee Profile", Duration.ofSeconds(15));
        WaitUtils.waitForDataToSettle();
        return ElementUtils.mainContentText();
    }

    /**
     * Deletes the employee through their card's Delete action and its
     * "Remove this employee?" confirmation.
     *
     * Note what the app means by delete: the person loses login access and
     * leaves the directory, attendance, leave and payroll, but their history
     * and records are KEPT (undoable only via the vendor's support).
     *
     * @return the confirmation dialog's text, for the report
     */
    public String deleteEmployee(String fullName) {
        Log.step("Deleting the employee '" + fullName + "'");
        searchByName(fullName);
        ElementUtils.clickViaScript(requireCardAction(fullName, "Delete"));
        MuiUtils.waitForDialog();
        String confirmation = MuiUtils.dialogText();

        org.openqa.selenium.WebElement dialog =
                ElementUtils.find(MuiUtils.DIALOG, com.mywehr.enums.WaitStrategy.VISIBLE);
        org.openqa.selenium.WebElement confirm = dialog.findElements(By.xpath(".//button")).stream()
                .filter(b -> {
                    // The app captions it "Remove Employee"; the others are kept
                    // for wording changes, never "Cancel".
                    String text = b.getText().trim().toLowerCase();
                    return text.startsWith("remove") || text.contains("delete")
                            || text.equals("confirm") || text.startsWith("yes");
                })
                .reduce((first, second) -> second)
                .orElseThrow(() -> new com.mywehr.exceptions.FrameworkException(
                        "The delete confirmation has no confirm button. It read: " + confirmation));
        ElementUtils.clickViaScript(confirm);
        if (!WaitUtils.waitForInvisibleQuietly(MuiUtils.DIALOG, Duration.ofSeconds(15))) {
            throw new com.mywehr.exceptions.FrameworkException(
                    "The delete confirmation did not close. It read: " + MuiUtils.dialogText());
        }
        WaitUtils.waitForDataToSettle();
        clearNameSearch();
        return confirmation;
    }

    /**
     * Removes the employee if a card for them exists. Cleanup only: safe to
     * call whether or not the record was ever created.
     *
     * @return true when a record was found and deleted
     */
    public boolean deleteEmployeeIfPresent(String fullName) {
        MuiUtils.dismissOverlays();
        if (!hasEmployeeNamed(fullName)) {
            return false;
        }
        deleteEmployee(fullName);
        boolean gone = !hasEmployeeNamed(fullName);
        if (gone) {
            Log.warn("Cleaned up the employee '" + fullName + "'");
        } else {
            Log.warn("Tried to delete '" + fullName + "' but the card is still listed");
        }
        return gone;
    }

    private org.openqa.selenium.WebElement requireCardAction(String fullName, String action) {
        try {
            return WaitUtils.fluently(d -> cardAction(fullName, action), Duration.ofSeconds(12),
                    "No '" + action + "' action on a card for '" + fullName + "'");
        } catch (org.openqa.selenium.TimeoutException e) {
            throw new com.mywehr.exceptions.FrameworkException(
                    "No card with a '" + action + "' action for '" + fullName + "'", e);
        }
    }

    // ------------------------------------------------- subscription capacity

    /** Banner the app shows when the tenant's plan seat limit is reached. */
    // Matched on the element's own text node so it resolves to the message
    // itself rather than every ancestor, whatever component renders it.
    private static final By PLAN_LIMIT_BANNER =
            By.xpath("//*[contains(text(),'employee limit')]");

    /** The "(used/limit)" figures inside the plan banner. */
    private static final java.util.regex.Pattern SEAT_USAGE =
            java.util.regex.Pattern.compile("\\((\\d+)\\s*/\\s*(\\d+)\\)");

    /**
     * True when the tenant cannot take another employee.
     *
     * The plan caps headcount, and on a shared test tenant that ceiling gets
     * reached eventually - every creation test consumes a seat and the app
     * offers no bulk cleanup. Without this check the symptom is
     * indistinguishable from a broken form: the dialog closes, nothing is
     * created, and the test reports "the employee is not listed". Detecting
     * the cap lets the suite say what is actually wrong.
     *
     * The banner appears BEFORE the cap too - "Approaching your subscription
     * plan's employee limit (9/10)" - so its presence alone is not the answer;
     * the used/limit figures are. Only a banner without figures falls back to
     * being treated as the cap.
     */
    public boolean isAtEmployeeLimit() {
        if (!ElementUtils.isPresent(PLAN_LIMIT_BANNER)) {
            return false;
        }
        java.util.regex.Matcher usage = SEAT_USAGE.matcher(employeeLimitMessage());
        if (usage.find()) {
            return Integer.parseInt(usage.group(1)) >= Integer.parseInt(usage.group(2));
        }
        return true;
    }

    /** The plan-limit message, or empty when the tenant still has capacity. */
    public String employeeLimitMessage() {
        return ElementUtils.isPresent(PLAN_LIMIT_BANNER)
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
