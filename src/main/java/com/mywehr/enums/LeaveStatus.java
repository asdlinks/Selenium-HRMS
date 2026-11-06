package com.mywehr.enums;

/** Leave request states as rendered in the Leave Management grid and tab strip. */
public enum LeaveStatus {

    ALL("All"),
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled");

    private final String label;

    LeaveStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
