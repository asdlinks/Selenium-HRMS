package com.mywehr.tests.leave;

import com.mywehr.base.BaseTest;
import com.mywehr.data.TestDataFactory;
import com.mywehr.data.model.LeaveRequestData;
import com.mywehr.driver.DriverManager;
import com.mywehr.enums.AppModule;
import com.mywehr.enums.LeaveStatus;
import com.mywehr.enums.Persona;
import com.mywehr.pages.dashboard.DashboardPage;
import com.mywehr.pages.timeleave.ApplyLeaveDrawer;
import com.mywehr.pages.timeleave.LeavesPage;
import com.mywehr.utils.DateUtils;
import com.mywehr.utils.ElementUtils;
import com.mywehr.utils.MuiUtils;
import com.mywehr.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.LocalDate;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * HEALABLE_ - leave flows that fail ONLY because of one stale locator.
 *
 * Every test here walks a real end-to-end leave flow that works against the
 * live app. Exactly one locator per test has been deliberately broken in a way
 * that a self-healing tool should be able to repair: the intended element is
 * on the page, unique, and recognisable from the broken locator's text,
 * attributes or position. Each broken locator is a BROKEN_* constant whose
 * comment gives the correct locator, so a heal can be checked against it.
 *
 * Expected result before healing: FAIL with NoSuchElementException on the
 * BROKEN_* locator. Expected result after healing: PASS, with every
 * validation in the test still green.
 *
 * See FLAKY_HEALING_DEMO.md at the project root for the full catalogue.
 * Run with:  mvn clean test -Phealable
 */
public class LeaveHealableTest extends BaseTest {

    private static final String TARGET_EMPLOYEE = "sales1";

    // ---------------------------------------------------- the broken locators

    /** Text drifted. Correct: //button[normalize-space(.)='Apply for Leave'] */
    private static final By BROKEN_APPLY_BUTTON =
            By.xpath("//button[normalize-space(.)='Apply Leave']");

    /** Attribute that never existed. Correct: //textarea[contains(@placeholder,'reason for leave')] */
    private static final By BROKEN_REASON_FIELD =
            By.xpath("//textarea[@name='leaveReason']");

    /** Class name typo. Correct: //span[contains(@class,'MuiChip-label')][starts-with(normalize-space(.),'Approved (')] */
    private static final By BROKEN_APPROVED_CHIP =
            By.xpath("//span[contains(@class,'MuiChip-lable')][starts-with(normalize-space(.),'Approved (')]");

    /** test-id the app never shipped. Correct: {row}//button[normalize-space(.)='Approve'] */
    private static final String BROKEN_APPROVE_BUTTON_IN_ROW = "//button[@data-testid='approve-leave']";

    /** Grid column field renamed. Correct: {row}//div[@data-field='status'] */
    private static final String BROKEN_STATUS_CELL_IN_ROW = "//div[@data-field='leave_status']";

    // ------------------------------------------------ the correct locators

    private static final By APPLY_BUTTON = By.xpath("//button[normalize-space(.)='Apply for Leave']");
    private static final By REASON_FIELD = By.xpath("//textarea[contains(@placeholder,'reason for leave')]");
    private static final By SUBMIT_FOR_APPROVAL =
            By.xpath("//button[@type='submit'][normalize-space(.)='Submit for Approval']");

    // ------------------------------------------------------------------ tests

