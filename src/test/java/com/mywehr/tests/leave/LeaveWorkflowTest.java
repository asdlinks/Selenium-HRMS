package com.mywehr.tests.leave;

import com.mywehr.base.BaseTest;
import com.mywehr.data.TestDataFactory;
import com.mywehr.data.model.LeaveRequestData;
import com.mywehr.dataproviders.TestDataProviders;
import com.mywehr.enums.AppModule;
import com.mywehr.enums.LeaveStatus;
import com.mywehr.enums.LeaveType;
import com.mywehr.enums.Persona;
import com.mywehr.pages.dashboard.DashboardPage;
import com.mywehr.pages.timeleave.ApplyLeaveDrawer;
import com.mywehr.pages.timeleave.LeavesPage;
import com.mywehr.utils.DateUtils;
import org.testng.annotations.Test;

import java.time.LocalDate;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * TC_LEAVE - The leave request workflow.
 *
 * The end-to-end case deliberately spans both personas: the Administrator
 * records a request, then HR signs in separately and sees it. That crosses a
 * real session boundary, which is the only way to prove the request was
 * persisted rather than held in client state.
 */
public class LeaveWorkflowTest extends BaseTest {

    /** Seeded employee used as the leave subject. */
    private static final String TARGET_EMPLOYEE = "sales1";

    @Test(priority = 1,
            groups = {"smoke", "leave", "critical"},
            dataProvider = "allPersonas",
            dataProviderClass = TestDataProviders.class,
            description = "TC_LEAVE_01 - Leave Management renders valid entitlement cards and "
                    + "the request grid, and its status tab counts reconcile with the grid")
    public void TC_LEAVE_01_leaveManagementRendersAndStatusCountsReconcile(Persona persona) {
        step("Signing in as " + persona.roleName() + " and opening Leave Management");
        loginAndOpen(persona, AppModule.LEAVES);

        LeavesPage leaves = new LeavesPage();
        assertTrue(leaves.isLoaded(), "Leave Management did not load for " + persona.roleName());

        step("Verifying the entitlement cards");
        softly().assertTrue(leaves.allBalanceCardsDisplayed(),
                "One of the Casual / Sick / Paid balance cards is missing for " + persona.roleName());
        for (LeaveType type : LeaveType.values()) {
            int remaining = leaves.remainingBalance(type);
            int total = leaves.totalEntitlement(type);
            note(persona.roleName() + " " + type.gridLabel() + " balance: " + remaining + "/" + total);
            softly().assertTrue(total > 0,
                    "The " + type.gridLabel() + " entitlement is " + total + " - the card did not resolve");
            softly().assertTrue(remaining >= 0 && remaining <= total,
                    "The " + type.gridLabel() + " balance shows " + remaining + " of " + total
                            + ", which is impossible");
        }

        step("Verifying the request grid");
        softly().assertTrue(leaves.hasRecentRequestsSection(), "The Recent Requests section is missing");
        softly().assertTrue(leaves.isGridDisplayed(), "The leave request grid did not render");

        step("Verifying the status tab counts reconcile");
        int all = leaves.tabCount(LeaveStatus.ALL);
        int pending = leaves.tabCount(LeaveStatus.PENDING);
        int approved = leaves.tabCount(LeaveStatus.APPROVED);
        int rejected = leaves.tabCount(LeaveStatus.REJECTED);
        int cancelled = leaves.tabCount(LeaveStatus.CANCELLED);
        note(String.format("Tab counts - All: %d, Pending: %d, Approved: %d, Rejected: %d, "
                + "Cancelled: %d", all, pending, approved, rejected, cancelled));

        softly().assertEquals(pending + approved + rejected + cancelled, all,
                "The status tabs do not reconcile: Pending(" + pending + ") + Approved(" + approved
                        + ") + Rejected(" + rejected + ") + Cancelled(" + cancelled
                        + ") should equal All(" + all + ")");
        softly().assertEquals(leaves.totalRequestCount(), all,
                "The grid reports " + leaves.totalRequestCount() + " requests but the All tab claims "
                        + all);

        leaves.selectStatusTab(LeaveStatus.PENDING);
        softly().assertEquals(leaves.totalRequestCount(), pending,
                "The Pending tab shows " + leaves.totalRequestCount()
                        + " rows but its own count says " + pending);

        assertAllSoft();
        verified("Leave Management renders completely and reconciles for " + persona.roleName());
    }

