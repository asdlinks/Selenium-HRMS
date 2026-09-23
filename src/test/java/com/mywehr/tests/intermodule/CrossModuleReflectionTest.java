package com.mywehr.tests.intermodule;

import com.mywehr.base.BaseTest;
import com.mywehr.data.TestDataFactory;
import com.mywehr.data.model.EmployeeData;
import com.mywehr.enums.AppModule;
import com.mywehr.enums.LeaveStatus;
import com.mywehr.enums.Persona;
import com.mywehr.pages.base.BasePage;
import com.mywehr.pages.dashboard.DashboardPage;
import com.mywehr.pages.documents.CompanyDocumentsPage;
import com.mywehr.pages.employees.DepartmentPage;
import com.mywehr.pages.employees.EmployeeDirectoryPage;
import com.mywehr.pages.timeleave.HolidayCalendarPage;
import com.mywehr.pages.timeleave.LeavesPage;
import com.mywehr.utils.DateUtils;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/**
 * TC_XMOD - Inter-module data reflection.
 *
 * WHY THESE MATTER MORE THAN THE REST
 * -----------------------------------
 * An HRMS is a set of modules sharing one dataset. The interesting defects are
 * rarely "the page did not load" - they are "the department was created but
 * the directory still shows the old count". Every test here makes a change in
 * one module (hard-asserted: without it there is nothing to propagate) and
 * then checks each downstream module softly, so one run reports every module
 * the change failed to reach.
 *
 * Each test cleans up after itself so the suite can run repeatedly against the
 * same tenant.
 */
public class CrossModuleReflectionTest extends BaseTest {

    @Test(priority = 1,
            groups = {"smoke", "intermodule", "e2e", "critical"},
            description = "TC_XMOD_01 - A department created in Department Management is "
                    + "reflected in the Employee Directory KPI and offered as a filter option")
    public void TC_XMOD_01_newDepartmentPropagatesToDirectoryKpiAndFilterOptions() {
        String departmentName = TestDataFactory.uniqueDepartmentName();

        step("Signing in as the Organization Administrator");
        loginAs(Persona.ADMIN);

        step("Recording the Employee Directory department count before the change");
        BasePage.navigateTo(AppModule.EMPLOYEE_DIRECTORY);
        EmployeeDirectoryPage directory = new EmployeeDirectoryPage();
        int directoryCountBefore = directory.departmentCount();
        note("Employee Directory reports " + directoryCountBefore + " departments");

        DepartmentPage departments = new DepartmentPage();
        try {
            step("MODULE 1 - creating the department '" + departmentName + "'");
            departments.open();
            int moduleCountBefore = departments.departmentCount();
            departments.createDepartment(departmentName);

            assertTrue(departments.hasDepartment(departmentName),
                    "The department was not created in its own module");
            softly().assertTrue(departments.waitForKpiCard(
                            DepartmentPage.KPI_DEPARTMENTS, moduleCountBefore + 1),
                    "Department Management did not increment its own count");

            step("MODULE 2 - verifying the Employee Directory KPI picked up the change");
            directory.open();
            softly().assertTrue(directory.waitForKpiCard(
                            EmployeeDirectoryPage.KPI_DEPARTMENTS, directoryCountBefore + 1),
                    "The Employee Directory still reports " + directory.departmentCount()
                            + " departments, expected " + (directoryCountBefore + 1));

            // The Dashboard's headcount chart plots only departments with
            // members, so a new empty department legitimately does not appear
            // there. The filter dropdown is built from the same reference data
            // and must still offer it.
            step("MODULE 2 - verifying the department is offered as a filter option");
            var filterOptions = directory.availableDepartmentFilterOptions();
            note("Department filter offers: " + filterOptions);
            softly().assertTrue(filterOptions.stream().anyMatch(o -> o.contains(departmentName)),
                    "The new department '" + departmentName + "' is not offered by the "
                            + "department filter. Options were: " + filterOptions);

            assertAllSoft();
            verified("The new department reached Department Management, the Directory KPI "
                    + "and its filter options");
        } finally {
            departments.deleteIfPresent(departmentName);
        }
    }

