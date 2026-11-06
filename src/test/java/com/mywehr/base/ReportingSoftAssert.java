package com.mywehr.base;

import com.aventstack.extentreports.Status;
import com.mywehr.config.ConfigManager;
import com.mywehr.listeners.ExtentReportManager;
import com.mywehr.utils.Log;
import org.testng.asserts.IAssert;
import org.testng.asserts.SoftAssert;

/**
 * A SoftAssert that reports each failure the moment it happens.
 *
 * TestNG's own SoftAssert stays silent until assertAll(), by which point the
 * screen shows whatever the test did last - not the state that was wrong. This
 * one logs the failure and attaches a screenshot at the point of failure, so
 * every soft failure in a combined test carries its own evidence. With
 * screenshot.on.soft.pass enabled, passing checks get a screenshot too, so a
 * green report still shows what each check saw.
 */
public class ReportingSoftAssert extends SoftAssert {

    private int failureCount;
    private boolean asserted;

    @Override
    public void onAssertSuccess(IAssert<?> assertCommand) {
        if (ConfigManager.getBoolean("screenshot.on.soft.pass")) {
            ExtentReportManager.logWithScreenshot(Status.PASS,
                    "Check passed: " + describe(assertCommand), "screenshot.pass.settle.millis");
        }
    }

    @Override
    public void onAssertFailure(IAssert<?> assertCommand, AssertionError ex) {
        failureCount++;
        Log.error("SOFT FAILURE: " + ex.getMessage());
        ExtentReportManager.logWithScreenshot(Status.FAIL,
                "Check failed: " + ex.getMessage(), "screenshot.settle.millis");
    }

    private static String describe(IAssert<?> assertCommand) {
        String message = assertCommand.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        return "expected [" + assertCommand.getExpected() + "], got [" + assertCommand.getActual() + "]";
    }

    @Override
    public void assertAll() {
        asserted = true;
        super.assertAll();
    }

    @Override
    public void assertAll(String message) {
        asserted = true;
        super.assertAll(message);
    }

    /** Failures recorded but never raised because assertAll() was not called. */
    public boolean hasUnreportedFailures() {
        return failureCount > 0 && !asserted;
    }

    public int failureCount() {
        return failureCount;
    }
}
