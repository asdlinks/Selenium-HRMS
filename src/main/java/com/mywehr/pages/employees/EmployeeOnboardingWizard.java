package com.mywehr.pages.employees;

import com.mywehr.data.model.OnboardingEmployee;
import com.mywehr.enums.WaitStrategy;
import com.mywehr.exceptions.FrameworkException;
import com.mywehr.pages.base.BaseDialog;
import com.mywehr.utils.DateUtils;
import com.mywehr.utils.ElementUtils;
import com.mywehr.utils.Log;
import com.mywehr.utils.MuiUtils;
import com.mywehr.utils.NetworkRecorder;
import com.mywehr.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The six-step "Employee Onboarding" wizard behind Add New Employee.
 *
 *   1 Basic Details          personal + employment, the only required step
 *   2 Contact, ID & Bank     contact, address, Aadhaar/Passport, PAN + bank
 *   3 Education              repeatable entries, each added with a button
 *   4 Employment History     repeatable entries
 *   5 References             repeatable entries; its forward button reads "Review"
 *   6 Review & Create        read-only summary and the final create button
 *
 * Only one step is rendered at a time, so label-based locators scoped to the
 * dialog resolve to the current step's field even where labels repeat across
 * steps ("Branch" on steps 1 and 2, "Contact Number" on steps 2 and 5).
 *
 * WHY THE NAVIGATION BUTTONS ARE CLICKED BY SCRIPT
 * ------------------------------------------------
 * A native WebDriver click on Next/Cancel is delivered to the button
 * (elementFromPoint confirms nothing covers it) yet the wizard does not react,
 * and nothing in the DOM says why. A dispatched click is acted on every time.
 * Each navigation is followed by a check that the step really changed, so a
 * swallowed click still fails loudly rather than silently.
 */
public class EmployeeOnboardingWizard extends BaseDialog {

    private static final Pattern STEP_PATTERN = Pattern.compile("STEP (\\d) OF (\\d)");

    private static final String IN = MuiUtils.IN_DIALOG;

    // --- step 1: inputs carrying name attributes ---
    private static final By FULL_NAME = By.cssSelector("input[name='name']");
    private static final By EMAIL = By.cssSelector("input[name='email']");
    private static final By DATE_OF_BIRTH = By.cssSelector("input[name='date_of_birth']");
    private static final By NATIONALITY = By.cssSelector("input[name='nationality']");
    private static final By EMPLOYEE_ID = By.cssSelector("input[name='employee_id']");
    private static final By PROBATION_MONTHS = By.cssSelector("input[name='probation_period']");
    private static final By JOINING_DATE = By.cssSelector("input[name='joining_date']");

    // --- step 2: contact block carries name attributes, ID and bank blocks do not ---
    private static final By CONTACT_NUMBER = By.cssSelector("input[name='contact_number']");
    private static final By EMERGENCY_CONTACT = By.cssSelector("input[name='emergency_contact_number']");
    private static final By ADDRESS = By.cssSelector("textarea[name='address']");
    private static final By CITY = By.cssSelector("input[name='address_city']");
    private static final By PIN_CODE = By.cssSelector("input[name='address_pin_code']");
    private static final By COUNTRY = By.cssSelector("input[name='address_country']");

    // ------------------------------------------------------------- state

    @Override
    protected String title() {
        return "Employee Onboarding";
    }

    /** The 1-based step the wizard is showing, read from its "STEP n OF 6" caption. */
    public int currentStep() {
        Matcher matcher = STEP_PATTERN.matcher(MuiUtils.dialogText());
        if (!matcher.find()) {
            throw new FrameworkException("The onboarding wizard shows no 'STEP n OF 6' caption");
        }
        return Integer.parseInt(matcher.group(1));
    }

    // ------------------------------------------------- step 1: basic details

