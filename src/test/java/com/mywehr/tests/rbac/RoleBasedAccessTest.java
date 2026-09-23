package com.mywehr.tests.rbac;

import com.mywehr.base.BaseTest;
import com.mywehr.dataproviders.TestDataProviders;
import com.mywehr.enums.AppModule;
import com.mywehr.enums.Persona;
import com.mywehr.enums.SettingsPanel;
import com.mywehr.pages.base.BasePage;
import com.mywehr.pages.base.GenericModulePage;
import com.mywehr.pages.employees.EmployeeDirectoryPage;
import com.mywehr.pages.settings.SettingsPage;
import com.mywehr.pages.timeleave.LeavesPage;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * TC_RBAC - What each persona can and cannot reach.
 *
 * The product exposes two authorisation surfaces, and this class covers both:
 *
 *   ROUTE GUARDS    - /work-modes, /attendance/policies, /attendance/kiosk-devices
 *                     and /attendance/face-enrollment silently redirect an
 *                     unauthorised persona to /dashboard.
 *
 *   RENDERED MENUS  - Settings is a single route whose left rail is built from
 *                     the caller's permissions. An Administrator sees twelve
 *                     panels; HR sees four.
 *
 * DENIALS ARE HARD ASSERTIONS. A persona reaching something it must not is a
 * security defect in its own right, so the test stops there. Checks that a
 * permitted feature is present are soft, so one run lists everything missing.
 *
 * Each denial is paired with the matching Administrator permit, because a test
 * that only asserts "HR cannot" would still pass if the route were broken for
 * everyone.
 */
public class RoleBasedAccessTest extends BaseTest {

    @Test(priority = 1,
            groups = {"smoke", "rbac", "security", "critical"},
            description = "TC_RBAC_01 - HR is redirected away from every restricted route "
                    + "and is shown only the settings panels its role permits")
    public void TC_RBAC_01_hrIsDeniedRestrictedRoutesAndOrganizationConfiguration() {
        step("Signing in as the HR Administrator");
        loginAs(Persona.HR);

        List<AppModule> denied = AppModule.deniedTo(Persona.HR);
        assertFalse(denied.isEmpty(),
                "The access matrix declares no HR-restricted routes - this test would be vacuous");

        for (AppModule module : denied) {
            step("Deep-linking to the restricted route " + module.route());
            BasePage.navigateTo(module);
            GenericModulePage page = new GenericModulePage(module);

            assertEquals(page.currentPath(), AppModule.ACCESS_DENIED_FALLBACK_ROUTE,
                    "HR reached " + module.route() + " but should have been redirected to "
                            + AppModule.ACCESS_DENIED_FALLBACK_ROUTE);
            assertFalse(page.displaysTextImmediately(module.landingMarker()),
                    "Content from the restricted module " + module.name() + " ('"
                            + module.landingMarker() + "') was rendered to HR despite the redirect");
            verified("HR is denied " + module.name());
        }

        step("Opening Settings as HR");
        BasePage.navigateTo(AppModule.SETTINGS);
        SettingsPage settings = new SettingsPage();
        assertTrue(settings.isLoaded(), "The Settings page did not load for HR");
        note("Panels visible to HR: " + settings.visiblePanelLabels());

        step("Verifying the organization-configuration panels are hidden");
        for (SettingsPanel panel : SettingsPanel.hiddenFrom(Persona.HR)) {
            assertFalse(settings.hasPanel(panel),
                    "HR can see the restricted settings panel '" + panel.label()
                            + "' (group " + panel.group() + "), which belongs to the "
                            + "Organization Administrator");
        }

        step("Verifying the panels HR is entitled to are offered and open");
        for (SettingsPanel panel : SettingsPanel.inPlacePanelsVisibleTo(Persona.HR)) {
            if (!settings.hasPanel(panel)) {
                softly().fail("HR should see the '" + panel.label() + "' panel but it is missing");
                continue;
            }
            settings.openPanel(panel);
            softly().assertTrue(settings.isPanelOpen(panel),
                    "The '" + panel.label() + "' panel did not open for HR. Expected the heading '"
                            + panel.contentHeading() + "' but the panel shows '"
                            + settings.openPanelHeading() + "'");
        }

        assertAllSoft();
        verified("HR is held to its role on every route and settings panel");
    }

