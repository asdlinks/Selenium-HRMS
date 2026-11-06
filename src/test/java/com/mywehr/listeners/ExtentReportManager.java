package com.mywehr.listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.mywehr.config.ConfigManager;
import com.mywehr.utils.ScreenshotUtils;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Owns the ExtentReports instance and the per-thread ExtentTest.
 *
 * The ExtentTest is held in a ThreadLocal so that a parallel suite attributes
 * each log line and screenshot to the test that produced it.
 *
 * Each run gets its own folder, report.dir/Run_&lt;stamp&gt;/, holding the HTML
 * report and a screenshots/ subfolder. Screenshots are linked by relative path
 * rather than embedded, so the report stays small and opens as long as the
 * folder is kept together.
 */
public final class ExtentReportManager {

    private static final ThreadLocal<ExtentTest> CURRENT_TEST = new ThreadLocal<>();
    private static final AtomicInteger SCREENSHOT_SEQ = new AtomicInteger();
    private static ExtentReports extent;
    private static File runDir;

    private ExtentReportManager() {
    }

    public static synchronized ExtentReports instance() {
        if (extent == null) {
            extent = build();
        }
        return extent;
    }

    private static ExtentReports build() {
        String stamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        runDir = new File(ConfigManager.get("report.dir"), "Run_" + stamp);
        String path = new File(runDir, "HRMS_Automation_Report_" + stamp + ".html").getPath();

        ExtentSparkReporter reporter = new ExtentSparkReporter(path);
        reporter.config().setTheme(Theme.DARK);
        reporter.config().setDocumentTitle("MyWe HRMS - Automation Report");
        reporter.config().setReportName("MyWe HRMS Smoke &amp; Regression Suite");
        reporter.config().setTimeStampFormat("dd-MM-yyyy HH:mm:ss");

        ExtentReports reports = new ExtentReports();
        reports.attachReporter(reporter);

        // Environment block - the first thing anyone triaging a red run needs.
        reports.setSystemInfo("Application", ConfigManager.baseUrl());
        reports.setSystemInfo("Tenant / Company Code", ConfigManager.tenantCode());
        reports.setSystemInfo("Browser", ConfigManager.browser());
        reports.setSystemInfo("Headless", String.valueOf(ConfigManager.headless()));
        reports.setSystemInfo("Java", System.getProperty("java.version"));
        reports.setSystemInfo("OS", System.getProperty("os.name"));
        reports.setSystemInfo("Executed By", System.getProperty("user.name"));

        System.out.println("Extent report will be written to: " + path);
        return reports;
    }

    public static void setCurrentTest(ExtentTest test) {
        CURRENT_TEST.set(test);
    }

    public static ExtentTest currentTest() {
        return CURRENT_TEST.get();
    }

    public static void clearCurrentTest() {
        CURRENT_TEST.remove();
    }

    /**
     * Logs {@code details} to the current test with a screenshot taken once the
     * screen has settled. Falls back to a text-only entry if no test is active
     * or the capture fails.
     *
     * @param settleMillisKey config key bounding the wait for loaders to clear
     */
    public static void logWithScreenshot(Status status, String details, String settleMillisKey) {
        ExtentTest test = currentTest();
        if (test == null) {
            return;
        }
        instance();
        String name = String.format("%04d_%s_%s.png",
                SCREENSHOT_SEQ.incrementAndGet(),
                test.getModel().getName().replaceAll("[^a-zA-Z0-9]+", "_"),
                status.toString().toLowerCase());
        String relative = "screenshots/" + name;
        Duration settle = Duration.ofMillis(ConfigManager.getInt(settleMillisKey));
        try {
            if (ScreenshotUtils.captureSettledToFile(new File(runDir, relative), settle)) {
                test.log(status, details, MediaEntityBuilder.createScreenCaptureFromPath(relative).build());
                return;
            }
        } catch (RuntimeException e) {
            // fall through to a text-only entry
        }
        test.log(status, details);
    }

    public static synchronized void flush() {
        if (extent != null) {
            extent.flush();
        }
    }
}
