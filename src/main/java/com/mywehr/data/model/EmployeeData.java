package com.mywehr.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Payload for the Register New Employee dialog.
 *
 * Field names mirror the form's own name attributes (name, email,
 * date_of_birth, employee_id ...) so the mapping from data file to page object
 * stays obvious.
 *
 * Built through {@link com.mywehr.data.TestDataFactory} rather than by hand,
 * so every run gets a unique email and employee id - the app rejects
 * duplicates, and a suite that can only pass once is not a suite.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmployeeData {

    private String scenario;
    private String fullName;
    private String email;
    private String dateOfBirth;
    private String branch;
    private String department;
    private String designation;
    private String employeeId;
    private String employmentType;
    private String probationMonths;
    private String joiningDate;
    private String role;
    private String manager;
    private String initialPassword;

    // Expectations, used by the negative dataset
    private String expectedError;
    private String erroneousField;

    public String getScenario() {
        return scenario;
    }

    public EmployeeData setScenario(String scenario) {
        this.scenario = scenario;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public EmployeeData setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public EmployeeData setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public EmployeeData setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }

    public String getBranch() {
        return branch;
    }

    public EmployeeData setBranch(String branch) {
        this.branch = branch;
        return this;
    }

    public String getDepartment() {
        return department;
    }

    public EmployeeData setDepartment(String department) {
        this.department = department;
        return this;
    }

    public String getDesignation() {
        return designation;
    }

    public EmployeeData setDesignation(String designation) {
        this.designation = designation;
        return this;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public EmployeeData setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
        return this;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public EmployeeData setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
        return this;
    }

    public String getProbationMonths() {
        return probationMonths;
    }

    public EmployeeData setProbationMonths(String probationMonths) {
        this.probationMonths = probationMonths;
        return this;
    }

    public String getJoiningDate() {
        return joiningDate;
    }

    public EmployeeData setJoiningDate(String joiningDate) {
        this.joiningDate = joiningDate;
        return this;
    }

    public String getRole() {
        return role;
    }

    public EmployeeData setRole(String role) {
        this.role = role;
        return this;
    }

    public String getManager() {
        return manager;
    }

    public EmployeeData setManager(String manager) {
        this.manager = manager;
        return this;
    }

    public String getInitialPassword() {
        return initialPassword;
    }

    public EmployeeData setInitialPassword(String initialPassword) {
        this.initialPassword = initialPassword;
        return this;
    }

    public String getExpectedError() {
        return expectedError;
    }

    public EmployeeData setExpectedError(String expectedError) {
        this.expectedError = expectedError;
        return this;
    }

    public String getErroneousField() {
        return erroneousField;
    }

    public EmployeeData setErroneousField(String erroneousField) {
        this.erroneousField = erroneousField;
        return this;
    }

    @Override
    public String toString() {
        return scenario != null ? scenario : (fullName + " / " + employeeId);
    }
}