    @Test(priority = 1, groups = {"demo", "healable"},
            description = "[HEALABLE] HEALABLE_TC_LEAVE_01 - Admin records leave for an employee and it is "
                    + "saved as Approved. Broken: 'Apply Leave' button text (real text 'Apply for Leave')")
    public void HEALABLE_TC_LEAVE_01_adminRecordsLeave_brokenButtonText_HEALABLE() {
        LeaveRequestData request = TestDataFactory.singleDayLeave(TARGET_EMPLOYEE);
        String[] day = gridFragments(request);

        step("Signing in as the Administrator and opening Leave Management");
        loginAndOpen(Persona.ADMIN, AppModule.LEAVES);
        LeavesPage leaves = new LeavesPage();
        int approvedBefore = leaves.tabCount(LeaveStatus.APPROVED);

        step("Opening the Apply for Leave drawer  <-- BROKEN LOCATOR");
        openDrawer(BROKEN_APPLY_BUTTON);

        step("Recording leave for " + TARGET_EMPLOYEE + " on " + request.getStartDate());
        new ApplyLeaveDrawer().fill(request).submitExpectingSuccess();

        step("Verifying the request was saved as Approved");
        assertTrue(leaves.waitForTabCount(LeaveStatus.APPROVED, approvedBefore + 1),
                "Approved count did not rise from " + approvedBefore);
        assertTrue(leaves.hasRequestWithStatus(TARGET_EMPLOYEE, LeaveStatus.APPROVED, day),
                "No Approved row for " + TARGET_EMPLOYEE + " on " + String.join(" ", day));
        verified("Admin-recorded leave is saved as Approved");
    }

    @Test(priority = 2, groups = {"demo", "healable"},
            description = "[HEALABLE] HEALABLE_TC_LEAVE_02 - HR submits its own leave for approval and it "
                    + "enters the Pending queue. Broken: reason textarea located by a non-existent name attribute")
    public void HEALABLE_TC_LEAVE_02_hrSubmitsForApproval_brokenAttribute_HEALABLE() {
        LeaveRequestData request = TestDataFactory.singleDayLeave(null).setReason(null);
        String[] day = gridFragments(request);

        step("Signing in as HR and opening Leave Management");
        loginAndOpen(Persona.HR, AppModule.LEAVES);
        LeavesPage leaves = new LeavesPage();
        int pendingBefore = leaves.tabCount(LeaveStatus.PENDING);

        step("Filling the leave form");
        openDrawer(APPLY_BUTTON);
        new ApplyLeaveDrawer().fill(request);

        step("Typing the reason  <-- BROKEN LOCATOR");
        DriverManager.get().findElement(BROKEN_REASON_FIELD).sendKeys("Healable demo " + TestDataFactory.uniqueToken());

        step("Submitting for approval");
        submitForApproval();

        step("Verifying the request is Pending");
        assertTrue(leaves.waitForTabCount(LeaveStatus.PENDING, pendingBefore + 1),
                "Pending count did not rise from " + pendingBefore);
        softly().assertTrue(hasRow(LeaveStatus.PENDING, day),
                "No Pending row on " + String.join(" ", day));
        assertAllSoft();
        verified("HR's own leave enters the Pending queue");
    }

    @Test(priority = 3, groups = {"demo", "healable"},
            description = "[HEALABLE] HEALABLE_TC_LEAVE_03 - The Approved tab count rises after the Admin "
                    + "records leave. Broken: chip located by a misspelt class 'MuiChip-lable'")
    public void HEALABLE_TC_LEAVE_03_approvedCountRises_brokenClassName_HEALABLE() {
        LeaveRequestData request = TestDataFactory.singleDayLeave(TARGET_EMPLOYEE);

        step("Signing in as the Administrator and opening Leave Management");
        loginAndOpen(Persona.ADMIN, AppModule.LEAVES);
        LeavesPage leaves = new LeavesPage();
        int before = leaves.tabCount(LeaveStatus.APPROVED);

        step("Recording leave for " + TARGET_EMPLOYEE);
        openDrawer(APPLY_BUTTON);
        new ApplyLeaveDrawer().fill(request).submitExpectingSuccess();
        leaves.waitForTabCount(LeaveStatus.APPROVED, before + 1);

        step("Reading the Approved chip  <-- BROKEN LOCATOR");
        String chip = DriverManager.get().findElement(BROKEN_APPROVED_CHIP).getText();
        int after = Integer.parseInt(chip.replaceAll("[^0-9]", ""));

        assertEquals(after, before + 1, "Approved chip reads '" + chip + "'");
        verified("The Approved chip count rose by one");
    }

