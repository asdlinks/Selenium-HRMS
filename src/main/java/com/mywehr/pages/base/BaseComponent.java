package com.mywehr.pages.base;

import com.mywehr.driver.DriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * A piece of UI that is not a page: it has no route of its own and lives on
 * top of, or inside, whatever page is showing - the header, the side rail, a
 * dialog, a drawer.
 *
 * Components follow the same rules as pages (private locators, intent-level
 * methods, no assertions) but are reached through a page method
 * ({@code directory.openEditFor(name)}, {@code page.header()}) rather than
 * opened by URL.
 */
public abstract class BaseComponent {

    protected WebDriver driver() {
        return DriverManager.get();
    }

    protected JavascriptExecutor js() {
        return (JavascriptExecutor) driver();
    }

    /** True when the component is on screen. */
    public abstract boolean isDisplayed();
}