    public EmployeeOnboardingWizard fillBasicDetails(OnboardingEmployee.BasicDetails basic) {
        Log.step("Step 1 - Basic Details for " + basic.fullName());
        ElementUtils.type(FULL_NAME, basic.fullName());
        ElementUtils.type(EMAIL, basic.email());
        ElementUtils.typeIntoDateField(DATE_OF_BIRTH, browserDate(basic.dateOfBirth()));
        select("Gender", basic.gender());
        select("Blood Group", basic.bloodGroup());
        select("Marital Status", basic.maritalStatus());
        ElementUtils.type(NATIONALITY, basic.nationality());

        select("Branch", basic.branch());
        select("Department", basic.department());
        select("Designation", basic.designation());
        // The field is pre-filled with an auto-generated id; typing replaces it.
        ElementUtils.type(EMPLOYEE_ID, basic.employeeId());
        select("Employment Type", basic.employmentType());
        ElementUtils.type(PROBATION_MONTHS, basic.probationMonths());
        ElementUtils.typeIntoDateField(JOINING_DATE, browserDate(basic.joiningDate()));

        // Options carry a suffix ("Branch Head (Brand head)",
        // "General Shift · General (...)"), so these match on the stable part.
        selectContaining("Reporting Manager", basic.reportingManager());
        selectContaining("Attendance Policy", basic.attendancePolicy());
        selectContaining("Shift", basic.shift());
        return this;
    }

    // ------------------------------------------- step 2: contact, ID & bank

    public EmployeeOnboardingWizard fillContactDetails(OnboardingEmployee.ContactDetails contact) {
        Log.step("Step 2 - Contact and address");
        ElementUtils.type(CONTACT_NUMBER, contact.contactNumber());
        ElementUtils.type(EMERGENCY_CONTACT, contact.emergencyContactNumber());
        ElementUtils.type(ADDRESS, contact.addressLine());
        ElementUtils.type(CITY, contact.city());
        // State before district: the district list is the chosen state's.
        select("State", contact.state());
        MuiUtils.searchAndSelect(MuiUtils.inputByLabelIn(IN, "District"), contact.district());
        if (contact.pinCode() != null && !contact.pinCode().isBlank()) {
            ElementUtils.type(PIN_CODE, contact.pinCode());
        }
        ElementUtils.type(COUNTRY, contact.country());
        return this;
    }

    public EmployeeOnboardingWizard fillIdentityDetails(OnboardingEmployee.IdentityDetails identity) {
        Log.step("Step 2 - Identity numbers");
        typeByLabel("Aadhaar Number", identity.aadhaarNumber());
        typeByLabel("Passport Number", identity.passportNumber());
        return this;
    }

    public EmployeeOnboardingWizard fillBankDetails(OnboardingEmployee.BankDetails bank) {
        Log.step("Step 2 - Banking");
        typeByLabel("PAN Number", bank.panNumber());
        typeByLabel("Account Holder Name", bank.accountHolderName());
        typeByLabel("Bank Name", bank.bankName());
        typeByLabel("Account Number", bank.accountNumber());
        typeByLabel("Confirm Account Number", bank.accountNumber());
        typeByLabel("Branch", bank.branch());
        typeByLabel("IFSC Code", bank.ifscCode());
        return this;
    }

    // ------------------------------------------ steps 3-5: repeatable lists

    public EmployeeOnboardingWizard addEducation(OnboardingEmployee.Education education) {
        Log.step("Step 3 - Adding education: " + education.qualification());
        typeByLabel("Qualification", education.qualification());
        typeByLabel("Institution", education.institution());
        typeByLabel("Specialization", education.specialization());
        typeByLabel("Year of Passing", education.yearOfPassing());
        typeByLabel("Grade", education.grade());
        addEntry("Add Education Entry", education.institution());
        return this;
    }

    public EmployeeOnboardingWizard addPreviousEmployment(OnboardingEmployee.PreviousEmployment job) {
        Log.step("Step 4 - Adding previous employer: " + job.companyName());
        typeByLabel("Company Name", job.companyName());
        typeByLabel("Designation", job.designation());
        typeDateByLabel("From", job.from());
        typeDateByLabel("To", job.to());
        if (job.mostRecent()) {
            By checkbox = By.xpath(IN + "//label[contains(normalize-space(.),'most recent employer')]"
                    + "//input[@type='checkbox']");
            if (!ElementUtils.find(checkbox, WaitStrategy.PRESENT).isSelected()) {
                ElementUtils.clickViaScript(checkbox);
            }
        }
        addEntry("Add Company", job.companyName());
        return this;
    }