    @Test(priority = 4, groups = {"demo", "healable"},
            description = "[HEALABLE] HEALABLE_TC_LEAVE_04 - HR submits leave and the Admin approves it from "
                    + "the Pending tab. Broken: Approve button located by a data-testid the app does not render")
    public void HEALABLE_TC_LEAVE_04_adminApprovesHrLeave_brokenTestId_HEALABLE() {
        LeaveRequestData request = TestDataFactory.singleDayLeave(null);
        String[] day = gridFragments(request);

        step("HR submits a leave request");
        hrSubmits(request);

        step("Signing in as the Administrator");
        loginAndOpen(Persona.ADMIN, AppModule.LEAVES);
        LeavesPage leaves = new LeavesPage();
        int pending = leaves.tabCount(LeaveStatus.PENDING);
        int approved = leaves.tabCount(LeaveStatus.APPROVED);
        leaves.selectStatusTab(LeaveStatus.PENDING);
        assertTrue(hasRow(LeaveStatus.PENDING, day), "HR's request is not in the Pending tab");

        step("Approving HR's request  <-- BROKEN LOCATOR");
        WebElement approve = DriverManager.get().findElement(
                By.xpath(rowXPath(LeaveStatus.PENDING, day) + BROKEN_APPROVE_BUTTON_IN_ROW));
        clickRowAction(approve, "Approve", day);

        step("Verifying the counts moved");
        assertTrue(leaves.waitForTabCount(LeaveStatus.PENDING, pending - 1), "Pending did not drop");
        softly().assertEquals(leaves.tabCount(LeaveStatus.APPROVED), approved + 1, "Approved did not rise");
        assertAllSoft();
        verified("Admin approved HR's leave");
    }

    @Test(priority = 5, groups = {"demo", "healable"},
            description = "[HEALABLE] HEALABLE_TC_LEAVE_05 - HR submits leave, the Admin rejects it and the row "
                    + "status reads Rejected. Broken: status cell located by renamed data-field 'leave_status'")
    public void HEALABLE_TC_LEAVE_05_adminRejectsHrLeave_brokenDataField_HEALABLE() {
        LeaveRequestData request = TestDataFactory.singleDayLeave(null);
        String[] day = gridFragments(request);

        step("HR submits a leave request");
        hrSubmits(request);

        step("Signing in as the Administrator and rejecting it");
        loginAndOpen(Persona.ADMIN, AppModule.LEAVES);
        LeavesPage leaves = new LeavesPage();
        int rejected = leaves.tabCount(LeaveStatus.REJECTED);
        leaves.selectStatusTab(LeaveStatus.PENDING);
        clickRowAction(DriverManager.get().findElement(
                By.xpath(rowXPath(LeaveStatus.PENDING, day) + "//button[normalize-space(.)='Reject']")), "Reject", day);
        assertTrue(leaves.waitForTabCount(LeaveStatus.REJECTED, rejected + 1), "Rejected did not rise");

        step("Reading the row's status cell  <-- BROKEN LOCATOR");
        leaves.selectStatusTab(LeaveStatus.REJECTED);
        WaitUtils.waitForVisible(By.xpath(rowXPath(LeaveStatus.REJECTED, day)));
        String status = DriverManager.get().findElement(
                By.xpath(rowXPath(LeaveStatus.REJECTED, day) + BROKEN_STATUS_CELL_IN_ROW)).getText();

        assertEquals(status.trim(), "Rejected", "Status cell");
        verified("Rejected leave shows Rejected in its status cell");
    }

    // ---------------------------------------------------------------- helpers

