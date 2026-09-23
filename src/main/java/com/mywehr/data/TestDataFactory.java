package com.mywehr.data;

import com.mywehr.data.model.EmployeeData;
import com.mywehr.data.model.LeaveRequestData;
import com.mywehr.enums.LeaveType;
import com.mywehr.utils.DateUtils;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds unique, valid test data at runtime.
 *
 * WHY NOT STATIC FIXTURES
 * -----------------------
 * The HRMS rejects duplicate employee emails and employee ids. A committed
 * fixture with a fixed email passes once, then fails on every subsequent run
 * against the same tenant - the classic "works on my machine, red in CI
 * tomorrow" failure. Every generated record therefore carries a timestamp and
 * a per-JVM counter, which also makes it trivial to spot and clean up
 * automation-created rows in the test tenant.
 *
 * Static reference data that the tenant already owns (department "Sales",
 * designation "Brand head" ...) stays in JSON, because those are inputs the
 * tests select rather than create.
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

    public static String uniqueEmail() {
        return "at.auto." + DateUtils.uniqueStamp()
                + SEQUENCE.incrementAndGet() + "@yopmail.com";
    }

    public static String uniqueDepartmentName() {
        return AUTOMATION_PREFIX + " Dept " + DateUtils.uniqueStamp()
                + SEQUENCE.incrementAndGet();
    }

    public static String uniqueHolidayName() {
        return AUTOMATION_PREFIX + " Holiday " + DateUtils.uniqueStamp()
                + SEQUENCE.incrementAndGet();
    }

    /**
     * A fully valid employee payload using reference data that already exists
     * in tenant myc001.
     */
    public static EmployeeData validEmployee() {
        String token = uniqueToken();
        return new EmployeeData()
                .setScenario("Valid employee " + token)
                .setFullName(AUTOMATION_PREFIX + " Tester " + SEQUENCE.get())
                .setEmail(uniqueEmail())
                .setDateOfBirth(DateUtils.forDateInput(LocalDate.of(1995, 6, 15)))
                .setDepartment("Sales")
                .setDesignation("Brand head")
                .setEmployeeId(token)
                .setProbationMonths("3")
                .setJoiningDate(DateUtils.forDateInput(DateUtils.today()))
                .setRole("Employee")
                .setInitialPassword("Automation@123");
    }

    /** Employee payload targeted at a specific department. */
    public static EmployeeData validEmployeeIn(String department) {
        return validEmployee().setDepartment(department);
    }

    /**
     * A single-day future leave request.
     *
     * The date is pushed to a working day: applying on a weekly off exercises
     * different app behaviour and would make balance assertions ambiguous.
     */
    /**
     * A single-day leave on a random working day two weeks to six months out.
     *
     * Randomised because the app refuses leave that overlaps an existing
     * request: a fixed "three days from now" makes the second run of the day
     * fail on data the first run left behind.
     */
    public static LeaveRequestData singleDayLeave(String targetEmployee) {
        LocalDate day = DateUtils.nextWorkingDay(
                java.util.concurrent.ThreadLocalRandom.current().nextInt(14, 180));
        return new LeaveRequestData()
                .setScenario("Single day casual leave for " + targetEmployee)
                .setTargetEmployee(targetEmployee)
                .setLeaveType(LeaveType.CASUAL)
                .setStartDate(DateUtils.forDateInput(day))
                .setEndDate(DateUtils.forDateInput(day))
                .setReason("Automated regression check " + uniqueToken());
    }

    public static LeaveRequestData leaveWithoutDates(String targetEmployee) {
        return new LeaveRequestData()
                .setScenario("Leave submitted with no dates")
                .setTargetEmployee(targetEmployee)
                .setLeaveType(LeaveType.CASUAL)
                .setReason("Negative validation check");
    }
}