    public EmployeeOnboardingWizard addReference(OnboardingEmployee.Reference reference) {
        Log.step("Step 5 - Adding reference: " + reference.name());
        typeByLabel("Reference Name", reference.name());
        typeByLabel("Relationship", reference.relationship());
        typeByLabel("Contact Number", reference.contactNumber());
        typeByLabel("Email", reference.email());
        ElementUtils.type(MuiUtils.textAreaByLabelIn(IN, "Address"), reference.address());
        addEntry("Add Reference", reference.name());
        return this;
    }

    // ----------------------------------------------------------- navigation

    /** Moves forward one step (Next, or Review on step 5) and checks it happened. */
    public EmployeeOnboardingWizard next() {
        int from = currentStep();
        if (!tryNext()) {
            throw new FrameworkException("The wizard stayed on step " + from
                    + ". Validation messages: " + validationMessages()
                    + ". Field values: " + fieldValues());
        }
        return this;
    }

    /**
     * Attempts to move forward one step.
     *
     * @return false when the wizard stayed put - read {@link #validationMessages()}
     */
    public boolean tryNext() {
        int from = currentStep();
        String button = from == 5 ? "Review" : "Next";
        Log.step("Wizard: '" + button + "' from step " + from);
        MuiUtils.clickDialogButtonViaScript(button);
        return WaitUtils.fluentlyQuietly(d -> currentStep() == from + 1, Duration.ofSeconds(10));
    }

    /** Empties the PIN Code field (step 2). */
    public EmployeeOnboardingWizard clearPinCode() {
        ElementUtils.setValueViaReact(PIN_CODE, "");
        return this;
    }

    /**
     * Clicks Create Employee on Review & Create and waits for the wizard to
     * leave that step - the proof the record was accepted.
     *
     * @return the success screen's text with the password redacted, or empty
     *         if the wizard closed straight away
     */
    public String create() {
        if (currentStep() != 6) {
            throw new FrameworkException("create() called on step " + currentStep() + ", not 6");
        }
        Log.step("Wizard: creating the employee");
        WebElement dialog = ElementUtils.find(MuiUtils.DIALOG, WaitStrategy.VISIBLE);
        WebElement createButton = dialog.findElements(By.xpath(".//button")).stream()
                .filter(b -> b.getText().trim().toLowerCase().contains("create"))
                .reduce((first, second) -> second) // the action bar comes last
                .orElseThrow(() -> new FrameworkException(
                        "No create button on the Review step. Buttons: " + buttonTexts(dialog)));
        NetworkRecorder.start();
        ElementUtils.clickViaScript(createButton);

        try {
            WaitUtils.fluently(d -> !MuiUtils.isDialogOpen() || !onReviewStep(),
                    Duration.ofSeconds(30), "The onboarding wizard did not move on after create");
        } catch (org.openqa.selenium.TimeoutException e) {
            // The app can reject a create without saying so on screen; the
            // API response is then the only place the reason exists.
            throw new IllegalStateException("The wizard is still on Review & Create 30s after "
                    + "create. API writes: " + redact(NetworkRecorder.recordedWrites())
                    + ". Validation messages: " + validationMessages()
                    + ". Alerts: " + alertTexts(), e);
        }
        Log.info("API writes during create: " + redact(NetworkRecorder.recordedWrites()));

        // A successful create shows an "Employee Created Successfully" screen
        // with the generated login details inside the same dialog; it also
        // names any section that failed to save. Done closes it.
        String outcome = "";
        if (MuiUtils.isDialogOpen()) {
            outcome = redact(MuiUtils.dialogText().replace("\n", " / "));
            Log.info("After create the wizard shows: " + outcome);
            WebElement after = ElementUtils.find(MuiUtils.DIALOG, WaitStrategy.VISIBLE);
            after.findElements(By.xpath(".//button")).stream()
                    .filter(b -> b.getText().trim().matches("(?i)done|close|finish|ok"))
                    .reduce((first, second) -> second)
                    .ifPresent(ElementUtils::clickViaScript);
            WaitUtils.waitForInvisibleQuietly(MuiUtils.DIALOG, Duration.ofSeconds(10));
            if (MuiUtils.isDialogOpen()) {
                MuiUtils.dismissOverlays();
            }
        }
        WaitUtils.waitForDataToSettle();
        Log.pass("Employee created through the onboarding wizard");
        return outcome;
    }

