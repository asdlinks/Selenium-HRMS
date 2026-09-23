package com.mywehr.pages.employees;

import com.mywehr.data.model.EmployeeData;
import com.mywehr.utils.ElementUtils;
import com.mywehr.utils.Log;
import com.mywehr.utils.MuiUtils;
import com.mywehr.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * The Register New Employee dialog.
 *
 * Five sections: Personal Information, Employment Information, Identity
 * Information (optional) and Banking Information (optional).
 *
 * The text inputs carry real name attributes, so they are located that way.
 * The four dropdowns (Branch, Department, Designation, Employment Type, Role,
 * Assign Manager) are MUI Selects whose options are portalled to body level,
 * so those go through MuiUtils.
 */
public class RegisterEmployeeDialog {

    private static final String TITLE = "Register New Employee";

    // --- inputs, by the form's own name attributes ---
    private static final By FULL_NAME = By.cssSelector("input[name='name']");
    private static final By EMAIL = By.cssSelector("input[name='email']");
    private static final By DATE_OF_BIRTH = By.cssSelector("input[name='date_of_birth']");
    private static final By EMPLOYEE_ID = By.cssSelector("input[name='employee_id']");
    private static final By PROBATION_MONTHS = By.cssSelector("input[name='probation_period']");
    private static final By JOINING_DATE = By.cssSelector("input[name='joining_date']");
    private static final By INITIAL_PASSWORD = By.cssSelector("input[name='password']");

    // --- dropdowns, by visible label ---
    // Scoped to the dialog: the Employee Directory underneath renders filters
    // with these exact same labels, and an unscoped locator resolves to those
    // instead. See MuiUtils.inputByLabelIn for what that failure looks like.
    private static final By BRANCH = MuiUtils.selectByLabelIn(MuiUtils.IN_DIALOG, "Branch");
    private static final By DEPARTMENT = MuiUtils.selectByLabelIn(MuiUtils.IN_DIALOG, "Department");
    private static final By DESIGNATION = MuiUtils.selectByLabelIn(MuiUtils.IN_DIALOG, "Designation");
    private static final By EMPLOYMENT_TYPE = MuiUtils.selectByLabelIn(MuiUtils.IN_DIALOG, "Employment Type");
    private static final By ROLE = MuiUtils.selectByLabelIn(MuiUtils.IN_DIALOG, "Role");
    private static final By ASSIGN_MANAGER = MuiUtils.selectByLabelIn(MuiUtils.IN_DIALOG, "Assign Manager");

    private static final String SAVE_BUTTON = "Save Record";
    private static final String CANCEL_BUTTON = "Cancel";

    // -------------------------------------------------------------- state

    public boolean isOpen() {
        return MuiUtils.isDialogOpen() && MuiUtils.dialogText().contains(TITLE);
    }

    public String title() {
        return ElementUtils.getText(MuiUtils.DIALOG_TITLE);
    }

    /** Section headings, used to assert the dialog rendered in full. */
    public boolean hasAllSections() {
        String text = MuiUtils.dialogText();
        return text.contains("Personal Information")
                && text.contains("Employment Information")
                && text.contains("Identity Information")
                && text.contains("Banking Information");
    }

    // -------------------------------------------------------------- filling

    public RegisterEmployeeDialog enterFullName(String value) {
        ElementUtils.type(FULL_NAME, value);
        return this;
    }

    public RegisterEmployeeDialog enterEmail(String value) {
        ElementUtils.type(EMAIL, value);
        return this;
    }

    public RegisterEmployeeDialog enterDateOfBirth(String mmddyyyy) {
        ElementUtils.typeIntoDateField(DATE_OF_BIRTH, mmddyyyy);
        return this;
    }

    public RegisterEmployeeDialog enterEmployeeId(String value) {
        ElementUtils.type(EMPLOYEE_ID, value);
        return this;
    }

    public RegisterEmployeeDialog enterProbationMonths(String value) {
        ElementUtils.type(PROBATION_MONTHS, value);
        return this;
    }

    public RegisterEmployeeDialog enterJoiningDate(String mmddyyyy) {
        ElementUtils.typeIntoDateField(JOINING_DATE, mmddyyyy);
        return this;
    }

    public RegisterEmployeeDialog enterInitialPassword(String value) {
        ElementUtils.type(INITIAL_PASSWORD, value);
        return this;
    }

    public RegisterEmployeeDialog selectBranch(String value) {
        MuiUtils.selectOption(BRANCH, value);
        return this;
    }

    public RegisterEmployeeDialog selectDepartment(String value) {
        MuiUtils.selectOption(DEPARTMENT, value);
        return this;
    }

    public RegisterEmployeeDialog selectDesignation(String value) {
        MuiUtils.selectOption(DESIGNATION, value);
        return this;
    }

    public RegisterEmployeeDialog selectEmploymentType(String value) {
        MuiUtils.selectOption(EMPLOYMENT_TYPE, value);
        return this;
    }

