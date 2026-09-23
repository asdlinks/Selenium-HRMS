package com.mywehr.tests.configuration;

import com.mywehr.base.BaseTest;
import com.mywehr.enums.AppModule;
import com.mywehr.enums.Persona;
import com.mywehr.enums.SettingsPanel;
import com.mywehr.pages.base.BasePage;
import com.mywehr.pages.base.GenericModulePage;
import com.mywehr.pages.employees.EmployeeDirectoryPage;
import com.mywehr.pages.settings.SettingsPage;
import com.mywehr.pages.timeleave.HolidayCalendarPage;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertTrue;

/**
 * TC_CFG - Organization configuration that other modules depend on.
 *
 * Branches, designations, employment types, shifts and work modes are the
 * reference data the rest of the HRMS is built from - an employee cannot be
 * registered without a department and a designation, and attendance cannot be
 * evaluated without a shift.
 *
 * Each test sweeps one configuration area in a single session. A page that
 * fails to load is recorded as a failure and its own checks are skipped, but
 * the sweep carries on, so one run reports the state of the whole area.
 */
public class OrganizationConfigurationTest extends BaseTest {

    @Test(priority = 1,
            groups = {"smoke", "configuration", "critical"},
            description = "TC_CFG_01 - Organization Structure exposes the reference data "
                    + "other modules consume, and the Company Profile carries the locale and "
                    + "financial-year settings payroll derives from")
    public void TC_CFG_01_organizationSetupExposesReferenceDataAndCompanyProfile() {
        step("Signing in as the Organization Administrator");
        loginAs(Persona.ADMIN);

        step("Organization Structure - reference-data tabs and a configured branch");
        BasePage.navigateTo(AppModule.ORGANIZATION_STRUCTURE);
        GenericModulePage structure = new GenericModulePage(AppModule.ORGANIZATION_STRUCTURE);
        if (softlyLoaded(structure, "Organization Structure")) {
            for (String tab : List.of("Branches", "Designations", "Employment Types")) {
                softly().assertTrue(structure.displaysTextImmediately(tab),
                        "The '" + tab + "' tab is missing from Organization Structure");
            }
            softly().assertTrue(structure.displaysTextImmediately("Attendance"),
                    "Organization Structure does not describe its relationship to Attendance");
            softly().assertTrue(structure.displaysText("Thrissur"),
                    "No branch is configured - employees cannot be assigned a location");
        }

        step("Company Profile - locale and financial year");
        BasePage.navigateTo(AppModule.SETTINGS);
        SettingsPage settings = new SettingsPage();
        assertTrue(settings.isLoaded(), "The Settings page did not load");
        settings.openPanel(SettingsPanel.COMPANY_PROFILE);
        assertTrue(settings.isPanelOpen(SettingsPanel.COMPANY_PROFILE),
                "The Company Profile panel did not open - the panel shows '"
                        + settings.openPanelHeading() + "'");

        for (String expected : List.of("Locale & Financial Year", "INR", "DD/MM/YYYY",
                "April", "payslips", "Save Company Profile")) {
            softly().assertTrue(settings.displaysTextImmediately(expected),
                    "The Company Profile does not show '" + expected + "'");
        }

        assertAllSoft();
        verified("Organization Structure and the Company Profile carry the reference data "
                + "other modules depend on");
    }

