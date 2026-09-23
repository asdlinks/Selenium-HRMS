package com.mywehr.tests.employee;

import com.mywehr.base.BaseTest;
import com.mywehr.data.TestDataFactory;
import com.mywehr.data.model.EmployeeData;
import com.mywehr.data.reader.JsonDataReader;
import com.mywehr.enums.AppModule;
import com.mywehr.enums.Persona;
import com.mywehr.pages.employees.DepartmentPage;
import com.mywehr.pages.employees.EmployeeDirectoryPage;
import com.mywehr.pages.employees.RegisterEmployeeDialog;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * TC_EMP - Employee directory and department management.
 *
 * Tests that create data use {@link TestDataFactory}, so every record is
 * unique and prefixed "AT" - the suite can be run repeatedly against the same
 * tenant, and anything it leaves behind is identifiable.
 */
public class EmployeeManagementTest extends BaseTest {

    @Test(priority = 1,
            groups = {"smoke", "employee", "critical"},
            description = "TC_EMP_01 - The Employee Directory renders its KPI strip, filters "
                    + "and controls, and search and department filtering narrow the results")
    public void TC_EMP_01_directoryRendersCompletelyAndSearchAndFilterNarrowResults() {
        step("Signing in as the Organization Administrator and opening the directory");
        loginAndOpen(Persona.ADMIN, AppModule.EMPLOYEE_DIRECTORY);

        EmployeeDirectoryPage directory = new EmployeeDirectoryPage();
        assertTrue(directory.isLoaded(), "The Employee Directory did not load");

        step("Verifying the KPI strip reports real figures");
        int totalEmployees = directory.totalEmployees();
        int departments = directory.departmentCount();
        note("Directory KPIs - employees: " + totalEmployees
                + ", departments: " + departments
                + ", managers: " + directory.managerCount()
                + ", new joiners: " + directory.newJoinersThisMonth());
        softly().assertTrue(totalEmployees > 0,
                "The directory reports " + totalEmployees + " employees - the KPI did not resolve");
        softly().assertTrue(departments > 0,
                "The directory reports " + departments + " departments");

        step("Verifying the filters and administration controls");
        softly().assertTrue(directory.allFiltersDisplayed(),
                "One or more of the Department / Branch / Designation / Employment Type / "
                        + "Role filters is missing");
        softly().assertTrue(directory.canAddEmployee(), "The Add New Employee control is missing");
        softly().assertTrue(directory.canExport(), "The Export control is missing");

        // Hard: the search checks below are meaningless without a known employee.
        step("Confirming a known seeded employee is present before filtering");
        assertTrue(directory.showsEmployee("hr"),
                "The seeded employee 'hr' is not in the unfiltered directory");

        step("Searching for a name that cannot match anything");
        directory.searchByName("zzz_no_such_employee_zzz");
        softly().assertFalse(directory.showsEmployeeImmediately("hr"),
                "A search with no possible match still shows 'hr' - the name filter is not applied");

        step("Clearing the search and confirming the directory is restored");
        directory.clearNameSearch();
        softly().assertTrue(directory.showsEmployee("hr"),
                "Clearing the search did not restore the full directory");

        step("Filtering by the HR department");
        directory.filterByDepartment("HR");
        softly().assertTrue(directory.showsEmployee("hr"),
                "Filtering by the HR department hid the employee who belongs to it");

        assertAllSoft();
        verified("The directory renders completely and search and filtering narrow correctly");
    }

    @Test(priority = 2,
            groups = {"smoke", "employee", "e2e", "critical"},
            description = "TC_EMP_02 - The Administrator registers a new employee end to end "
                    + "and the record appears in the directory with the headcount raised")
    public void TC_EMP_02_administratorRegistersEmployeeAndDirectoryReflectsIt() {
        step("Signing in as the Organization Administrator and opening the directory");
        loginAndOpen(Persona.ADMIN, AppModule.EMPLOYEE_DIRECTORY);

        EmployeeDirectoryPage directory = new EmployeeDirectoryPage();
        skipIfTenantAtEmployeeLimit(directory);

        int headcountBefore = directory.totalEmployees();
        note("Headcount before registration: " + headcountBefore);

        EmployeeData employee = TestDataFactory.validEmployee();
        note("Registering: " + employee.getFullName() + " / " + employee.getEmployeeId()
                + " / " + employee.getEmail());

        step("Opening the Register New Employee dialog");
        RegisterEmployeeDialog dialog = directory.openRegisterEmployeeDialog();
        assertTrue(dialog.isOpen(), "The Register New Employee dialog did not open");
        softly().assertTrue(dialog.hasAllSections(),
                "The dialog is missing one of its Personal / Employment / Identity / "
                        + "Banking sections");

        step("Completing the mandatory details and saving");
        dialog.fill(employee);
        dialog.saveExpectingSuccess();

        // Hard: the record existing is the point of the test.
        step("Verifying the new employee is listed in the directory");
        assertTrue(directory.containsEmployeeIn(employee.getDepartment(), employee.getFullName()),
                "The newly registered employee '" + employee.getFullName()
                        + "' is not listed in the directory");

        step("Verifying the headcount KPI rose by one");
        softly().assertTrue(directory.waitForKpiCard(
                        EmployeeDirectoryPage.KPI_TOTAL_EMPLOYEES, headcountBefore + 1),
                "The Total Employees KPI reported " + directory.totalEmployees()
                        + " after registration, expected " + (headcountBefore + 1));

        assertAllSoft();
        verified("The employee was registered and the directory reflects it");
    }

