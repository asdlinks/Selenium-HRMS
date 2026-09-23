package com.mywehr.driver;

import com.mywehr.exceptions.FrameworkException;
import org.openqa.selenium.WebDriver;

/**
 * ThreadLocal holder for the active WebDriver.
 *
 * Page objects and utilities pull the driver from here instead of receiving it
 * through constructors, which keeps page-object signatures clean and makes
 * parallel execution safe - every TestNG thread gets its own instance.
 */
public final class DriverManager {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverManager() {
    }

    public static WebDriver get() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new FrameworkException(
                    "No WebDriver bound to thread '" + Thread.currentThread().getName()
                            + "'. A page object was used before BaseTest started the browser.");
        }
        return driver;
    }

    public static void set(WebDriver driver) {
        DRIVER.set(driver);
    }

    public static boolean isInitialised() {
        return DRIVER.get() != null;
    }

    /**
     * Quits the browser and clears the slot. Always call from an @AfterMethod -
     * remove() matters as much as quit(), or a reused TestNG thread keeps a
     * reference to a dead session.
     */
    public static void quit() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            try {
                driver.quit();
            } finally {
                DRIVER.remove();
            }
        }
    }
}
