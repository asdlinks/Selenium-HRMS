package com.mywehr.driver;

import com.mywehr.config.ConfigManager;
import com.mywehr.exceptions.FrameworkException;
import com.mywehr.utils.Log;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.util.Locale;

/**
 * Creates the browser for a test thread.
 *
 * Driver binaries are resolved by Selenium Manager (built into Selenium 4.6+),
 * so there is no WebDriverManager dependency and nothing to keep in sync when
 * Chrome auto-updates.
 */
public final class DriverFactory {

    private DriverFactory() {
    }

    public static WebDriver create() {
        String browser = ConfigManager.browser().toLowerCase(Locale.ROOT).trim();
        boolean headless = ConfigManager.headless();

        Log.info("Starting browser '" + browser + "' (headless=" + headless + ")");

        WebDriver driver = switch (browser) {
            case "chrome" -> new ChromeDriver(chromeOptions(headless));
            case "firefox" -> new FirefoxDriver(firefoxOptions(headless));
            case "edge" -> new EdgeDriver(edgeOptions(headless));
            case "remote-chrome" -> remote(chromeOptions(headless));
            case "remote-firefox" -> remote(firefoxOptions(headless));
            default -> throw new FrameworkException(
                    "Unsupported browser '" + browser + "'. Use chrome, firefox, edge, "
                            + "remote-chrome or remote-firefox.");
        };

        applyTimeouts(driver);
        applyWindowSize(driver, headless);
        return driver;
    }

    // --------------------------------------------------------------- options

    private static ChromeOptions chromeOptions(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        // A predictable window size keeps MUI responsive breakpoints stable:
        // below ~900px the app collapses its side rail into a drawer and the
        // nav locators would legitimately stop matching.
        options.addArguments("--window-size=" + ConfigManager.get("browser.width")
                + "," + ConfigManager.get("browser.height"));
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-popup-blocking");
        // Suppresses the "Chrome is being controlled by automated software" bar
        // and the password-manager bubble, both of which can steal focus.
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.addArguments("--disable-features=PasswordLeakDetection,AutofillServerCommunication");
        return options;
    }

    private static FirefoxOptions firefoxOptions(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        if (headless) {
            options.addArguments("-headless");
        }
        options.addPreference("dom.webnotifications.enabled", false);
        options.addPreference("signon.rememberSignons", false);
        return options;
    }

    private static EdgeOptions edgeOptions(boolean headless) {
        EdgeOptions options = new EdgeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--remote-allow-origins=*");
        return options;
    }

    private static WebDriver remote(org.openqa.selenium.MutableCapabilities capabilities) {
        String gridUrl = ConfigManager.get("grid.url");
        try {
            return new RemoteWebDriver(URI.create(gridUrl).toURL(), capabilities);
        } catch (MalformedURLException e) {
            throw new FrameworkException("Invalid grid.url: " + gridUrl, e);
        }
    }

    // --------------------------------------------------------------- tuning

    private static void applyTimeouts(WebDriver driver) {
        // Implicit wait stays at zero on purpose. Mixing implicit and explicit
        // waits makes every negative assertion ("this element must NOT be
        // there") pay the implicit timeout, and the RBAC suite is full of them.
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        driver.manage().timeouts().pageLoadTimeout(ConfigManager.pageLoadTimeout());
        driver.manage().timeouts().scriptTimeout(ConfigManager.scriptTimeout());
    }

    private static void applyWindowSize(WebDriver driver, boolean headless) {
        if (headless) {
            driver.manage().window().setSize(new Dimension(
                    ConfigManager.getInt("browser.width"),
                    ConfigManager.getInt("browser.height")));
        } else if (ConfigManager.getBoolean("window.maximize")) {
            driver.manage().window().maximize();
        }
    }
}