    @Test(priority = 2,
            groups = {"smoke", "rbac", "security", "critical"},
            description = "TC_RBAC_02 - The Administrator reaches every route denied to HR, sees "
                    + "the complete settings catalogue and governs roles with live user counts")
    public void TC_RBAC_02_administratorHoldsFullAccessIncludingRoleGovernance() {
        step("Signing in as the Organization Administrator");
        loginAs(Persona.ADMIN);

        // Proves the HR denials are a permission boundary, not broken pages.
        for (AppModule module : AppModule.deniedTo(Persona.HR)) {
            step("Opening " + module.name() + " as the Administrator");
            BasePage.navigateTo(module);
            GenericModulePage page = new GenericModulePage(module);
            softly().assertTrue(page.isLoaded(),
                    module.name() + " did not load for the Administrator either - the HR "
                            + "denial may be a broken page rather than a permission boundary");
            softly().assertNotEquals(page.currentPath(), AppModule.ACCESS_DENIED_FALLBACK_ROUTE,
                    "The Administrator was also redirected away from " + module.route());
        }

        step("Opening Settings as the Administrator");
        BasePage.navigateTo(AppModule.SETTINGS);
        SettingsPage settings = new SettingsPage();
        assertTrue(settings.isLoaded(), "The Settings page did not load for the Administrator");
        note("Panels visible to the Administrator: " + settings.visiblePanelLabels());

        step("Verifying the complete settings catalogue");
        for (SettingsPanel panel : SettingsPanel.values()) {
            softly().assertTrue(settings.hasPanel(panel),
                    "The Administrator cannot see the '" + panel.label() + "' panel");
        }
        for (String group : List.of("ORGANIZATION", "ATTENDANCE", "PAYROLL", "SECURITY", "ADVANCED")) {
            softly().assertTrue(settings.hasGroup(group),
                    "The '" + group + "' settings group is missing for the Administrator");
        }

        step("Opening Roles & Permissions");
        settings.openPanel(SettingsPanel.ROLES_AND_PERMISSIONS);
        assertTrue(settings.isPanelOpen(SettingsPanel.ROLES_AND_PERMISSIONS),
                "The Roles & Permissions panel did not open - the panel shows '"
                        + settings.openPanelHeading() + "'");

        step("Verifying both persona roles are listed with live user counts");
        for (Persona persona : Persona.values()) {
            softly().assertTrue(settings.listsRole(persona.roleName()),
                    "The '" + persona.roleName() + "' role is missing from the catalogue");
            int users = settings.userCountFor(persona.roleName());
            note(persona.roleName() + " users: " + users);
            softly().assertTrue(users >= 1,
                    "The " + persona.roleName() + " role reports " + users
                            + " users, but the " + persona + " persona holds it");
        }

        assertAllSoft();
        verified("The Administrator holds full access, including role governance");
    }

    @Test(priority = 3,
            groups = {"smoke", "rbac"},
            dataProvider = "allPersonas",
            dataProviderClass = TestDataProviders.class,
            description = "TC_RBAC_03 - Both personas retain the people-operations rights their "
                    + "roles depend on, proving scoping rather than blanket restriction")
    public void TC_RBAC_03_bothPersonasRetainSharedPeopleOperationsRights(Persona persona) {
        step("Signing in as " + persona.roleName() + " and opening the Employee Directory");
        loginAndOpen(persona, AppModule.EMPLOYEE_DIRECTORY);

        EmployeeDirectoryPage directory = new EmployeeDirectoryPage();
        assertTrue(directory.isLoaded(),
                "The Employee Directory did not load for " + persona.roleName());

        step("Verifying employee administration and the per-row actions");
        softly().assertTrue(directory.canAddEmployee(),
                persona.roleName() + " cannot register a new employee");
        softly().assertTrue(directory.canExport(),
                persona.roleName() + " cannot export the directory");
        for (String action : List.of("View Profile", "Edit", "Reset Password")) {
            softly().assertTrue(directory.offersAction(action),
                    "The '" + action + "' row action is not offered to " + persona.roleName());
        }

        step("Opening Leave Management");
        BasePage.navigateTo(AppModule.LEAVES);
        LeavesPage leaves = new LeavesPage();
        if (softlyLoaded(leaves, "Leave Management (for " + persona.roleName() + ")")) {
            softly().assertTrue(leaves.canApplyForLeave(),
                    persona.roleName() + " cannot apply for leave");
        }

        assertAllSoft();
        verified(persona.roleName() + " retains its people-operations rights");
    }
}
