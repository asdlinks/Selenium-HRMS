package com.mywehr.listeners;

import com.mywehr.config.ConfigManager;
import com.mywehr.utils.Log;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Retries a failed test a bounded number of times.
 *
 * DELIBERATELY CONSERVATIVE. Retrying hides real intermittent product bugs, so
 * the default is a single retry and assertion failures are never retried -
 * only infrastructure-shaped failures (timeouts, stale elements, a session
 * that died) are. A test whose assertion failed did its job; running it again
 * just delays the bad news.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Map<String, Integer> ATTEMPTS = new ConcurrentHashMap<>();

    @Override
    public boolean retry(ITestResult result) {
        if (!isRetryable(result.getThrowable())) {
            return false;
        }

        int maxRetries = ConfigManager.retryCount();
        String key = testKey(result);
        int attempts = ATTEMPTS.getOrDefault(key, 0);

        if (attempts >= maxRetries) {
            return false;
        }

        ATTEMPTS.put(key, attempts + 1);
        Log.warn("Retrying " + result.getName() + " (attempt " + (attempts + 1)
                + " of " + maxRetries + ") after "
                + result.getThrowable().getClass().getSimpleName());
        return true;
    }

    /**
     * An AssertionError means the product behaved differently from the spec,
     * which is a result worth reporting rather than re-rolling.
     */
    private boolean isRetryable(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        if (throwable instanceof AssertionError) {
            return false;
        }
        return throwable instanceof org.openqa.selenium.TimeoutException
                || throwable instanceof org.openqa.selenium.StaleElementReferenceException
                || throwable instanceof org.openqa.selenium.ElementClickInterceptedException
                || throwable instanceof org.openqa.selenium.NoSuchElementException
                || throwable instanceof org.openqa.selenium.WebDriverException;
    }

    private String testKey(ITestResult result) {
        return result.getTestClass().getName() + "#" + result.getName()
                + "#" + java.util.Arrays.toString(result.getParameters());
    }
}
