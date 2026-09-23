package com.mywehr.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One login row from credentials.json.
 *
 * Also used for the negative-login dataset, where {@code expectedError} carries
 * the message the app must show and {@code scenario} names the case in the
 * report.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Credential {

    private String scenario;
    private String companyCode;
    private String email;
    private String password;
    private String expectedError;
    private boolean shouldSucceed;

    public Credential() {
    }

    public Credential(String companyCode, String email, String password) {
        this.companyCode = companyCode;
        this.email = email;
        this.password = password;
        this.shouldSucceed = true;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getExpectedError() {
        return expectedError;
    }

    public void setExpectedError(String expectedError) {
        this.expectedError = expectedError;
    }

    public boolean isShouldSucceed() {
        return shouldSucceed;
    }

    public void setShouldSucceed(boolean shouldSucceed) {
        this.shouldSucceed = shouldSucceed;
    }

    /** Never print the password - reports and CI logs are widely readable. */
    @Override
    public String toString() {
        return scenario != null ? scenario : (email + " @ " + companyCode);
    }
}