    @Test(priority = 2,
            groups = {"smoke", "intermodule", "e2e", "critical"},
            description = "TC_XMOD_02 - A newly registered employee raises the headcount "
                    + "consistently across the Directory, the Dashboard and their department")
    public void TC_XMOD_02_newEmployeeRaisesHeadcountAcrossDirectoryDashboardAndDepartment() {
        EmployeeData employee = TestDataFactory.validEmployeeIn("Sales");

        step("Signing in as the Organization Administrator");
        DashboardPage dashboard = loginAs(Persona.ADMIN);

        step("Recording every module's headcount before the change");
        EmployeeDirectoryPage directory = new EmployeeDirectoryPage();
        directory.open();
        skipIfTenantAtEmployeeLimit(directory);
        int directoryBefore = directory.totalEmployees();

        dashboard.open();
        int workforceBefore = dashboard.totalWorkforce();

        DepartmentPage departments = new DepartmentPage();
        departments.open();
        int salesBefore = departments.headcountFor("Sales");
        note("Before - Directory: " + directoryBefore + ", Dashboard: " + workforceBefore
                + ", Sales: " + salesBefore);
        softly().assertEquals(directoryBefore, workforceBefore,
                "The Directory (" + directoryBefore + ") and the Dashboard (" + workforceBefore
                        + ") disagree on headcount before any change");

        step("MODULE 1 - registering " + employee.getFullName() + " into Sales");
        directory.open();
        directory.openRegisterEmployeeDialog().fill(employee).saveExpectingSuccess();
        assertTrue(directory.containsEmployeeIn(employee.getDepartment(), employee.getFullName()),
                "The new employee is not listed in the directory");
        softly().assertTrue(directory.waitForKpiCard(
                        EmployeeDirectoryPage.KPI_TOTAL_EMPLOYEES, directoryBefore + 1),
                "The Employee Directory reports " + directory.totalEmployees()
                        + " employees, expected " + (directoryBefore + 1));

        step("MODULE 2 - verifying the Dashboard workforce figure rose by one");
        dashboard.open();
        softly().assertTrue(dashboard.waitForKpiCard(
                        DashboardPage.KPI_TOTAL_WORKFORCE, workforceBefore + 1),
                "The Dashboard reports a workforce of " + dashboard.totalWorkforce()
                        + ", expected " + (workforceBefore + 1));

        step("MODULE 3 - verifying the Sales department absorbed the new member");
        departments.open();
        softly().assertEquals(departments.headcountFor("Sales"), salesBefore + 1,
                "The Sales department reports " + departments.headcountFor("Sales")
                        + " members, expected " + (salesBefore + 1));
        softly().assertTrue(departments.hasDepartment(employee.getFullName()),
                "The new employee is not listed under the Sales department");

        note("The employee is left in place - the app offers deactivation rather than "
                + "hard deletion, and removing a person is not something an unattended "
                + "suite should do.");

        assertAllSoft();
        verified("The new employee raised the headcount consistently in all three modules");
    }

    @Test(priority = 3,
            groups = {"smoke", "intermodule", "e2e"},
            description = "TC_XMOD_03 - A holiday added to the calendar is listed there and "
                    + "promoted by the Dashboard's next-holiday widget")
    public void TC_XMOD_03_newHolidayPropagatesToCalendarAndDashboardWidget() {
        String holidayName = TestDataFactory.uniqueHolidayName();
        // Two days out, so it is the soonest upcoming holiday and therefore the
        // one the dashboard promotes.
        String holidayDate = DateUtils.forDateInput(2);

        step("Signing in as the Organization Administrator");
        DashboardPage dashboard = loginAs(Persona.ADMIN);
        note("The Dashboard currently promotes: '" + dashboard.nextHolidayName() + "'");

        HolidayCalendarPage holidays = new HolidayCalendarPage();
        try {
            step("MODULE 1 - adding the holiday '" + holidayName + "' on " + holidayDate);
            holidays.open();
            assertTrue(holidays.isLoaded(), "The Holiday Calendar did not load");
            assertTrue(holidays.canAddHoliday(),
                    "The Administrator is not offered the Add Holiday control");
            holidays.addHoliday(holidayName, holidayDate);
            assertTrue(holidays.hasHoliday(holidayName),
                    "The holiday '" + holidayName + "' is not listed in the calendar");

            step("MODULE 2 - verifying the Dashboard promoted it as the next holiday");
            dashboard.open();
            softly().assertTrue(dashboard.displaysText(holidayName),
                    "The Dashboard does not show the new holiday '" + holidayName
                            + "'. It still promotes '" + dashboard.nextHolidayName() + "'");

            assertAllSoft();
            verified("The new holiday reached both the calendar and the Dashboard widget");
        } finally {
            holidays.deleteIfPresent(holidayName);
        }
    }

