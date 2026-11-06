package com.mywehr.data;

import com.mywehr.data.model.LeaveRequestData;
import com.mywehr.enums.LeaveType;
import com.mywehr.utils.DateUtils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds unique, valid test data at runtime.
 *
 * WHY NOT STATIC FIXTURES
 * -----------------------
 * The HRMS rejects duplicate names (departments, for one). A committed
 * fixture with a fixed email passes once, then fails on every subsequent run
 * against the same tenant - the classic "works on my machine, red in CI
 * tomorrow" failure. Every generated record therefore carries a timestamp and
 * a per-JVM counter, which also makes it trivial to spot and clean up
 * automation-created rows in the test tenant.
 *
 * Realistic people (the onboarding tests) and reference data the tenant
 * already owns stay in JSON under testdata/, because those are inputs the
 * tests read rather than invent.
 */
public final class TestDataFactory {

    /** Prefix on every automation-created record, for easy identification. */
    public static final String AUTOMATION_PREFIX = "AT";

    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    private TestDataFactory() {
    }

    /** Unique token, e.g. "AT_84213077_3". */
    public static String uniqueToken() {
        return AUTOMATION_PREFIX + "_" + DateUtils.uniqueStamp()
                + "_" + SEQUENCE.incrementAndGet();
    }

    public static String uniqueDepartmentName() {
        return AUTOMATION_PREFIX + " Dept " + DateUtils.uniqueStamp()
                + SEQUENCE.incrementAndGet();
    }

    public static LeaveRequestData leaveWithoutDates(String targetEmployee) {
        return new LeaveRequestData()
                .setScenario("Leave submitted with no dates")
                .setTargetEmployee(targetEmployee)
                .setLeaveType(LeaveType.CASUAL)
                .setReason("Negative validation check");
    }
}