    public RegisterEmployeeDialog selectRole(String value) {
        MuiUtils.selectOption(ROLE, value);
        return this;
    }

    public RegisterEmployeeDialog assignManager(String value) {
        MuiUtils.selectOption(ASSIGN_MANAGER, value);
        return this;
    }

    /**
     * Fills whatever the dataset provides and leaves the rest untouched.
     *
     * Null-tolerant on purpose: the negative dataset deliberately omits fields
     * to exercise validation, and a blank string is itself a meaningful input
     * ("clear this field") rather than "skip it".
     */
    public RegisterEmployeeDialog fill(EmployeeData data) {
        Log.step("Filling the employee form: " + data);
        if (data.getFullName() != null) {
            enterFullName(data.getFullName());
        }
        if (data.getEmail() != null) {
            enterEmail(data.getEmail());
        }
        if (data.getDateOfBirth() != null && !data.getDateOfBirth().isBlank()) {
            enterDateOfBirth(data.getDateOfBirth());
        }
        if (data.getBranch() != null) {
            selectBranch(data.getBranch());
        }
        if (data.getDepartment() != null) {
            selectDepartment(data.getDepartment());
        }
        if (data.getDesignation() != null) {
            selectDesignation(data.getDesignation());
        }
        if (data.getEmployeeId() != null) {
            enterEmployeeId(data.getEmployeeId());
        }
        if (data.getEmploymentType() != null) {
            selectEmploymentType(data.getEmploymentType());
        }
        if (data.getProbationMonths() != null) {
            enterProbationMonths(data.getProbationMonths());
        }
        if (data.getJoiningDate() != null && !data.getJoiningDate().isBlank()) {
            enterJoiningDate(data.getJoiningDate());
        }
        if (data.getRole() != null) {
            selectRole(data.getRole());
        }
        if (data.getManager() != null) {
            assignManager(data.getManager());
        }
        if (data.getInitialPassword() != null) {
            enterInitialPassword(data.getInitialPassword());
        }
        return this;
    }

    // ------------------------------------------------------------ submitting

    /**
     * Clicks Save without asserting the outcome - used by validation tests.
     *
     * A submit has two legitimate outcomes here: the dialog closes (accepted)
     * or validation messages appear (rejected). Either proves the click
     * registered. Seeing NEITHER means the click was swallowed, which would
     * otherwise be misreported as "the form raised no validation errors" - so
     * that case is retried with the full pointer sequence.
     */
    public RegisterEmployeeDialog clickSave() {
        Log.step("Submitting the employee form");
        MuiUtils.clickDialogButton(SAVE_BUTTON);

        if (!submissionWasProcessed()) {
            Log.warn("The form did not react to the submit - "
                    + "replaying the full pointer sequence");
            if (MuiUtils.isDialogOpen()) {
                MuiUtils.clickDialogButtonViaScript(SAVE_BUTTON);
                submissionWasProcessed();
            }
        }
        return this;
    }

    /** Waits briefly for the dialog to either close or raise validation errors. */
    private boolean submissionWasProcessed() {
        try {
            WaitUtils.fluently(d -> !MuiUtils.isDialogOpen()
                            || !ElementUtils.findAll(MuiUtils.FIELD_ERROR).isEmpty(),
                    Duration.ofSeconds(8),
                    "The form neither closed nor raised a validation message");
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }

    /** Saves and waits for the dialog to close, i.e. the happy path. */
    public EmployeeDirectoryPage saveExpectingSuccess() {
        if (!MuiUtils.clickDialogButtonAndWaitForClose(SAVE_BUTTON)) {
            throw new IllegalStateException(
                    "The employee was not saved - the dialog is still open. "
                            + "Validation messages: " + validationMessages());
        }
        WaitUtils.waitForDataToSettle();
        Log.pass("Employee record saved");
        return new EmployeeDirectoryPage();
    }

    public EmployeeDirectoryPage cancel() {
        MuiUtils.clickDialogButtonAndWaitForClose(CANCEL_BUTTON);
        return new EmployeeDirectoryPage();
    }

    // ------------------------------------------------------------ validation

    /** Every inline validation message currently rendered in the dialog. */
    public List<String> validationMessages() {
        WaitUtils.sleepQuietly(Duration.ofMillis(600));
        return ElementUtils.findAll(MuiUtils.FIELD_ERROR).stream()
                .map(WebElement::getText)
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .distinct()
                .toList();
    }

    public boolean showsValidationMessage(String message) {
        return validationMessages().stream().anyMatch(text -> text.contains(message));
    }

    /** The dialog staying open is itself proof the submission was rejected. */
    public boolean remainsOpenAfterSubmit() {
        WaitUtils.sleepQuietly(Duration.ofMillis(800));
        return MuiUtils.isDialogOpen();
    }
}
