package com.mywehr.tests.navigation;

import com.mywehr.base.BaseTest;
import com.mywehr.dataproviders.TestDataProviders;
import com.mywehr.enums.AppModule;
import com.mywehr.enums.Persona;
import com.mywehr.pages.base.BasePage;
import com.mywehr.pages.base.GenericModulePage;
import com.mywehr.pages.dashboard.DashboardPage;
import com.mywehr.pages.employees.DepartmentPage;
import com.mywehr.pages.employees.EmployeeDirectoryPage;
import com.mywehr.pages.timeleave.LeavesPage;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertTrue;

/**
 * TC_NAV - Every page loads, for every persona entitled to it.
 *
 * Content-based rather than URL-based: each module declares a landing marker
 * in {@link AppModule} and the test asserts that marker actually rendered. A
 * client-side router changes the URL long before the view has data, so a URL
 * check alone would pass on a blank screen.
 *
 * The module sweep runs in ONE session per persona with soft assertions, so a
 * single run reports every module that is broken, not just the first.
 */
public class ModuleLoadSmokeTest extends BaseTest {

    /** Strings that betray a crashed view or an unhandled server error. */
    private static final List<String> ERROR_SIGNATURES = List.of(
            "Something went wrong",
            "Unexpected Application Error",
            "500 Internal Server Error",
            "Cannot read properties of",
            "TypeError:",
            "ChunkLoadError");

    @Test(priority = 1,
            groups = {"smoke", "navigation", "critical"},
            dataProvider = "allPersonas",
            dataProviderClass = TestDataProviders.class,
            description = "TC_NAV_01 - The header is complete and every module the persona is "
                    + "entitled to renders its own content without an error state")
    public void TC_NAV_01_headerIsCompleteAndEveryPermittedModuleLoadsCleanly(Persona persona) {
        step("Signing in as " + persona.roleName());
        DashboardPage dashboard = loginAs(persona);

        step("Verifying the global header");
        var header = dashboard.header();
        note("Navigation offered: " + header.navigationLabels());
        for (String section : List.of("Dashboard", "Time & Leave", "Employees",
                "Documents", "Settings")) {
            softly().assertTrue(header.hasNavigationItem(section),
                    "The '" + section + "' section is missing from the header for " + persona);
        }
        var hrefs = header.navigationHrefs();
        for (AppModule linked : List.of(AppModule.DASHBOARD, AppModule.EMPLOYEE_DIRECTORY,
                AppModule.COMPANY_DOCUMENTS)) {
            softly().assertTrue(hrefs.contains(linked.route()),
                    "No header link points at " + linked.route() + " for " + persona);
        }
        softly().assertTrue(header.hasGlobalSearch(),
                "The global employee search is missing from the header for " + persona);

        List<AppModule> modules = AppModule.accessibleBy(persona);
        note(persona.roleName() + " is entitled to " + modules.size() + " modules");

        for (AppModule module : modules) {
            step("Opening " + module.name() + " at " + module.route());
            BasePage.navigateTo(module);
            GenericModulePage page = new GenericModulePage(module);

            if (!softlyLoaded(page, module.name() + " (for " + persona.roleName() + ")")) {
                note(module.name() + " showed: " + firstLines(page.contentText()));
                continue;
            }
            softly().assertEquals(page.currentPath(), module.route(),
                    module.name() + " redirected " + persona.roleName()
                            + " away from a route it is entitled to");

            String content = page.contentText();
            for (String signature : ERROR_SIGNATURES) {
                softly().assertFalse(content.contains(signature),
                        module.name() + " rendered an error state containing '" + signature
                                + "'. Screen showed: " + firstLines(content));
            }
        }

        assertAllSoft();
        verified("The header is complete and all " + modules.size()
                + " modules loaded cleanly for " + persona.roleName());
    }

    @Test(priority = 2,
            groups = {"smoke", "navigation"},
            description = "TC_NAV_02 - The Employees and Time & Leave section rails link to "
                    + "every sub-module and the links navigate")
    public void TC_NAV_02_sectionRailsLinkToEverySubModule() {
        step("Signing in as the Organization Administrator and opening Employees");
        loginAndOpen(Persona.ADMIN, AppModule.EMPLOYEE_DIRECTORY);

        var employeesRail = new EmployeeDirectoryPage().sideNav();
        assertTrue(employeesRail.isDisplayed(), "The Employees section rail did not render");
        note("Employees rail: " + employeesRail.itemLabels());

        step("Verifying the Employees rail links");
        for (AppModule module : List.of(AppModule.DEPARTMENT,
                AppModule.ORGANIZATION_STRUCTURE, AppModule.MY_TEAM)) {
            softly().assertTrue(employeesRail.linksToRoute(module.route()),
                    "The Employees rail does not link to " + module.route());
        }

        step("Following the Department link and verifying it navigates");
        employeesRail.clickItem("Department");
        softly().assertTrue(new DepartmentPage().isLoaded(),
                "Following the rail to Department did not load Department Management");

        step("Opening Time & Leave");
        BasePage.navigateTo(AppModule.DAILY_CHECK_IN);
        var timeLeaveRail = new LeavesPage().sideNav();
        assertTrue(timeLeaveRail.isDisplayed(), "The Time & Leave rail did not render");
        note("Time & Leave rail: " + timeLeaveRail.itemLabels());

        step("Verifying the Time & Leave rail links");
        for (AppModule module : List.of(AppModule.LEAVES,
                AppModule.LEAVE_CANCELLATION,
                AppModule.HOLIDAY_CALENDAR,
                AppModule.SHIFTS,
                AppModule.ATTENDANCE_POLICIES,
                AppModule.KIOSK_DEVICES,
                AppModule.FACE_ENROLLMENT)) {
            softly().assertTrue(timeLeaveRail.linksToRoute(module.route()),
                    "The Time & Leave rail does not link to " + module.route()
                            + " (" + module.name() + ")");
        }

        assertAllSoft();
        verified("Both section rails link to every sub-module");
    }

    private String firstLines(String text) {
        return text.lines().limit(6).reduce("", (a, b) -> a + " | " + b);
    }
}
