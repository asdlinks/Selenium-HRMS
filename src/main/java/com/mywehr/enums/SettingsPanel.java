package com.mywehr.enums;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Settings is a single route (/settings) whose left rail is rendered from the
 * caller permissions, so panel visibility - not routing - is the RBAC surface
 * here. A persona who cannot manage company configuration simply never sees
 * the rail entry. Verified against tenant myc001:
 *
 * <pre>
 *   ADMIN sees all 12 entries.
 *   HR    sees only 4: Locations / Offices, Holiday Config,
 *                      Payroll Settings, Account Security.
 * </pre>
 *
 * TWO LABELS PER PANEL, DELIBERATELY
 * ----------------------------------
 * The rail label and the heading the panel renders are frequently different -
 * "Locations / Offices" opens "Locations Management", "General Config" opens
 * "Leave Allocations", "Account Security" opens "Password Management". Storing
 * only the rail label would make "did the panel open?" unverifiable, because
 * the rail label is on screen whether or not the click did anything. Asserting
 * on the content heading is what makes the check real.
 *
 * Audit &amp; Compliance is not a panel at all: it is a link out to
 * /reports/audit. It is modelled here because it occupies a rail slot and
 * carries the same permission boundary, but it is flagged so the panel tests
 * do not expect an in-place heading from it.
 */
public enum SettingsPanel {

    COMPANY_PROFILE("Company Profile", "Company Profile",
            "ORGANIZATION", EnumSet.of(Persona.ADMIN)),
    GENERAL_CONFIG("General Config", "Leave Allocations",
            "ORGANIZATION", EnumSet.of(Persona.ADMIN)),
    LOCATIONS_OFFICES("Locations / Offices", "Locations Management",
            "ORGANIZATION", EnumSet.allOf(Persona.class)),

    HOLIDAY_CONFIG("Holiday Config", "Holiday Management",
            "ATTENDANCE", EnumSet.allOf(Persona.class)),
    ATTENDANCE_RULES("Attendance Rules", "Attendance Rules",
            "ATTENDANCE", EnumSet.of(Persona.ADMIN)),
    WORK_MODES("Work Modes", "Work Modes",
            "ATTENDANCE", EnumSet.of(Persona.ADMIN)),

    PAYROLL_SETTINGS("Payroll Settings", "Payroll Settings",
            "PAYROLL", EnumSet.allOf(Persona.class)),

    ACCOUNT_SECURITY("Account Security", "Password Management",
            "SECURITY", EnumSet.allOf(Persona.class)),

    MENU_MANAGEMENT("Menu Management", "Menu Management",
            "ADVANCED", EnumSet.of(Persona.ADMIN)),
    ROLES_AND_PERMISSIONS("Roles & Permissions", "Roles & Permissions",
            "ADVANCED", EnumSet.of(Persona.ADMIN)),
    SALARY_GRADES("Salary Grades", "Salary Grades",
            "ADVANCED", EnumSet.of(Persona.ADMIN)),

    /** Navigates to /reports/audit rather than opening an in-place panel. */
    AUDIT_AND_COMPLIANCE("Audit & Compliance", null,
            "ADVANCED", EnumSet.of(Persona.ADMIN), "/reports/audit");

    private final String label;
    private final String contentHeading;
    private final String group;
    private final Set<Persona> allowedPersonas;
    private final String navigatesTo;

    SettingsPanel(String label, String contentHeading, String group, Set<Persona> allowedPersonas) {
        this(label, contentHeading, group, allowedPersonas, null);
    }

    SettingsPanel(String label, String contentHeading, String group,
                  Set<Persona> allowedPersonas, String navigatesTo) {
        this.label = label;
        this.contentHeading = contentHeading;
        this.group = group;
        this.allowedPersonas = allowedPersonas;
        this.navigatesTo = navigatesTo;
    }

    /** Exact rail label as rendered in the left column. */
    public String label() {
        return label;
    }

    /**
     * Heading the opened panel renders on the right. Null for entries that
     * navigate away instead of opening a panel.
     */
    public String contentHeading() {
        return contentHeading;
    }

    /** Section header the entry sits under (ORGANIZATION, ATTENDANCE, ...). */
    public String group() {
        return group;
    }

    /** True when activating this entry routes away from /settings. */
    public boolean isNavigationLink() {
        return navigatesTo != null;
    }

    /** Route this entry navigates to, or null when it opens a panel in place. */
    public String navigatesTo() {
        return navigatesTo;
    }

    public boolean isVisibleTo(Persona persona) {
        return allowedPersonas.contains(persona);
    }

    public static List<SettingsPanel> visibleTo(Persona persona) {
        return Arrays.stream(values()).filter(p -> p.isVisibleTo(persona)).toList();
    }

    public static List<SettingsPanel> hiddenFrom(Persona persona) {
        return Arrays.stream(values()).filter(p -> !p.isVisibleTo(persona)).toList();
    }

    /** Entries that open a panel in place - the ones a panel test can assert on. */
    public static List<SettingsPanel> inPlacePanelsVisibleTo(Persona persona) {
        return visibleTo(persona).stream().filter(p -> !p.isNavigationLink()).toList();
    }

    @Override
    public String toString() {
        return label;
    }
}