    @Test(priority = 2,
            groups = {"smoke", "configuration"},
            description = "TC_CFG_02 - Attendance is fully configured: shifts, work modes, "
                    + "policies binding them, the Daily Check-In calendar and the holiday calendar")
    public void TC_CFG_02_attendanceIsConfiguredAcrossShiftsWorkModesPoliciesAndHolidays() {
        step("Signing in as the Organization Administrator");
        loginAs(Persona.ADMIN);

        step("Shift Management - working hours and assignment");
        BasePage.navigateTo(AppModule.SHIFTS);
        GenericModulePage shifts = new GenericModulePage(AppModule.SHIFTS);
        if (softlyLoaded(shifts, "Shift Management")) {
            softly().assertTrue(shifts.displaysText("General Shift"),
                    "No shift is defined - attendance has no working hours to evaluate against");
            softly().assertTrue(shifts.displaysTextImmediately("Assign Shift to Employee"),
                    "The shift assignment section is missing");
            softly().assertTrue(shifts.displaysTextImmediately("Effective From"),
                    "The Effective From control is missing from shift assignment");
        }

        step("Work Modes - the five standard modes");
        BasePage.navigateTo(AppModule.WORK_MODES);
        GenericModulePage workModes = new GenericModulePage(AppModule.WORK_MODES);
        if (softlyLoaded(workModes, "Work Modes")) {
            for (String mode : List.of("Office", "Work From Home", "Hybrid",
                    "Client Visit", "Field Work")) {
                softly().assertTrue(workModes.displaysTextImmediately(mode),
                        "The '" + mode + "' work mode is not configured");
            }
            softly().assertTrue(workModes.displaysTextImmediately("attendance policies"),
                    "Work Modes does not describe its relationship to attendance policies");
        }

        step("Attendance Policies - check-in methods bound to work modes");
        BasePage.navigateTo(AppModule.ATTENDANCE_POLICIES);
        GenericModulePage policies = new GenericModulePage(AppModule.ATTENDANCE_POLICIES);
        if (softlyLoaded(policies, "Attendance Policies")) {
            for (String column : List.of("Name", "Type", "Allowed Methods", "Assigned", "Status")) {
                softly().assertTrue(policies.displaysTextImmediately(column),
                        "The '" + column + "' column is missing from the policy table");
            }
            softly().assertTrue(policies.displaysTextImmediately("Field Work")
                            || policies.displaysTextImmediately("Client Visit"),
                    "No policy references a configured work mode");
            softly().assertTrue(policies.displaysTextImmediately("New Policy"),
                    "The New Policy control is missing");
        }

        step("Daily Check-In - monthly summary and legend");
        BasePage.navigateTo(AppModule.DAILY_CHECK_IN);
        GenericModulePage checkIn = new GenericModulePage(AppModule.DAILY_CHECK_IN);
        if (softlyLoaded(checkIn, "Daily Check-In")) {
            for (String label : List.of("Present Days", "On Leave", "Absent Days",
                    "Attendance Rate", "Present", "Leave", "Absent", "Holiday",
                    "Attendance History")) {
                softly().assertTrue(checkIn.displaysTextImmediately(label),
                        "'" + label + "' is missing from Daily Check-In");
            }
        }

        step("Holiday Calendar - populated, scoped by location, editable");
        BasePage.navigateTo(AppModule.HOLIDAY_CALENDAR);
        HolidayCalendarPage holidays = new HolidayCalendarPage();
        if (softlyLoaded(holidays, "Holiday Calendar")) {
            int count = holidays.holidayCountThisYear();
            note("Holidays configured this year: " + count);
            softly().assertTrue(count > 0,
                    "No holidays are configured this year - attendance has no off-days to apply");
            softly().assertTrue(holidays.displaysTextImmediately("Next Holiday"),
                    "The Next Holiday card is missing from the calendar");
            softly().assertTrue(holidays.hasLocationFilter(),
                    "The Location filter is missing - holidays cannot be scoped per office");
            softly().assertTrue(holidays.canAddHoliday(),
                    "The Add Holiday control is missing for the Administrator");
        }

        assertAllSoft();
        verified("Shifts, work modes, policies, check-in and holidays are all configured");
    }

    @Test(priority = 3,
            groups = {"smoke", "configuration", "reports"},
            description = "TC_CFG_03 - The Reports workspace aggregates live figures from every "
                    + "operational module, consistent with the Employee Directory")
    public void TC_CFG_03_reportsWorkspaceAggregatesFiguresConsistentWithTheDirectory() {
        step("Signing in as the Organization Administrator and opening Reports");
        loginAndOpen(Persona.ADMIN, AppModule.REPORTS);

        GenericModulePage reports = new GenericModulePage(AppModule.REPORTS);
        assertTrue(reports.isLoaded(), "The Reports workspace did not load");

        step("Verifying the KPI strip, analytics workspaces and charts");
        for (String kpi : List.of("Total Employees", "Present Today", "On Leave",
                "Pending Approvals", "New Joiners (MTD)")) {
            softly().assertTrue(reports.displaysTextImmediately(kpi),
                    "The '" + kpi + "' figure is missing from the executive dashboard");
        }
        for (String workspace : List.of("Employees", "Attendance", "Leave", "Payroll",
                "Organization", "Compliance")) {
            softly().assertTrue(reports.displaysTextImmediately(workspace),
                    "The '" + workspace + "' analytics workspace is missing");
        }
        for (String chart : List.of("Department Distribution", "Work Mode Distribution")) {
            softly().assertTrue(reports.displaysTextImmediately(chart),
                    "The " + chart + " chart is missing");
        }

        step("Verifying the reported headcount agrees with the Employee Directory");
        int reported = reports.readSettledKpiCard("Total Employees");
        BasePage.navigateTo(AppModule.EMPLOYEE_DIRECTORY);
        int directory = new EmployeeDirectoryPage().totalEmployees();
        note("Headcount - Reports: " + reported + ", Directory: " + directory);
        softly().assertEquals(reported, directory,
                "Reports claims " + reported + " employees but the Employee Directory reports "
                        + directory);

        assertAllSoft();
        verified("The Reports workspace aggregates consistent figures across modules");
    }
}
