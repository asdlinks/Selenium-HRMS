package com.mywehr.tests.leave;

import com.mywehr.base.BaseTest;
import com.mywehr.data.TestDataFactory;
import com.mywehr.dataproviders.TestDataProviders;
import com.mywehr.enums.LeaveStatus;
import com.mywehr.enums.LeaveType;
import com.mywehr.enums.Persona;
import com.mywehr.pages.timeleave.ApplyLeaveDrawer;
import com.mywehr.pages.timeleave.LeavesPage;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * TC_LEAVE - Leave Management.
 *
 * Neither test leaves anything behind: TC_LEAVE_01 only reads, and
 * TC_LEAVE_02 submits a request the app must reject.
 */
public class LeaveWorkflowTest extends BaseTest {

    /** Seeded employee named as the leave subject in the rejected request. */
    private static final String TARGET_EMPLOYEE = "hr";

    @Test(priority = 1,
            groups = {"smoke", "leave", "critical"},
            dataProvider = "allPersonas",
            dataProviderClass = TestDataProviders.class,
            description = "TC_LEAVE_01 - Leave Management renders valid entitlement cards and "
                    + "the request grid, and its status tab counts reconcile with the grid")
    public void TC_LEAVE_01_leaveManagementRendersAndStatusCountsReconcile(Persona persona) {
        step("Signing in as " + persona.roleName() + " and opening Leave Management");
        LeavesPage leaves = loginAndOpen(persona, new LeavesPage());
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
            groups = {"smoke", "leave", "negative", "validation"},
            description = "TC_LEAVE_02 - Submitting a leave request without its mandatory "
                    + "dates is rejected and nothing is recorded")
    public void TC_LEAVE_02_leaveSubmissionWithoutMandatoryDatesIsRejected() {
        step("Signing in as the Organization Administrator and opening Leave Management");
        LeavesPage leaves = loginAndOpen(Persona.ADMIN, new LeavesPage());
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
}