    @Test(priority = 4,
            groups = {"smoke", "intermodule", "critical"},
            description = "TC_XMOD_04 - The Dashboard's pending-approval and workforce figures "
                    + "agree with the Leave, Employee Directory and Department modules")
    public void TC_XMOD_04_dashboardFiguresAgreeWithLeaveDirectoryAndDepartmentModules() {
        step("Signing in as the Organization Administrator and reading the Dashboard");
        DashboardPage dashboard = loginAs(Persona.ADMIN);

        int dashboardPending = dashboard.pendingRequests();
        int dashboardWorkforce = dashboard.totalWorkforce();
        note("Dashboard - pending: " + dashboardPending + ", workforce: " + dashboardWorkforce);
        // Hard: every comparison below is against these two figures.
        assertTrue(dashboardPending >= 0, "The Dashboard pending-requests KPI did not resolve");
        assertTrue(dashboardWorkforce > 0, "The Dashboard workforce KPI did not resolve");
        softly().assertTrue(dashboard.hasPendingApprovalsWidget(),
                "The Pending Approvals widget is missing from the Dashboard");

        step("Leave Management - pending count");
        BasePage.navigateTo(AppModule.LEAVES);
        LeavesPage leaves = new LeavesPage();
        if (softlyLoaded(leaves, "Leave Management")) {
            int leavePending = leaves.tabCount(LeaveStatus.PENDING);
            note("Leave module pending: " + leavePending);
            softly().assertEquals(dashboardPending, leavePending,
                    "The Dashboard reports " + dashboardPending + " pending requests but the "
                            + "Leave module reports " + leavePending);
        }

        step("Employee Directory - headcount");
        BasePage.navigateTo(AppModule.EMPLOYEE_DIRECTORY);
        int directoryTotal = new EmployeeDirectoryPage().totalEmployees();
        note("Employee Directory headcount: " + directoryTotal);
        softly().assertEquals(directoryTotal, dashboardWorkforce,
                "The Employee Directory reports " + directoryTotal
                        + " but the Dashboard reports " + dashboardWorkforce);

        step("Department Management - headcount");
        BasePage.navigateTo(AppModule.DEPARTMENT);
        int departmentTotal = new DepartmentPage().totalEmployees();
        note("Department module headcount: " + departmentTotal);
        softly().assertEquals(departmentTotal, dashboardWorkforce,
                "The Department module reports " + departmentTotal
                        + " but the Dashboard reports " + dashboardWorkforce);

        assertAllSoft();
        verified("The Dashboard agrees with the Leave, Directory and Department modules");
    }

    @Test(priority = 5,
            groups = {"smoke", "intermodule", "documents"},
            description = "TC_XMOD_05 - Company Documents published by the Administrator are "
                    + "visible to HR with full column metadata")
    public void TC_XMOD_05_publishedDocumentsAreVisibleToBothPersonasWithMetadata() {
        step("Signing in as the Organization Administrator and opening Company Documents");
        loginAndOpen(Persona.ADMIN, AppModule.COMPANY_DOCUMENTS);

        CompanyDocumentsPage adminDocuments = new CompanyDocumentsPage();
        assertTrue(adminDocuments.isLoaded(), "Company Documents did not load for the Administrator");

        softly().assertTrue(adminDocuments.hasAllExpectedColumns(),
                "The document grid is missing columns: " + adminDocuments.missingColumns());

        int adminVisibleCount = adminDocuments.documentCount();
        note("The Administrator can see " + adminVisibleCount + " active document(s)");
        // Hard: with nothing published there is nothing to compare across personas.
        assertTrue(adminVisibleCount > 0,
                "No documents are published in this tenant, so cross-persona visibility "
                        + "cannot be verified");

        step("Verifying the upload dialog exposes its publishing controls");
        adminDocuments.openUploadDialog();
        softly().assertEquals(adminDocuments.uploadDialogTitle(), "Upload Document",
                "The upload dialog has an unexpected title");
        softly().assertTrue(adminDocuments.uploadDialogHasAllFields(),
                "The upload dialog is missing one of Title / Category / Effective Date / "
                        + "Share with All Employees");
        adminDocuments.closeUploadDialog();

        step("Signing out and signing in as the HR Administrator");
        new DashboardPage().header().logout();
        loginAndOpen(Persona.HR, AppModule.COMPANY_DOCUMENTS);

        CompanyDocumentsPage hrDocuments = new CompanyDocumentsPage();
        assertTrue(hrDocuments.isLoaded(), "Company Documents did not load for HR");

        step("Verifying HR sees the same documents and holds the publishing right");
        softly().assertEquals(hrDocuments.documentCount(), adminVisibleCount,
                "HR can see " + hrDocuments.documentCount() + " document(s) but the "
                        + "Administrator sees " + adminVisibleCount);
        softly().assertTrue(hrDocuments.canUpload(),
                "HR is not offered the Upload Document control");

        assertAllSoft();
        verified("Published documents and the publishing right reach both personas");
    }
}
