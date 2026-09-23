package com.mywehr.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mywehr.enums.LeaveType;

/** Payload for the Apply-for-Leave drawer. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeaveRequestData {

    private String scenario;
    private String targetEmployee;
    private LeaveType leaveType = LeaveType.CASUAL;
    private boolean halfDay;
    private String startDate;
    private String endDate;
    private String reason;
    private String expectedError;

    public String getScenario() {
        return scenario;
    }

    public LeaveRequestData setScenario(String scenario) {
        this.scenario = scenario;
        return this;
    }

    public String getTargetEmployee() {
        return targetEmployee;
    }

    public LeaveRequestData setTargetEmployee(String targetEmployee) {
        this.targetEmployee = targetEmployee;
        return this;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public LeaveRequestData setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
        return this;
    }

    public boolean isHalfDay() {
        return halfDay;
    }

    public LeaveRequestData setHalfDay(boolean halfDay) {
        this.halfDay = halfDay;
        return this;
    }

    public String getStartDate() {
        return startDate;
    }

    public LeaveRequestData setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public String getEndDate() {
        return endDate;
    }

    public LeaveRequestData setEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public LeaveRequestData setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public String getExpectedError() {
        return expectedError;
    }

    public LeaveRequestData setExpectedError(String expectedError) {
        this.expectedError = expectedError;
        return this;
    }

    @Override
    public String toString() {
        return scenario != null ? scenario
                : (leaveType + " " + startDate + " -> " + endDate);
    }
}
