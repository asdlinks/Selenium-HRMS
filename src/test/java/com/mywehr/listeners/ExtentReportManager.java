package com.mywehr.listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.mywehr.config.ConfigManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Owns the ExtentReports instance and the per-thread ExtentTest.
 *
 * The ExtentTest is held in a ThreadLocal so that a parallel suite attributes
 * each log line and screenshot to the test that produced it.
 */
public final class ExtentReportManager {

    private static final ThreadLocal<ExtentTest> CURRENT_TEST = new ThreadLocal<>();
    private static ExtentReports extent;

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
        String path = ConfigManager.get("report.dir") + "/HRMS_Automation_Report_" + stamp + ".html";

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

    public static synchronized void flush() {
        if (extent != null) {
            extent.flush();
        }
    }
}
