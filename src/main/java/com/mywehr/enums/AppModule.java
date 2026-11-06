package com.mywehr.enums;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The application route map, with the on-screen landing marker for each page
 * and the personas allowed to reach it.
 *
 * This enum is the single place the suite encodes "what exists and who may see
 * it". The navigation smoke tests and the RBAC tests are both generated from
 * it, so adding a module to the product means adding one line here - not
 * writing two more tests.
 *
 * ACCESS MATRIX (verified against the test tenant myc001):
 *
 * <pre>
 *   Route                          ADMIN   HR
 *   /dashboard                      yes    yes
 *   /employees                      yes    yes
 *   /department                     yes    yes
 *   /organization                   yes    yes
 *   /my-team                        yes    yes
 *   /attendance                     yes    yes
 *   /leaves                         yes    yes
 *   /cancellation                   yes    yes
 *   /holidays                       yes    yes
 *   /shifts                         yes    yes
 *   /company-documents              yes    yes
 *   /settings                       yes    yes (fewer panels)
 *   /reports                        yes    yes
 *   /payroll                        yes    yes
 *   /work-modes                     yes    NO  -&gt; redirected to /dashboard
 *   /attendance/policies            yes    NO  -&gt; redirected to /dashboard
 *   /attendance/kiosk-devices       yes    NO  -&gt; redirected to /dashboard
 *   /attendance/face-enrollment     yes    NO  -&gt; redirected to /dashboard
 * </pre>
 */
public enum AppModule {

    // ------------------------------------------------ shared by both personas
    DASHBOARD("/dashboard", "Organization-wide analytics", EnumSet.allOf(Persona.class)),
    EMPLOYEE_DIRECTORY("/employees", "Employee Directory", EnumSet.allOf(Persona.class)),
    DEPARTMENT("/department", "Department Management", EnumSet.allOf(Persona.class)),
    ORGANIZATION_STRUCTURE("/organization", "Organization Structure", EnumSet.allOf(Persona.class)),
    MY_TEAM("/my-team", "My Team", EnumSet.allOf(Persona.class)),
    DAILY_CHECK_IN("/attendance", "Daily Check-In", EnumSet.allOf(Persona.class)),
    LEAVES("/leaves", "Leave Management", EnumSet.allOf(Persona.class)),
    LEAVE_CANCELLATION("/cancellation", "Leave Cancellation", EnumSet.allOf(Persona.class)),
    HOLIDAY_CALENDAR("/holidays", "Holiday Calendar", EnumSet.allOf(Persona.class)),
    SHIFTS("/shifts", "Shift Management", EnumSet.allOf(Persona.class)),
    COMPANY_DOCUMENTS("/company-documents", "Company Documents", EnumSet.allOf(Persona.class)),
    SETTINGS("/settings", "Settings", EnumSet.allOf(Persona.class)),
    REPORTS("/reports", "Executive Dashboard", EnumSet.allOf(Persona.class)),
    PAYROLL("/payroll", "Payroll", EnumSet.allOf(Persona.class)),

    // ----------------------------------------------------- administrator only
    WORK_MODES("/work-modes", "Work Modes", EnumSet.of(Persona.ADMIN)),
    ATTENDANCE_POLICIES("/attendance/policies", "Attendance Policies", EnumSet.of(Persona.ADMIN)),
    KIOSK_DEVICES("/attendance/kiosk-devices", "Kiosk Devices", EnumSet.of(Persona.ADMIN)),
    FACE_ENROLLMENT("/attendance/face-enrollment", "Face Enrollment", EnumSet.of(Persona.ADMIN));

    /** Where an unauthorised persona lands when a route guard rejects them. */
    public static final String ACCESS_DENIED_FALLBACK_ROUTE = "/dashboard";

    private final String route;
    private final String landingMarker;
    private final Set<Persona> allowedPersonas;

    AppModule(String route, String landingMarker, Set<Persona> allowedPersonas) {
        this.route = route;
        this.landingMarker = landingMarker;
        this.allowedPersonas = allowedPersonas;
    }

    public String route() {
        return route;
    }

    /**
     * Text that proves the page actually rendered - a heading or subtitle only
     * this page shows. Asserting on it is what makes these "did the page load
     * properly" checks rather than "did the URL change" checks.
     */
    public String landingMarker() {
        return landingMarker;
    }

    public boolean isAccessibleBy(Persona persona) {
        return allowedPersonas.contains(persona);
    }

    public static List<AppModule> accessibleBy(Persona persona) {
        return Arrays.stream(values()).filter(m -> m.isAccessibleBy(persona)).toList();
    }

    public static List<AppModule> deniedTo(Persona persona) {
        return Arrays.stream(values()).filter(m -> !m.isAccessibleBy(persona)).toList();
    }

    @Override
    public String toString() {
        return name() + "[" + route + "]";
    }
}
