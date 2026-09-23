package com.mywehr.pages.auth;

import com.mywehr.config.ConfigManager;
import com.mywehr.data.model.Credential;
import com.mywehr.data.reader.JsonDataReader;
import com.mywehr.driver.DriverManager;
import com.mywehr.enums.Persona;
import com.mywehr.pages.base.BasePage;
import com.mywehr.pages.dashboard.DashboardPage;
import com.mywehr.utils.ElementUtils;
import com.mywehr.utils.Log;
import com.mywehr.utils.WaitUtils;
import org.openqa.selenium.By;

import java.time.Duration;

/**
 * The tenant-scoped sign-in page.
 *
 * Authentication needs three inputs rather than the usual two - the company
 * code selects the tenant - and all three carry stable name attributes, which
 * makes this the one page in the app with genuinely first-class locators.
 */
public class LoginPage extends BasePage {

    // --- form controls (stable name attributes) ---
    private static final By COMPANY_CODE = By.cssSelector("input[name='tenantCode']");
    private static final By EMAIL = By.cssSelector("input[name='email']");
    private static final By PASSWORD = By.cssSelector("input[name='password']");
    private static final By SIGN_IN_BUTTON = By.cssSelector("button[type='submit']");

    // --- static content ---
    private static final By WELCOME_HEADING =
            By.xpath("//*[normalize-space(text())='Welcome Back']");
    private static final By COMPANY_LOGO = By.cssSelector("img[alt='Company Logo']");
    private static final By FORGOT_PASSWORD =
            By.xpath("//button[contains(normalize-space(.),'Forgot your password')]");

    // --- feedback ---
    private static final By ERROR_ALERT =
            By.xpath("//*[contains(@class,'MuiAlert-root') or @role='alert']"
                    + "[contains(.,'Invalid') or contains(.,'incorrect') or contains(.,'failed')]");

    /** Message the API returns for every bad-credential combination. */
    public static final String INVALID_CREDENTIALS_MESSAGE =
            "Invalid company code, email or password";

    @Override
    public String landingMarker() {
        return "Welcome Back";
    }

    @Override
    public String route() {
        return ConfigManager.get("login.path");
    }

    // ------------------------------------------------------------ rendering

    public boolean isCompanyLogoDisplayed() {
        return ElementUtils.isDisplayed(COMPANY_LOGO);
    }

    public boolean isWelcomeHeadingDisplayed() {
        return ElementUtils.isDisplayed(WELCOME_HEADING);
    }

    public boolean areAllCredentialFieldsDisplayed() {
        return ElementUtils.isDisplayed(COMPANY_CODE)
                && ElementUtils.isDisplayed(EMAIL)
                && ElementUtils.isDisplayed(PASSWORD)
                && ElementUtils.isDisplayed(SIGN_IN_BUTTON);
    }

    public boolean isForgotPasswordDisplayed() {
        return ElementUtils.isDisplayed(FORGOT_PASSWORD);
    }

    /** The password field must mask input - a basic but worthwhile check. */
    public boolean isPasswordMasked() {
        return "password".equals(ElementUtils.getAttribute(PASSWORD, "type"));
    }

    // --------------------------------------------------------------- actions

    public LoginPage enterCompanyCode(String companyCode) {
        ElementUtils.type(COMPANY_CODE, companyCode);
        return this;
    }

    public LoginPage enterEmail(String email) {
        ElementUtils.type(EMAIL, email);
        return this;
    }

    public LoginPage enterPassword(String password) {
        ElementUtils.type(PASSWORD, password);
        return this;
    }

    /**
     * Submits the form and confirms the app actually reacted.
     *
     * A sign-in has exactly two legitimate outcomes: the browser leaves the
     * login route, or a rejection banner appears. Seeing NEITHER means the
     * click never reached React - and the symptom of that is brutal to
     * diagnose, because the test reports "sign-in failed" with an empty error
     * message while the credentials were perfectly good. The retry replays the
     * full pointer sequence.
     */
    public LoginPage submit() {
        Log.step("Submitting the sign-in form");
        ElementUtils.click(SIGN_IN_BUTTON);

        if (submissionWasProcessed()) {
            return this;
        }

        Log.warn("The sign-in form did not react to the submit - "
                + "replaying the full pointer sequence");
        ElementUtils.clickViaScript(SIGN_IN_BUTTON);
        submissionWasProcessed();
        return this;
    }

    /** Waits briefly for the app to either navigate away or reject the attempt. */
    private boolean submissionWasProcessed() {
        try {
            WaitUtils.fluently(d -> !isStillOnLoginPage()
                            || !d.findElements(ERROR_ALERT).isEmpty(),
                    Duration.ofSeconds(10),
                    "The sign-in form neither navigated nor raised an error");
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }

    /** Fills the form and submits, without asserting the outcome. */
    public LoginPage fillAndSubmit(String companyCode, String email, String password) {
        enterCompanyCode(companyCode);
        enterEmail(email);
        enterPassword(password);
        return submit();
    }

    public LoginPage fillAndSubmit(Credential credential) {
        return fillAndSubmit(credential.getCompanyCode(),
                credential.getEmail(),
                credential.getPassword());
    }

    // ------------------------------------------------------- happy path API

    /**
     * Signs in as a persona and waits for the dashboard.
     *
     * The single entry point used by test setup, so credentials are resolved
     * from the dataset in exactly one place.
     */
    public DashboardPage loginAs(Persona persona) {
        Credential credential = JsonDataReader.credentialFor(persona.key());
        Log.step("Signing in as " + persona + " (" + credential.getEmail() + ")");

        open();
        fillAndSubmit(credential);

        DashboardPage dashboard = new DashboardPage();
        boolean loaded = dashboard.isLoaded();

        // Signed in, on /dashboard, yet nothing rendered: the app occasionally
        // leaves a blank white page after login. The session is valid, so one
        // reload recovers it. Logged as a warning so a rising rate is visible
        // in the report rather than silently absorbed.
        if (!loaded && currentPath().startsWith("/dashboard")) {
            Log.warn("Signed in but the dashboard rendered blank - reloading once");
            DriverManager.get().navigate().refresh();
            loaded = dashboard.isLoaded();
        }

        if (!loaded) {
            throw new IllegalStateException(
                    "Sign-in as " + persona + " did not reach the dashboard. Current path: "
                            + currentPath() + ", error on screen: '" + errorMessage() + "'");
        }
        Log.pass("Signed in as " + persona);
        return dashboard;
    }

    // -------------------------------------------------------- error handling

    /**
     * Waits briefly for the rejection banner and returns its text.
     * Empty string when no error appeared, so negative tests can assert on the
     * absence of an error just as easily as on its presence.
     */
    public String errorMessage() {
        if (WaitUtils.isVisibleWithin(ERROR_ALERT, Duration.ofSeconds(10))) {
            return ElementUtils.getText(ERROR_ALERT);
        }
        return "";
    }

    public boolean isErrorDisplayed() {
        return WaitUtils.isVisibleWithin(ERROR_ALERT, Duration.ofSeconds(10));
    }

    /** True while the browser is still parked on the login route. */
    public boolean isStillOnLoginPage() {
        return currentPath().startsWith(route());
    }
}
