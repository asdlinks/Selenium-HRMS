package com.mywehr.pages.employees;

import com.mywehr.enums.AppModule;
import com.mywehr.pages.base.BasePage;
import com.mywehr.utils.ElementUtils;
import com.mywehr.utils.Log;
import com.mywehr.utils.MuiUtils;
import com.mywehr.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.time.Duration;

/**
 * Department Management.
 *
 * Worth knowing when reading the tests below: the app enforces "only empty
 * departments can be deleted", and it reports success by refreshing the list
 * and the KPI strip rather than by raising a toast. There is no snackbar to
 * assert on anywhere in this app, so every mutation is verified against
 * resulting state.
 */
public class DepartmentPage extends BasePage {

    public static final String KPI_DEPARTMENTS = "Departments";
    public static final String KPI_TOTAL_EMPLOYEES = "Total Employees";
    public static final String KPI_LARGEST_TEAM = "Largest Team";

    /** Business rule surfaced in the delete confirmation dialog. */
    public static final String EMPTY_ONLY_DELETE_RULE = "Only empty departments can be deleted";

    private static final By NEW_DEPARTMENT_BUTTON = MuiUtils.buttonByText("New Department");
    private static final By DIALOG_NAME_INPUT =
            By.cssSelector(".MuiDialog-root input[type='text']");

    private static final String CREATE_BUTTON = "Create Team";
    private static final String CANCEL_BUTTON = "Cancel";
    private static final String DELETE_BUTTON = "Delete";

    @Override
    public String landingMarker() {
        return AppModule.DEPARTMENT.landingMarker();
    }

    @Override
    public String route() {
        return AppModule.DEPARTMENT.route();
    }

    // ----------------------------------------------------------------- KPIs

    public int departmentCount() {
        return readSettledKpiCard(KPI_DEPARTMENTS);
    }

    public int totalEmployees() {
        return readSettledKpiCard(KPI_TOTAL_EMPLOYEES);
    }

    public boolean canCreateDepartment() {
        return ElementUtils.isDisplayed(NEW_DEPARTMENT_BUTTON);
    }

    // ------------------------------------------------------------- creating

    public DepartmentPage openCreateDialog() {
        Log.step("Opening the Create New Department dialog");
        MuiUtils.openDialogFrom(NEW_DEPARTMENT_BUTTON);
        return this;
    }

    public String createDialogTitle() {
        return ElementUtils.getText(MuiUtils.DIALOG_TITLE);
    }

    public DepartmentPage enterDepartmentName(String name) {
        ElementUtils.type(DIALOG_NAME_INPUT, name);
        return this;
    }

    public DepartmentPage submitCreateDialog() {
        if (!MuiUtils.clickDialogButtonAndWaitForClose(CREATE_BUTTON)) {
            throw new IllegalStateException(
                    "The Create New Department dialog stayed open after submitting. "
                            + "Validation messages on screen: " + MuiUtils.dialogText());
        }
        WaitUtils.waitForDataToSettle();
        return this;
    }

    /** Full create flow: open, name, save, wait for the row to appear. */
    public DepartmentPage createDepartment(String name) {
        Log.step("Creating department '" + name + "'");
        openCreateDialog();
        enterDepartmentName(name);
        submitCreateDialog();
        return this;
    }

    public DepartmentPage cancelCreateDialog() {
        MuiUtils.clickDialogButtonAndWaitForClose(CANCEL_BUTTON);
        return this;
    }

    // -------------------------------------------------------------- reading

    public boolean hasDepartment(String name) {
        return ElementUtils.waitForPageText(name, Duration.ofSeconds(12));
    }

    public boolean hasDepartmentImmediately(String name) {
        return ElementUtils.pageContainsText(name);
    }

    /** Waits for a department to disappear after deletion. */
    public boolean departmentRemoved(String name) {
        try {
            WaitUtils.fluently(d -> !ElementUtils.pageContainsText(name),
                    Duration.ofSeconds(15),
                    "Department '" + name + "' was still listed");
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }

    /**
     * Headcount chip shown beside a department in the left-hand list.
     *
     * Walks up from the department name looking for a sibling leaf that is
     * purely a number. Matching "the first number in the row text" would be
     * wrong here - generated department names contain digits themselves, so
     * the count has to be identified structurally rather than by scraping.
     *
     * @return the headcount, or -1 when the department is not listed
     */
    public int headcountFor(String departmentName) {
        Object result = ((org.openqa.selenium.JavascriptExecutor) driver()).executeScript(
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
                    const numeric = [...node.querySelectorAll('*')].filter(
                        e => e.children.length === 0
                          && /^\\d+$/.test((e.textContent || '').trim()));
                    if (numeric.length) return numeric[0].textContent.trim();
                }
                return null;
                """, departmentName);
        return result == null ? -1 : Integer.parseInt(result.toString());
    }

    // ------------------------------------------------------------- deleting

    /**
     * Opens the delete confirmation for a department.
     *
     * The trash control is an unlabelled icon button inside the department's
     * row, so it is reached by scoping to the row that carries the name.
     */
    public DepartmentPage clickDeleteFor(String departmentName) {
        Log.step("Requesting deletion of department '" + departmentName + "'");
        MuiUtils.openDialogFrom(resolveDeleteButton(departmentName));
        return this;
    }

    /**
     * Finds the trash control on a department's row.
     *
     * The rows are unlabelled divs, not list items, and the delete control is
     * an icon button with no accessible name - so it is resolved structurally:
     * from the department's own text node, walk up to the nearest ancestor
     * that owns buttons and take the last one. An XPath such as
     * {@code ancestor::div[.//button][1]} is unsafe here because the first
     * matching ancestor can be the whole page section, whose first button is
     * "New Department" - clicking that would open the wrong dialog and the
     * test would fail somewhere confusing.
     */
    private WebElement resolveDeleteButton(String departmentName) {
        Object element = ((JavascriptExecutor) driver()).executeScript(
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
                """, departmentName);

        if (element == null) {
            throw new NoSuchElementException(
                    "No delete control found for the department '" + departmentName
                            + "' - it may not be listed on this page");
        }
        return (WebElement) element;
    }

    public String deleteConfirmationText() {
        return MuiUtils.dialogText();
    }

    public DepartmentPage confirmDelete() {
        MuiUtils.clickDialogButtonAndWaitForClose(DELETE_BUTTON);
        WaitUtils.waitForDataToSettle();
        return this;
    }

    public DepartmentPage cancelDelete() {
        MuiUtils.clickDialogButtonAndWaitForClose(CANCEL_BUTTON);
        return this;
    }

    /** Full delete flow, used both by tests and by cleanup. */
    public DepartmentPage deleteDepartment(String name) {
        clickDeleteFor(name);
        confirmDelete();
        return this;
    }

    /**
     * Best-effort cleanup for teardown.
     *
     * Never throws: a cleanup failure must not mask the real assertion failure
     * that a test already reported.
     */
    public void deleteIfPresent(String name) {
        try {
            open();
            if (hasDepartmentImmediately(name)) {
                deleteDepartment(name);
                Log.info("Cleaned up department '" + name + "'");
            }
        } catch (RuntimeException e) {
            Log.warn("Could not clean up department '" + name + "': " + e.getMessage());
        }
    }
}
