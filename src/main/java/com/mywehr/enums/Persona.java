package com.mywehr.enums;

import com.mywehr.config.ConfigManager;

/**
 * The application's two test personas.
 *
 * Credentials are read through ConfigManager rather than hardcoded here so a
 * pipeline can inject them (-Dadmin.password=...) without touching source.
 * The values in credentials.json are the committed test-environment defaults.
 */
public enum Persona {

    /** Organization Administrator - the tenant's full-rights "System" role. */
    ADMIN("admin", "Organization Administrator", "Surag"),

    /** HR Administrator - people-operations rights, no org configuration. */
    HR("hr", "HR Administrator", "hr");

    private final String key;
    private final String roleName;
    private final String displayName;

    Persona(String key, String roleName, String displayName) {
        this.key = key;
        this.roleName = roleName;
        this.displayName = displayName;
    }

    public String key() {
        return key;
    }

    /** Role label exactly as rendered on Settings -> Roles & Permissions. */
    public String roleName() {
        return roleName;
    }

    /** Name rendered by the dashboard greeting: "Welcome, {displayName}". */
    public String displayName() {
        return displayName;
    }

    public String tenantCode() {
        return ConfigManager.tenantCode();
    }

    @Override
    public String toString() {
        return name() + "(" + displayName + ")";
    }
}
