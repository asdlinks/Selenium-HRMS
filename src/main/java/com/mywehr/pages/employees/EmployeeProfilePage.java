package com.mywehr.pages.employees;

import com.mywehr.pages.base.BasePage;

/**
 * An employee's profile page (/profile/{id}), reached from the directory's
 * "View Profile" card action.
 *
 * The id is server-generated, so this page is navigated to through
 * {@link EmployeeDirectoryPage#openProfileOf(String)} rather than opened by
 * URL; {@link #route()} is the shared prefix only.
 */
public class EmployeeProfilePage extends BasePage {

    @Override
    public String landingMarker() {
        return "Employee Profile";
    }

    @Override
    public String route() {
        return "/profile/";
    }

    /** True when the profile shows the value anywhere in its main content. */
    public boolean shows(String value) {
        return contentText().contains(value);
    }
}
