package com.mywehr.pages.timeleave;

import com.mywehr.data.model.LeaveRequestData;
import com.mywehr.enums.LeaveType;
import com.mywehr.utils.ElementUtils;
import com.mywehr.utils.Log;
import com.mywehr.utils.MuiUtils;
import com.mywehr.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * The right-hand Apply-for-Leave drawer.
 *
 * A MUI Drawer, not a Dialog, so it has no role="dialog" and MuiUtils dialog
 * helpers do not apply. Fields carry no name attributes either, so everything
 * here is located by its visible label.
 *
 * "Target Employee" only renders for personas allowed to apply on behalf of
 * someone else; an ordinary employee sees the drawer without it.
 */
public class ApplyLeaveDrawer {

    // Scoped to the drawer for the same reason the dialogs are: an unscoped
    // label match can resolve to a control on the page behind the overlay.
    private static final By TARGET_EMPLOYEE_INPUT =
            By.cssSelector(".MuiDrawer-paper input[placeholder*='Search employee']");
    private static final By LEAVE_TYPE_SELECT =
            MuiUtils.selectByLabelIn(MuiUtils.IN_DRAWER, "Leave Type");
    private static final By START_DATE =
            MuiUtils.inputByLabelIn(MuiUtils.IN_DRAWER, "Start Date");
    private static final By END_DATE =
            MuiUtils.inputByLabelIn(MuiUtils.IN_DRAWER, "End Date");
    private static final By REASON =
            By.cssSelector(".MuiDrawer-paper textarea[placeholder*='reason for leave']");

    private static final By FULL_DAY_TOGGLE = MuiUtils.buttonByText("Full Day");
    private static final By HALF_DAY_TOGGLE = MuiUtils.buttonByText("Half Day");
    private static final By RECORD_LEAVE_BUTTON = MuiUtils.buttonByText("Record Leave");

    // -------------------------------------------------------------- state

    public boolean isOpen() {
        return ElementUtils.isDisplayed(RECORD_LEAVE_BUTTON);
    }

    /** Present only for personas permitted to apply on another's behalf. */
    public boolean hasTargetEmployeeField() {
        return ElementUtils.isDisplayed(TARGET_EMPLOYEE_INPUT);
    }

    public boolean hasDurationToggles() {
        return ElementUtils.isDisplayed(FULL_DAY_TOGGLE)
                && ElementUtils.isDisplayed(HALF_DAY_TOGGLE);
    }

    public boolean allFieldsDisplayed() {
        return ElementUtils.isDisplayed(LEAVE_TYPE_SELECT)
                && ElementUtils.isDisplayed(START_DATE)
                && ElementUtils.isDisplayed(END_DATE)
                && hasDurationToggles();
    }

    // ------------------------------------------------------------- filling

    public ApplyLeaveDrawer selectTargetEmployee(String employeeName) {
        Log.step("Recording leave on behalf of " + employeeName);
        MuiUtils.searchAndSelect(TARGET_EMPLOYEE_INPUT, employeeName);
        return this;
    }

    /**
     * Picks the leave type.
     *
     * Matched on a prefix, not an exact string: the option carries the live
     * remaining balance - "Casual Leave (9 left)" - so the label changes as
     * leave is consumed. An exact match works exactly once and then breaks for
     * a reason that has nothing to do with the code.
     */
    public ApplyLeaveDrawer selectLeaveType(LeaveType leaveType) {
        MuiUtils.selectOptionContaining(LEAVE_TYPE_SELECT, leaveType.optionLabel());
        return this;
    }

    public ApplyLeaveDrawer chooseFullDay() {
        ElementUtils.click(FULL_DAY_TOGGLE);
        return this;
    }

    public ApplyLeaveDrawer chooseHalfDay() {
        ElementUtils.click(HALF_DAY_TOGGLE);
        return this;
    }

    public ApplyLeaveDrawer enterStartDate(String mmddyyyy) {
        ElementUtils.typeIntoDateField(START_DATE, mmddyyyy);
        return this;
    }

