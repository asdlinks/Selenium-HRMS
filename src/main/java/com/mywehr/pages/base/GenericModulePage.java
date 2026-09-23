package com.mywehr.pages.base;

import com.mywehr.enums.AppModule;

/**
 * Page object for modules the smoke suite only needs to load and verify.
 *
 * Writing twelve near-identical classes for Daily Check-In, Shifts, My Team,
 * Reports, Payroll and friends would be ceremony, not coverage. When one of
 * these grows real interaction tests it earns its own class; until then it is
 * addressed through its AppModule entry.
 */
public class GenericModulePage extends BasePage {

    private final AppModule module;

    public GenericModulePage(AppModule module) {
        this.module = module;
    }

    public AppModule module() {
        return module;
    }

    @Override
    public String landingMarker() {
        return module.landingMarker();
    }

    @Override
    public String route() {
        return module.route();
    }
}