    @Test(priority = 2,
            groups = {"smoke", "leave", "e2e", "intermodule", "critical"},
            description = "TC_LEAVE_02 - Leave the Administrator records on an employee's behalf "
                    + "is approved on save and HR, in a separate session, sees the same record")
    public void TC_LEAVE_02_leaveRecordedByAdminIsAutoApprovedAndVisibleToHr() {
        LeaveRequestData request = TestDataFactory.singleDayLeave(TARGET_EMPLOYEE);
        LocalDate leaveDay = DateUtils.fromDateInput(request.getStartDate());
        String dayMonth = DateUtils.gridDayMonth(leaveDay);
        String year = String.valueOf(leaveDay.getYear());

        // ---------- Part 1: the Administrator records the request ----------
        step("Signing in as the Organization Administrator and opening Leave Management");
        loginAndOpen(Persona.ADMIN, AppModule.LEAVES);

        LeavesPage adminLeaves = new LeavesPage();
        int pendingBefore = adminLeaves.tabCount(LeaveStatus.PENDING);
        int approvedBefore = adminLeaves.tabCount(LeaveStatus.APPROVED);
        note("Before: " + pendingBefore + " pending, " + approvedBefore + " approved");

        step("Opening the Apply for Leave drawer");
        ApplyLeaveDrawer drawer = adminLeaves.openApplyLeaveDrawer();
        assertTrue(drawer.isOpen(), "The Apply for Leave drawer did not open");
        // Hard: without the target field the Administrator cannot apply on anyone's behalf.
        assertTrue(drawer.hasTargetEmployeeField(),
                "The Target Employee field is missing - the Administrator should be able to "
                        + "apply on behalf of an employee");
        softly().assertTrue(drawer.allFieldsDisplayed(),
                "The leave drawer is missing one of its required controls");

        step("Recording a single-day casual leave for " + TARGET_EMPLOYEE + " on "
                + request.getStartDate());
        drawer.fill(request).submitExpectingSuccess();

        // Leave an Administrator records for someone else is an HR decision
        // already taken, so the app stores it as Approved rather than queueing it.
        //
        // The count rising is HARD because it is the only proof a NEW record
        // was saved: a matching row can already exist from an earlier run on
        // the same date, and the app does not record an overlapping request.
        step("Verifying a new request was saved directly as Approved");
        assertTrue(adminLeaves.waitForTabCount(LeaveStatus.APPROVED, approvedBefore + 1),
                "The Approved tab reported " + adminLeaves.tabCount(LeaveStatus.APPROVED)
                        + " after recording, expected " + (approvedBefore + 1)
                        + " - no new request was saved. If an earlier run already recorded "
                        + TARGET_EMPLOYEE + " on " + dayMonth + " " + year
                        + ", the app refuses the overlapping request.");
        assertTrue(adminLeaves.hasRequestWithStatus(TARGET_EMPLOYEE, LeaveStatus.APPROVED,
                        dayMonth, year),
                "No Approved request for " + TARGET_EMPLOYEE + " on " + dayMonth + " " + year
                        + " is listed after recording it");
        softly().assertEquals(adminLeaves.tabCount(LeaveStatus.PENDING), pendingBefore,
                "A request recorded by the Administrator should not enter the pending queue");

        step("Signing out of the Administrator session");
        new DashboardPage().header().logout();

        // ---------- Part 2: HR sees the same record in its own session ----------
        step("Signing in as the HR Administrator in a fresh session");
        loginAndOpen(Persona.HR, AppModule.LEAVES);

        LeavesPage hrLeaves = new LeavesPage();
        assertTrue(hrLeaves.isLoaded(), "Leave Management did not load for HR");

        step("Verifying the approved request persisted across the session boundary");
        assertTrue(hrLeaves.hasRequestWithStatus(TARGET_EMPLOYEE, LeaveStatus.APPROVED,
                        dayMonth, year),
                "HR cannot see the Approved request the Administrator recorded for "
                        + TARGET_EMPLOYEE + " on " + dayMonth + " " + year);
        softly().assertEquals(hrLeaves.tabCount(LeaveStatus.APPROVED), approvedBefore + 1,
                "HR's Approved count disagrees with the Administrator's");

        assertAllSoft();
        verified("Leave recorded by the Administrator was approved on save and is visible to HR");
    }