    public ApplyLeaveDrawer enterEndDate(String mmddyyyy) {
        ElementUtils.typeIntoDateField(END_DATE, mmddyyyy);
        return this;
    }

    public ApplyLeaveDrawer enterReason(String reason) {
        ElementUtils.type(REASON, reason);
        return this;
    }

    public ApplyLeaveDrawer fill(LeaveRequestData data) {
        Log.step("Filling the leave form: " + data);
        if (data.getTargetEmployee() != null && hasTargetEmployeeField()) {
            selectTargetEmployee(data.getTargetEmployee());
        }
        if (data.getLeaveType() != null) {
            selectLeaveType(data.getLeaveType());
        }
        if (data.isHalfDay()) {
            chooseHalfDay();
        } else {
            chooseFullDay();
        }
        if (data.getStartDate() != null && !data.getStartDate().isBlank()) {
            enterStartDate(data.getStartDate());
        }
        if (data.getEndDate() != null && !data.getEndDate().isBlank()) {
            enterEndDate(data.getEndDate());
        }
        if (data.getReason() != null) {
            enterReason(data.getReason());
        }
        return this;
    }

    // ---------------------------------------------------------- submitting

    /** Submits without asserting - used by the validation tests. */
    public ApplyLeaveDrawer clickRecordLeave() {
        Log.step("Submitting the leave request");
        ElementUtils.click(RECORD_LEAVE_BUTTON);
        return this;
    }

    /** Submits and waits for the drawer to close. */
    /**
     * Submits a valid request and waits for the drawer to close.
     *
     * The drawer closing is the only confirmation a save happened. A native
     * click can be absorbed without effect - the form stays open with no
     * validation error - so that case is retried once with the full pointer
     * sequence, the same way sign-in is.
     */
    public LeavesPage submitExpectingSuccess() {
        clickRecordLeave();
        if (!WaitUtils.waitForInvisibleQuietly(RECORD_LEAVE_BUTTON, Duration.ofSeconds(8))) {
            Log.warn("The leave drawer did not react to the submit - "
                    + "replaying the full pointer sequence");
            ElementUtils.clickViaScript(RECORD_LEAVE_BUTTON);
        }
        WaitUtils.waitForInvisible(RECORD_LEAVE_BUTTON);
        WaitUtils.waitForDataToSettle();
        Log.pass("Leave request recorded");
        return new LeavesPage();
    }

    /**
     * Closes the drawer without submitting, and confirms it actually closed.
     *
     * A drawer left open keeps its backdrop over the page, so the next
     * interaction fails somewhere unrelated to the test that caused it.
     */
    public LeavesPage close() {
        ElementUtils.pressEscape();
        if (WaitUtils.waitForInvisibleQuietly(RECORD_LEAVE_BUTTON, Duration.ofSeconds(5))) {
            return new LeavesPage();
        }

        Log.warn("The leave drawer ignored Escape - dismissing it through the backdrop");
        if (ElementUtils.isPresent(MuiUtils.BACKDROP)) {
            ElementUtils.clickViaScript(
                    ElementUtils.find(MuiUtils.BACKDROP, com.mywehr.enums.WaitStrategy.PRESENT));
        }
        WaitUtils.waitForInvisibleQuietly(RECORD_LEAVE_BUTTON, Duration.ofSeconds(5));
        return new LeavesPage();
    }

    // ---------------------------------------------------------- validation

    public List<String> validationMessages() {
        WaitUtils.sleepQuietly(Duration.ofMillis(600));
        return ElementUtils.findAll(MuiUtils.FIELD_ERROR).stream()
                .map(WebElement::getText)
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .distinct()
                .toList();
    }

    /**
     * The drawer staying open is the app rejecting the submission.
     * With no toast to read, this is the observable signal of a failed submit.
     */
    public boolean remainsOpenAfterSubmit() {
        WaitUtils.sleepQuietly(Duration.ofSeconds(2));
        return ElementUtils.isDisplayed(RECORD_LEAVE_BUTTON);
    }

    /** True when the native date input rejected the value and stayed empty. */
    public boolean isStartDateEmpty() {
        return ElementUtils.getValue(START_DATE).isEmpty();
    }
}
