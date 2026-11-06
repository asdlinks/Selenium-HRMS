package com.mywehr.enums;

/**
 * Leave categories configured for tenant myc001.
 *
 * The dropdown and the grid label the same category differently ("Casual
 * Leave" vs "Casual"), so both spellings live here and page objects pick the
 * one their locator needs.
 */
public enum LeaveType {

    CASUAL("Casual Leave", "Casual"),
    SICK("Sick Leave", "Sick"),
    PAID("Paid Leave", "Paid");

    private final String optionLabel;
    private final String gridLabel;

    LeaveType(String optionLabel, String gridLabel) {
        this.optionLabel = optionLabel;
        this.gridLabel = gridLabel;
    }

    /** Label shown in the Apply-for-Leave "Leave Type" dropdown. */
    public String optionLabel() {
        return optionLabel;
    }

    /** Shorter label used by the leave grid and the balance cards. */
    public String gridLabel() {
        return gridLabel;
    }
}