    @Test(priority = 3,
            groups = {"smoke", "leave", "negative", "validation"},
            description = "TC_LEAVE_03 - Submitting a leave request without its mandatory "
                    + "dates is rejected and nothing is recorded")
    public void TC_LEAVE_03_leaveSubmissionWithoutMandatoryDatesIsRejected() {
        step("Signing in as the Organization Administrator and opening Leave Management");
        loginAndOpen(Persona.ADMIN, AppModule.LEAVES);

        LeavesPage leaves = new LeavesPage();
        int totalBefore = leaves.totalRequestCount();
        note("Total requests before the invalid submission: " + totalBefore);

        step("Filling everything except the mandatory start and end dates");
        ApplyLeaveDrawer drawer = leaves.openApplyLeaveDrawer();
        drawer.fill(TestDataFactory.leaveWithoutDates(TARGET_EMPLOYEE));
        assertTrue(drawer.isStartDateEmpty(),
                "The start date was populated - this test cannot prove date validation");

        step("Submitting and expecting rejection");
        drawer.clickRecordLeave();

        // Both hard: either failing means an incomplete request may have been saved.
        assertTrue(drawer.remainsOpenAfterSubmit(),
                "The leave drawer closed despite the mandatory dates being empty, "
                        + "which suggests an incomplete request was accepted");
        note("Validation messages raised: " + drawer.validationMessages());

        drawer.close();
        assertEquals(leaves.totalRequestCount(), totalBefore,
                "The request count changed from " + totalBefore + " to "
                        + leaves.totalRequestCount() + " even though the submission was rejected");

        verified("An incomplete leave request is rejected and nothing is recorded");
    }

    @Test(priority = 4,
            groups = {"smoke", "leave"},
            description = "TC_LEAVE_04 - Leave entitlement cards report the signed-in user's "
                    + "own balance, not the balance of the employee being administered")
    public void TC_LEAVE_04_entitlementCardsReportSignedInUsersOwnBalance() {
        step("Signing in as the Organization Administrator and reading its balances");
        loginAndOpen(Persona.ADMIN, AppModule.LEAVES);

        LeavesPage adminLeaves = new LeavesPage();
        int adminSick = adminLeaves.remainingBalance(LeaveType.SICK);
        int adminSickTotal = adminLeaves.totalEntitlement(LeaveType.SICK);
        note("Administrator sick leave: " + adminSick + "/" + adminSickTotal);
        assertTrue(adminSick >= 0, "The Administrator's sick-leave balance did not resolve");

        step("Signing out and signing in as the HR Administrator");
        new DashboardPage().header().logout();
        loginAndOpen(Persona.HR, AppModule.LEAVES);

        LeavesPage hrLeaves = new LeavesPage();
        int hrSick = hrLeaves.remainingBalance(LeaveType.SICK);
        int hrSickTotal = hrLeaves.totalEntitlement(LeaveType.SICK);
        note("HR sick leave: " + hrSick + "/" + hrSickTotal);
        assertTrue(hrSick >= 0, "HR's sick-leave balance did not resolve");

        // HR has consumed sick leave in the seeded tenant and the Administrator
        // has not, so identical balances would mean the card shows tenant-wide
        // data rather than the caller's own.
        step("Verifying the balances are scoped per user rather than shared");
        softly().assertEquals(adminSickTotal, hrSickTotal,
                "The two personas report different sick-leave entitlements (" + adminSickTotal
                        + " vs " + hrSickTotal + "), but entitlement is a tenant-level policy");
        softly().assertTrue(adminSick != hrSick || adminSick == adminSickTotal,
                "Both personas report exactly " + adminSick + " sick days remaining. HR has "
                        + "consumed sick leave, so a shared figure suggests the card is not "
                        + "scoped to the signed-in user.");

        assertAllSoft();
        verified("Entitlement cards are scoped to the signed-in user");
    }
}
