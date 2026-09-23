package com.mywehr.listeners;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.mywehr.config.ConfigManager;
import com.mywehr.utils.Log;
import com.mywehr.utils.ScreenshotUtils;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Arrays;

/**
 * Reporting listener: opens an Extent node per test, records the outcome and
 * attaches a screenshot on failure.
 *
 * Screenshots are embedded as base64 so the HTML report stays a single
 * self-contained file that can be archived by CI or mailed as-is.
 */
public class TestListener implements ITestListener, ISuiteListener {

    @Override
    public void onStart(ISuite suite) {
        Log.info("=".repeat(78));
        Log.info("SUITE START: " + suite.getName());
        Log.info("Application : " + ConfigManager.baseUrl());
        Log.info("Tenant      : " + ConfigManager.tenantCode());
        Log.info("Browser     : " + ConfigManager.browser()
                + " (headless=" + ConfigManager.headless() + ")");
        Log.info("=".repeat(78));
        ExtentReportManager.instance();
    }

    @Override
    public void onFinish(ISuite suite) {
        ExtentReportManager.flush();
        Log.info("SUITE FINISHED: " + suite.getName());
    }

    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String description = result.getMethod().getDescription();

        ExtentTest test = ExtentReportManager.instance()
                .createTest(testName, description == null ? "" : description);

        // TestNG groups become Extent categories, so the report can be filtered
        // the same way the suite files slice the tests.
        String[] groups = result.getMethod().getGroups();
        if (groups.length > 0) {
            test.assignCategory(groups);
        }
        if (result.getParameters().length > 0) {
            test.info("Dataset: " + Arrays.toString(result.getParameters()));
        }

        ExtentReportManager.setCurrentTest(test);

        Log.info("-".repeat(78));
        Log.info("TEST START: " + testName);
        if (description != null && !description.isBlank()) {
            Log.info("            " + description);
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTest test = ExtentReportManager.currentTest();
        if (test != null) {
            test.log(Status.PASS, "Test passed in " + durationOf(result) + "s");
            if (ConfigManager.getBoolean("screenshot.on.success")) {
                attachScreenshot(test, Status.PASS, "Final state");
            }
        }
        Log.pass("TEST PASSED: " + result.getMethod().getMethodName()
                + " (" + durationOf(result) + "s)");
        ExtentReportManager.clearCurrentTest();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = ExtentReportManager.currentTest();
        Throwable throwable = result.getThrowable();

        if (test != null) {
            test.log(Status.FAIL, "Test failed after " + durationOf(result) + "s");
            if (throwable != null) {
                test.fail(throwable);
            }
            if (ConfigManager.getBoolean("screenshot.on.failure")) {
                attachScreenshot(test, Status.FAIL, "Screen at the point of failure");
            }
        }

        // A PNG on disk as well as in the report - CI artifacts are easier to
        // browse than an HTML file when a whole suite goes red.
        ScreenshotUtils.captureToFile(result.getMethod().getMethodName());

        Log.error("TEST FAILED: " + result.getMethod().getMethodName());
        if (throwable != null) {
            Log.error("  Reason", throwable);
        }
        ExtentReportManager.clearCurrentTest();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = ExtentReportManager.currentTest();
        if (test != null) {
            String reason = result.getThrowable() == null
                    ? "Skipped - a dependency did not pass"
                    : result.getThrowable().getMessage();
            test.log(Status.SKIP, reason);
        }
        Log.warn("TEST SKIPPED: " + result.getMethod().getMethodName());
        ExtentReportManager.clearCurrentTest();
    }

    @Override
    public void onFinish(ITestContext context) {
        Log.info("-".repeat(78));
        Log.info(String.format("RESULTS for %s -> passed: %d, failed: %d, skipped: %d",
                context.getName(),
                context.getPassedTests().size(),
                context.getFailedTests().size(),
                context.getSkippedTests().size()));
        ExtentReportManager.flush();
    }

    // ------------------------------------------------------------- helpers

    private void attachScreenshot(ExtentTest test, Status status, String caption) {
        String base64 = ScreenshotUtils.captureAsBase64();
        if (base64 == null) {
            return;
        }
        try {
            test.log(status, caption,
                    MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
        } catch (Exception e) {
            Log.warn("Could not attach the screenshot to the report: " + e.getMessage());
        }
    }

    private String durationOf(ITestResult result) {
        return String.format("%.1f", (result.getEndMillis() - result.getStartMillis()) / 1000.0);
    }
}
