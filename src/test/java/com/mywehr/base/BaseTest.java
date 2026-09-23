package com.mywehr.base;

import com.aventstack.extentreports.ExtentTest;
import com.mywehr.driver.DriverFactory;
import com.mywehr.driver.DriverManager;
import com.mywehr.enums.AppModule;
import com.mywehr.enums.Persona;
import com.mywehr.listeners.ExtentReportManager;
import com.mywehr.pages.auth.LoginPage;
import com.mywehr.pages.base.BasePage;
import com.mywehr.pages.dashboard.DashboardPage;
import com.mywehr.pages.employees.EmployeeDirectoryPage;
import com.mywehr.utils.Log;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.asserts.SoftAssert;

import java.lang.reflect.Method;

/**
 * Lifecycle shared by every test class.
 *
 * A FRESH BROWSER PER TEST, ON PURPOSE
 * ------------------------------------
 * Each test method gets its own driver and its own login. That costs a few
 * seconds per test and buys three things worth far more: tests can run in any
 * order, a failure cannot cascade into the next test through leftover session
 * state, and the suite parallelises without redesign. In an RBAC suite that
 * repeatedly switches persona, sharing a session would be actively dangerous -
 * a stale admin cookie would silently turn an HR test green.
 */
public abstract class BaseTest {

    /** Persona the current test is signed in as, for logging and teardown. */
    protected Persona activePersona;

    /** Soft checks for the current test; replaced before every test method. */
    private ReportingSoftAssert softAssert = new ReportingSoftAssert();

    // --------------------------------------------------------------- setup

    @BeforeMethod(alwaysRun = true)
    @Parameters({"browser"})
    public void startBrowser(@Optional String browserOverride, Method method) {
        if (browserOverride != null && !browserOverride.isBlank()) {
            System.setProperty("browser", browserOverride);
        }
        softAssert = new ReportingSoftAssert();
        Log.info("Preparing browser for " + method.getName());
        DriverManager.set(DriverFactory.create());
    }

    /**
     * Safety net for a test that recorded soft failures but never called
     * {@link #assertAllSoft()} - without it those failures would be silently
     * dropped and the test reported green.
     */
    @AfterMethod(alwaysRun = true)
    public void failOnUnreportedSoftAssertions(ITestResult result) {
        if (result.getStatus() == ITestResult.SUCCESS && softAssert.hasUnreportedFailures()) {
            Log.error(result.getMethod().getMethodName() + " recorded "
                    + softAssert.failureCount() + " soft failure(s) but never called assertAllSoft()");
            result.setStatus(ITestResult.FAILURE);
            result.setThrowable(new AssertionError(softAssert.failureCount()
                    + " soft assertion(s) failed and were never reported - see the log"));
        }
    }

    @AfterMethod(alwaysRun = true)
    public void stopBrowser() {
        DriverManager.quit();
        activePersona = null;
    }

    // ------------------------------------------------------ session helpers

    /** Signs in and returns the dashboard. */
    protected DashboardPage loginAs(Persona persona) {
        this.activePersona = persona;
        return new LoginPage().loginAs(persona);
    }

    /** Signs in and navigates straight to a module. */
    protected void loginAndOpen(Persona persona, AppModule module) {
        loginAs(persona);
        BasePage.navigateTo(module);
    }

    /** Opens the login page without authenticating. */
    protected LoginPage openLoginPage() {
        LoginPage login = new LoginPage();
        login.open();
        return login;
    }

    // ------------------------------------------------------- soft assertions

    /**
     * Soft checks for this test.
     *
     * THE RULE THE SUITE FOLLOWS
     * --------------------------
     * Hard assertions (org.testng.Assert) guard the checks everything after
     * them depends on: sign-in, a page or dialog being loaded, a record having
     * actually been saved, and security boundaries. Continuing past one of
     * those would only produce noise.
     *
     * Soft assertions cover independent observations - the cards, columns,
     * controls and counts on a screen - so one run reports every one that is
     * wrong rather than stopping at the first. Every test that uses them ends
     * with {@link #assertAllSoft()}.
     */
    protected SoftAssert softly() {
        return softAssert;
    }

    /** Raises every soft failure recorded by this test. Call it last. */
    protected void assertAllSoft() {
        softAssert.assertAll();
    }

    /**
     * Records a soft failure when a page did not load and returns whether it
     * did, so a sweep across several pages can skip one page's checks without
     * abandoning the others.
     */
    protected boolean softlyLoaded(BasePage page, String pageName) {
        boolean loaded = page.isLoaded();
        softAssert.assertTrue(loaded, pageName + " did not load - its checks were skipped");
        return loaded;
    }

    // ------------------------------------------------ environment guards

    /**
     * Skips the current test when the tenant's plan has no free employee seat.
     *
     * Hitting that ceiling is an environment problem, not a product defect, so
     * it is reported as a skip - a failure would point at the registration
     * form and send whoever triages it looking in the wrong place entirely.
     */
    protected void skipIfTenantAtEmployeeLimit(EmployeeDirectoryPage directory) {
        if (directory.isAtEmployeeLimit()) {
            throw new SkipException("Cannot exercise employee registration: "
                    + directory.employeeLimitMessage()
                    + " Free a seat in the tenant (Employees -> Delete on an "
                    + "AT-prefixed record) and re-run.");
        }
    }

    // ------------------------------------------------------- report logging

    /**
     * Records a numbered step in the Extent report and the console.
     *
     * Tests call this for each logical step so a failed run reads as a
     * narrative rather than a stack trace - the difference between a report a
     * developer can triage and one they ignore.
     */
    protected void step(String description) {
        Log.step(description);
        ExtentTest test = ExtentReportManager.currentTest();
        if (test != null) {
            test.info(description);
        }
    }

    /** Records a satisfied expectation. */
    protected void verified(String description) {
        Log.pass("VERIFIED: " + description);
        ExtentTest test = ExtentReportManager.currentTest();
        if (test != null) {
            test.pass(description);
        }
    }

    /** Records context that is neither a step nor an assertion. */
    protected void note(String description) {
        Log.info(description);
        ExtentTest test = ExtentReportManager.currentTest();
        if (test != null) {
            test.info(description);
        }
    }
}