    /**
     * Removes the generated initial password from text bound for logs and
     * reports - from the success screen ("Password / xxxx") and from the API
     * response ("initial_password":"xxxx").
     */
    private static String redact(String text) {
        return text
                .replaceAll("(Password / )[^ /]+", "$1<redacted>")
                .replaceAll("(\"initial_password\"\\s*:\\s*\")[^\"]*", "$1<redacted>");
    }

    // -------------------------------------------------------------- helpers

    private static void select(String label, String option) {
        if (option != null && !option.isBlank()) {
            MuiUtils.selectOption(MuiUtils.selectByLabelIn(IN, label), option);
        }
    }

    private static void selectContaining(String label, String fragment) {
        if (fragment != null && !fragment.isBlank()) {
            MuiUtils.selectOptionContaining(MuiUtils.selectByLabelIn(IN, label), fragment);
        }
    }

    private static void typeByLabel(String label, String value) {
        if (value != null && !value.isBlank()) {
            ElementUtils.type(MuiUtils.inputByLabelIn(IN, label), value);
        }
    }

    /** From/To may render as date or month inputs; each takes a different value shape. */
    private static void typeDateByLabel(String label, String isoDate) {
        if (isoDate == null || isoDate.isBlank()) {
            return;
        }
        By locator = MuiUtils.inputByLabelIn(IN, label);
        String type = ElementUtils.getAttribute(locator, "type");
        if ("date".equals(type)) {
            ElementUtils.typeIntoDateField(locator, browserDate(isoDate));
        } else if ("month".equals(type)) {
            ElementUtils.setValueViaReact(locator, isoDate.substring(0, 7));
        } else {
            ElementUtils.type(locator, isoDate);
        }
    }

    /** Clicks a list's Add button and waits for the new entry to be listed. */
    private static void addEntry(String buttonText, String entryMarker) {
        MuiUtils.clickDialogButtonViaScript(buttonText);
        // Dialog TEXT excludes input values, so the marker only shows up once
        // the entry is rendered in the list - not while it sits in the form.
        if (!WaitUtils.fluentlyQuietly(d -> MuiUtils.dialogText().contains(entryMarker),
                Duration.ofSeconds(8))) {
            throw new FrameworkException("'" + buttonText + "' did not add an entry showing '"
                    + entryMarker + "'");
        }
        WaitUtils.sleepQuietly(Duration.ofMillis(400));
    }

    private static String browserDate(String iso) {
        return DateUtils.forDateInput(LocalDate.parse(iso));
    }

    private boolean onReviewStep() {
        try {
            return MuiUtils.dialogText().contains("STEP 6 OF 6");
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** "label=value" for every field on the current step - for failure messages. */
    private Object fieldValues() {
        return js().executeScript("""
                        const dialog = document.querySelector('.MuiDialog-root');
                        if (!dialog) return 'no dialog';
                        return [...dialog.querySelectorAll('input, textarea')]
                            .filter(e => e.type !== 'file' && e.getAttribute('aria-hidden') !== 'true')
                            .map(e => {
                                const control = e.closest('.MuiFormControl-root');
                                const label = control && control.querySelector('label');
                                return (label ? label.textContent.trim() : (e.name || '?'))
                                    + '=' + JSON.stringify(e.value);
                            }).join(', ');
                        """);
    }

    private static List<String> buttonTexts(WebElement dialog) {
        return dialog.findElements(By.xpath(".//button")).stream()
                .map(b -> b.getText().trim()).filter(t -> !t.isEmpty()).toList();
    }

    private static List<String> alertTexts() {
        return ElementUtils.findAll(MuiUtils.ALERT).stream()
                .map(WebElement::getText).map(String::trim)
                .filter(t -> !t.isEmpty()).toList();
    }
}
