package com.mywehr.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * A complete employee record for the six-step Employee Onboarding wizard.
 *
 * Unlike {@link EmployeeData}, which TestDataFactory fills with unique random
 * tokens, this is a fixed, realistic person loaded from
 * testdata/employee-onboarding.json. Only the email and Employee ID get a run
 * stamp ({@link #withRunStamp}), because the app never releases the email of
 * a deleted employee. The test deletes the record at the end, and removes any
 * same-named leftover from an aborted run before it starts.
 *
 * Dates are ISO (yyyy-MM-dd) in the JSON: unambiguous to read, and converted
 * to whatever the browser's date input expects at the point of entry.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OnboardingEmployee(
        BasicDetails basicDetails,
        ContactDetails contactDetails,
        IdentityDetails identityDetails,
        BankDetails bankDetails,
        List<Education> education,
        List<PreviousEmployment> employmentHistory,
        List<Reference> references) {

    public String fullName() {
        return basicDetails.fullName();
    }

    /**
     * This person with a run stamp on their unique identifiers only: email,
     * Employee ID, Aadhaar, Passport and PAN.
     *
     * The app permanently reserves these for a deleted employee - "This email
     * belongs to a deleted employee", "This Aadhaar number is already
     * assigned to another employee" (both HTTP 409) - so the cleanup at the
     * end of a run would otherwise block every later run. The identity
     * numbers keep their valid shapes (12-digit Aadhaar, letter + 7 digits
     * passport, AAAAA9999A PAN). Everything else - name, dates, address,
     * bank account, history - stays exactly as stored.
     *
     * @param stamp ten digits, e.g. "1104213400";
     *              "priya.nair@x.com" becomes "priya.nair.1104213400@x.com"
     */
    public OnboardingEmployee withRunStamp(String stamp) {
        if (!stamp.matches("\\d{10}")) {
            throw new IllegalArgumentException("The run stamp must be ten digits: " + stamp);
        }
        BasicDetails b = basicDetails;
        int at = b.email().indexOf('@');
        String email = b.email().substring(0, at) + "." + stamp + b.email().substring(at);
        BasicDetails stamped = new BasicDetails(b.fullName(), email, b.dateOfBirth(), b.gender(),
                b.bloodGroup(), b.maritalStatus(), b.nationality(), b.branch(), b.department(),
                b.designation(), b.employeeId() + "-" + stamp, b.employmentType(),
                b.probationMonths(), b.joiningDate(), b.reportingManager(),
                b.attendancePolicy(), b.shift());
        IdentityDetails id = identityDetails;
        IdentityDetails stampedId = new IdentityDetails(
                id.aadhaarNumber().substring(0, 2) + stamp,
                id.passportNumber().charAt(0) + stamp.substring(3));
        BankDetails bank = bankDetails;
        String pan = bank.panNumber();
        BankDetails stampedBank = new BankDetails(
                pan.substring(0, 5) + stamp.substring(6) + pan.charAt(9),
                bank.accountHolderName(), bank.bankName(), bank.accountNumber(),
                bank.branch(), bank.ifscCode());
        return new OnboardingEmployee(stamped, contactDetails, stampedId, stampedBank,
                education, employmentHistory, references);
    }

    /** Step 1 - Basic Details. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BasicDetails(
            String fullName,
            String email,
            String dateOfBirth,
            String gender,
            String bloodGroup,
            String maritalStatus,
            String nationality,
            String branch,
            String department,
            String designation,
            String employeeId,
            String employmentType,
            String probationMonths,
            String joiningDate,
            String reportingManager,
            String attendancePolicy,
            String shift) {
    }

    /** Step 2 - contact and address. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContactDetails(
            String contactNumber,
            String emergencyContactNumber,
            String addressLine,
            String city,
            String district,
            String state,
            String pinCode,
            String country) {
    }

    /** Step 2 - identity documents (numbers only; no uploads). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IdentityDetails(String aadhaarNumber, String passportNumber) {
    }

    /** Step 2 - banking. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BankDetails(
            String panNumber,
            String accountHolderName,
            String bankName,
            String accountNumber,
            String branch,
            String ifscCode) {
    }

    /** Step 3 - one qualification. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Education(
            String qualification,
            String institution,
            String specialization,
            String yearOfPassing,
            String grade) {
    }

    /** Step 4 - one previous employer. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PreviousEmployment(
            String companyName,
            String designation,
            String from,
            String to,
            boolean mostRecent) {
    }

    /** Step 5 - one reference. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Reference(
            String name,
            String relationship,
            String contactNumber,
            String email,
            String address) {
    }
}