    @Test(priority = 3,
            groups = {"smoke", "employee", "negative", "validation"},
            description = "TC_EMP_03 - The registration form blocks every invalid dataset, "
                    + "raises the correct inline message and saves nothing")
    public void TC_EMP_03_registrationFormRejectsEveryInvalidDataset() {
        List<EmployeeData> scenarios = JsonDataReader.employeeValidationScenarios().stream()
                .filter(scenario -> scenario.getExpectedError() != null)
                .toList();
        note("Invalid registration scenarios under test: " + scenarios.size());

        step("Signing in as the Organization Administrator and opening the directory");
        loginAndOpen(Persona.ADMIN, AppModule.EMPLOYEE_DIRECTORY);
        EmployeeDirectoryPage directory = new EmployeeDirectoryPage();
        int headcountBefore = directory.totalEmployees();

        for (EmployeeData scenario : scenarios) {
            step("Scenario: " + scenario.getScenario());
            RegisterEmployeeDialog dialog = directory.openRegisterEmployeeDialog();
            dialog.fill(scenario);
            dialog.clickSave();

            // Hard: a closed dialog means an invalid record may have been saved.
            assertTrue(dialog.remainsOpenAfterSubmit(),
                    "The dialog closed for an invalid dataset (" + scenario.getScenario()
                            + "), which means the record may have been saved");

            var messages = dialog.validationMessages();
            note("Validation messages raised: " + messages);
            softly().assertTrue(dialog.showsValidationMessage(scenario.getExpectedError()),
                    scenario.getScenario() + " - expected '" + scenario.getExpectedError()
                            + "' on the " + scenario.getErroneousField()
                            + " field, but the form raised: " + messages);
            dialog.cancel();
        }

        // Hard: nothing may have been persisted by any rejected submission.
        step("Verifying no record was created by any rejected submission");
        assertEquals(directory.totalEmployees(), headcountBefore,
                "The headcount changed even though every submission was rejected - "
                        + "an invalid record may have been persisted");

        assertAllSoft();
        verified("All " + scenarios.size() + " invalid datasets were rejected");
    }

    @Test(priority = 4,
            groups = {"smoke", "employee", "e2e"},
            description = "TC_EMP_04 - A department can be created and then deleted, and "
                    + "the department count tracks both operations")
    public void TC_EMP_04_departmentCanBeCreatedAndDeletedWithCountTracking() {
        String departmentName = TestDataFactory.uniqueDepartmentName();

        step("Signing in as the Organization Administrator and opening Department Management");
        loginAndOpen(Persona.ADMIN, AppModule.DEPARTMENT);

        DepartmentPage departments = new DepartmentPage();
        assertTrue(departments.isLoaded(), "Department Management did not load");

        int countBefore = departments.departmentCount();
        note("Departments before: " + countBefore);

        try {
            step("Creating the department '" + departmentName + "'");
            departments.openCreateDialog();
            softly().assertEquals(departments.createDialogTitle(), "Create New Department",
                    "The create dialog has an unexpected title");
            departments.enterDepartmentName(departmentName).submitCreateDialog();

            assertTrue(departments.hasDepartment(departmentName),
                    "The new department '" + departmentName + "' is not listed");
            softly().assertTrue(departments.waitForKpiCard(
                            DepartmentPage.KPI_DEPARTMENTS, countBefore + 1),
                    "The department count reported " + departments.departmentCount()
                            + ", expected " + (countBefore + 1));
            softly().assertEquals(departments.headcountFor(departmentName), 0,
                    "A newly created department should start with zero employees");

            step("Deleting the department");
            departments.clickDeleteFor(departmentName);
            String confirmation = departments.deleteConfirmationText();
            note("Confirmation dialog: " + confirmation.replace("\n", " / "));
            softly().assertTrue(confirmation.contains(DepartmentPage.EMPTY_ONLY_DELETE_RULE),
                    "The delete confirmation does not state the rule '"
                            + DepartmentPage.EMPTY_ONLY_DELETE_RULE + "'. It read: " + confirmation);
            departments.confirmDelete();

            assertTrue(departments.departmentRemoved(departmentName),
                    "The department '" + departmentName + "' is still listed after deletion");
            softly().assertTrue(departments.waitForKpiCard(
                            DepartmentPage.KPI_DEPARTMENTS, countBefore),
                    "The department count did not return to " + countBefore);

            assertAllSoft();
            verified("The department was created and deleted, with the count tracking both");
        } finally {
            // Guarantees the tenant is left clean even if an assertion above failed.
            departments.deleteIfPresent(departmentName);
        }
    }
}