    /**
     * Opens the Apply for Leave drawer. The button often ignores the first
     * native click, so it is retried with a scripted pointer sequence.
     */
    private void openDrawer(By applyButton) {
        WebElement button = DriverManager.get().findElement(applyButton);
        ElementUtils.click(button);
        for (int attempt = 0; attempt < 3 && !WaitUtils.isVisibleWithin(REASON_FIELD, Duration.ofSeconds(3)); attempt++) {
            ElementUtils.clickViaScript(button);
        }
        assertTrue(ElementUtils.isDisplayed(REASON_FIELD), "The Apply for Leave drawer did not open");
        WaitUtils.sleepQuietly(Duration.ofMillis(700));
    }

    private void submitForApproval() {
        ElementUtils.click(SUBMIT_FOR_APPROVAL);
        if (!WaitUtils.waitForInvisibleQuietly(SUBMIT_FOR_APPROVAL, Duration.ofSeconds(8))) {
            ElementUtils.clickViaScript(SUBMIT_FOR_APPROVAL);
        }
        assertTrue(WaitUtils.waitForInvisibleQuietly(SUBMIT_FOR_APPROVAL, Duration.ofSeconds(10)),
                "The drawer stayed open after Submit for Approval");
        WaitUtils.waitForDataToSettle();
    }

    /** HR signs in, submits the request for approval, and signs out. */
    private void hrSubmits(LeaveRequestData request) {
        loginAndOpen(Persona.HR, AppModule.LEAVES);
        LeavesPage leaves = new LeavesPage();
        int pendingBefore = leaves.tabCount(LeaveStatus.PENDING);
        openDrawer(APPLY_BUTTON);
        new ApplyLeaveDrawer().fill(request);
        submitForApproval();
        assertTrue(leaves.waitForTabCount(LeaveStatus.PENDING, pendingBefore + 1),
                "HR's request was not queued as Pending");
        new DashboardPage().header().logout();
    }

    private static String[] gridFragments(LeaveRequestData request) {
        LocalDate day = DateUtils.fromDateInput(request.getStartDate());
        return new String[] {DateUtils.gridDayMonth(day), String.valueOf(day.getYear())};
    }

    private static String rowXPath(LeaveStatus status, String... fragments) {
        StringBuilder xpath = new StringBuilder("//div[contains(@class,'MuiDataGrid-row')]")
                .append("[.//span[contains(@class,'MuiChip-label')][normalize-space(.)=")
                .append(MuiUtils.escapeForXPath(status.label())).append("]]");
        for (String fragment : fragments) {
            xpath.append("[contains(normalize-space(.),").append(MuiUtils.escapeForXPath(fragment)).append(")]");
        }
        return xpath.toString();
    }

    private static boolean hasRow(LeaveStatus status, String... fragments) {
        return WaitUtils.isVisibleWithin(By.xpath(rowXPath(status, fragments)), Duration.ofSeconds(12));
    }

    /**
     * Clicks Approve / Reject on a Pending row. Like the Apply button, these
     * often ignore a single native click, so the click is replayed until the
     * row leaves the Pending state.
     */
    private static void clickRowAction(WebElement button, String action, String... day) {
        By pendingRow = By.xpath(rowXPath(LeaveStatus.PENDING, day));
        ElementUtils.click(button);
        for (int attempt = 0; attempt < 3; attempt++) {
            confirmIfAsked(action);
            if (WaitUtils.waitForInvisibleQuietly(pendingRow, Duration.ofSeconds(5))) {
                return;
            }
            try {
                ElementUtils.clickViaScript(button);
            } catch (org.openqa.selenium.StaleElementReferenceException e) {
                return; // the row re-rendered, so the action went through
            }
        }
    }

    private static void confirmIfAsked(String action) {
        if (WaitUtils.isVisibleWithin(MuiUtils.DIALOG, Duration.ofSeconds(3))) {
            assertTrue(MuiUtils.clickDialogButtonAndWaitForClose(action),
                    "The '" + action + "' confirmation dialog did not close");
        }
        WaitUtils.waitForDataToSettle();
    }
}
