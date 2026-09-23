package com.mywehr.base;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.mywehr.listeners.ExtentReportManager;
import com.mywehr.utils.Log;
import com.mywehr.utils.ScreenshotUtils;
import org.testng.asserts.IAssert;
import org.testng.asserts.SoftAssert;

/**
 * A SoftAssert that reports each failure the moment it happens.
 *
 * TestNG's own SoftAssert stays silent until assertAll(), by which point the
 * screen shows whatever the test did last - not the state that was wrong. This
 * one logs the failure and attaches a screenshot at the point of failure, so
 * every soft failure in a combined test carries its own evidence.
 */
public class ReportingSoftAssert extends SoftAssert {

    private int failureCount;
    private boolean asserted;

    @Override
    public void onAssertFailure(IAssert<?> assertCommand, AssertionError ex) {
        failureCount++;
        Log.error("SOFT FAILURE: " + ex.getMessage());

        ExtentTest test = ExtentReportManager.currentTest();
        if (test == null) {
            return;
        }
        String base64 = ScreenshotUtils.captureAsBase64();
        try {
            if (base64 != null) {
                test.log(Status.FAIL, "Check failed: " + ex.getMessage(),
                        MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
            } else {
                test.log(Status.FAIL, "Check failed: " + ex.getMessage());
            }
        } catch (Exception e) {
            test.log(Status.FAIL, "Check failed: " + ex.getMessage());
        }
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
