package com.mywehr.config;

import com.mywehr.exceptions.FrameworkException;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

/**
 * Single source of truth for configuration.
 *
 * Resolution order (first hit wins):
 *   1. JVM system property   -Dbrowser=firefox        (CI / Maven surefire)
 *   2. OS environment var    BROWSER=firefox          (containers, Jenkins)
 *   3. config.properties     browser=chrome           (committed default)
 *
 * That order is what lets one committed config file serve local runs, the
 * "ci" Maven profile and a Grid run without ever being edited.
 */
public final class ConfigManager {

    private static final String CONFIG_FILE = "config.properties";
    private static final Properties PROPERTIES = load();

    private ConfigManager() {
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream stream = ConfigManager.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (stream == null) {
                throw new FrameworkException(CONFIG_FILE + " not found on the test classpath");
            }
            properties.load(stream);
            return properties;
        } catch (IOException e) {
            throw new FrameworkException("Unable to read " + CONFIG_FILE, e);
        }
    }

    // ------------------------------------------------------------------ core

    public static String get(String key) {
        String value = resolve(key);
        if (value == null || value.isBlank()) {
            throw new FrameworkException("Config key '" + key + "' is missing or empty");
        }
        return value.trim();
    }

    public static String get(String key, String defaultValue) {
        String value = resolve(key);
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }

    public static int getInt(String key) {
        String value = get(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new FrameworkException("Config key '" + key + "' is not a number: " + value, e);
        }
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    public static Duration getDuration(String key) {
        return Duration.ofSeconds(getInt(key));
    }

    private static String resolve(String key) {
        String fromSystem = System.getProperty(key);
        if (fromSystem != null && !fromSystem.isBlank()) {
            return fromSystem;
        }
        String fromEnv = System.getenv(key.toUpperCase().replace('.', '_'));
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return PROPERTIES.getProperty(key);
    }

    // ------------------------------------------------------- typed shortcuts

    public static String baseUrl() {
        return get("base.url");
    }

    public static String loginUrl() {
        return baseUrl() + get("login.path");
    }

    /** Absolute URL for an in-app route such as {@code /employees}. */
    public static String urlFor(String route) {
        return baseUrl() + route;
    }

    public static String tenantCode() {
        return get("tenant.code");
    }

    public static String browser() {
        return get("browser");
    }

    public static boolean headless() {
        return getBoolean("headless");
    }

    public static Duration explicitWait() {
        return getDuration("explicit.wait");
    }

    public static Duration spaSettleWait() {
        return getDuration("spa.settle.wait");
    }

    public static Duration pollingInterval() {
        return Duration.ofMillis(getInt("polling.interval.millis"));
    }

    public static Duration pageLoadTimeout() {
        return getDuration("page.load.timeout");
    }

    public static Duration scriptTimeout() {
        return getDuration("script.timeout");
    }

    public static int retryCount() {
        return Integer.parseInt(get("retry.count", "1"));
    }
}
