package com.mywehr.tests.auth;

import com.mywehr.base.BaseTest;
import com.mywehr.data.model.Credential;
import com.mywehr.data.reader.JsonDataReader;
import com.mywehr.dataproviders.TestDataProviders;
import com.mywehr.enums.AppModule;
import com.mywehr.enums.Persona;
import com.mywehr.pages.auth.LoginPage;
import com.mywehr.pages.base.BasePage;
import com.mywehr.pages.dashboard.DashboardPage;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * TC_AUTH - Authentication and session handling.
 *
 * The tenant-scoped login (company code + email + password) is the gate every
 * other test passes through, so these run first and the rest of the suite
 * depends on them.
 */
public class AuthenticationSmokeTest extends BaseTest {

    @Test(priority = 1,
            groups = {"smoke", "auth", "critical"},
            description = "TC_AUTH_01 - The login page renders every credential field, "
                    + "the company branding and the forgot-password help")
    public void TC_AUTH_01_loginPageRendersAllCredentialFieldsAndBranding() {
        step("Opening the login page without an authenticated session");
        LoginPage login = openLoginPage();
        assertTrue(login.isLoaded(),
                "The login page did not render its 'Welcome Back' heading");

        step("Verifying the page furniture and the credential form");
        softly().assertTrue(login.isWelcomeHeadingDisplayed(),
                "The 'Welcome Back' heading is missing");
        softly().assertTrue(login.isCompanyLogoDisplayed(),
                "The company logo is missing from the login page");
        softly().assertTrue(login.areAllCredentialFieldsDisplayed(),
                "Company code, email, password or the Sign In button is missing");
        softly().assertTrue(login.isPasswordMasked(),
                "The password field is not of type=password, so input is not masked");
        softly().assertTrue(login.isForgotPasswordDisplayed(),
                "The 'Forgot your password?' control is missing");

        assertAllSoft();
        verified("The login page renders completely and masks the password");
    }

    @Test(priority = 2,
            groups = {"smoke", "auth", "negative", "critical"},
            description = "TC_AUTH_02 - Every invalid credential combination is rejected with "
                    + "a generic message and the user is kept on the login page")
    public void TC_AUTH_02_invalidCredentialCombinationsAreRejected() {
        List<Credential> scenarios = JsonDataReader.invalidLoginScenarios();
        note("Invalid sign-in scenarios under test: " + scenarios.size());

        for (Credential credential : scenarios) {
            step("Attempting sign-in: " + credential.getScenario());
            LoginPage login = openLoginPage();
            login.fillAndSubmit(credential);

            // Hard: an invalid sign-in that gets through is a security defect,
            // and every later scenario would then run inside a live session.
            assertTrue(login.isStillOnLoginPage(),
                    "An invalid sign-in (" + credential.getScenario()
                            + ") navigated away from the login page to " + login.currentPath());

            String message = login.errorMessage();
            softly().assertTrue(login.isErrorDisplayed(),
                    "No error was shown for: " + credential.getScenario());
            softly().assertTrue(message.contains(credential.getExpectedError()),
                    credential.getScenario() + " - expected the message to contain '"
                            + credential.getExpectedError() + "' but it read '" + message + "'");

            // The message must not reveal which field was wrong, or it lets an
            // attacker enumerate valid tenants and accounts.
            String lower = message.toLowerCase();
            softly().assertFalse(lower.contains("user not found") || lower.contains("wrong password"),
                    credential.getScenario() + " - the message discloses which credential "
                            + "was incorrect: " + message);
        }

        assertAllSoft();
        verified("All " + scenarios.size() + " invalid credential combinations were rejected");
    }

    @Test(priority = 3,
            groups = {"smoke", "auth", "critical"},
            dataProvider = "validCredentials",
            dataProviderClass = TestDataProviders.class,
            description = "TC_AUTH_03 - Each persona signs in with its company code, owns its "
                    + "dashboard session and is offered account self-service")
    public void TC_AUTH_03_eachPersonaSignsInOwnsSessionAndGetsAccountSelfService(
            Persona persona, Credential credential) {

        step("Signing in as " + persona.roleName() + " (" + credential.getEmail() + ")");
        this.activePersona = persona;
        LoginPage login = openLoginPage();
        login.fillAndSubmit(credential);

        DashboardPage dashboard = new DashboardPage();
        assertTrue(dashboard.isLoaded(),
                "Sign-in as " + persona + " did not reach the dashboard. Landed on: "
                        + dashboard.currentPath());

        step("Verifying the session belongs to the persona that signed in");
        softly().assertEquals(dashboard.currentPath(), AppModule.DASHBOARD.route(),
                "Sign-in did not land on /dashboard");
        softly().assertEquals(dashboard.signedInUserName(), persona.displayName(),
                "The dashboard greeting names a different user than the one who signed in");
        softly().assertFalse(login.isErrorDisplayed(),
                "An error banner appeared even though sign-in succeeded");

        step("Verifying the avatar menu offers account self-service");
        var items = dashboard.header().avatarMenuItems();
        note("Menu items offered: " + items);
        for (String action : List.of("Change Password", "Update Photo", "Logout")) {
            softly().assertTrue(items.stream().anyMatch(item -> item.contains(action)),
                    action + " is missing from the avatar menu for " + persona);
        }

        assertAllSoft();
        verified(persona.roleName() + " signed in, owns the session and has account self-service");
    }

    @Test(priority = 4,
            groups = {"smoke", "auth", "security", "critical"},
            description = "TC_AUTH_04 - Protected routes are unreachable without a session, "
                    + "both before sign-in and after logout")
    public void TC_AUTH_04_protectedRoutesAreUnreachableBeforeSignInAndAfterLogout() {
        // Every check here is a security boundary, so all are hard assertions.
        step("Deep-linking to the Employee Directory with no session at all");
        BasePage.navigateTo(AppModule.EMPLOYEE_DIRECTORY);
        LoginPage login = new LoginPage();
        assertTrue(login.isLoaded(),
                "An unauthenticated deep link to " + AppModule.EMPLOYEE_DIRECTORY.route()
                        + " was not redirected to the login page");
        assertFalse(login.displaysTextImmediately(AppModule.EMPLOYEE_DIRECTORY.landingMarker()),
                "Employee Directory content rendered before the redirect completed");

        step("Signing in as the Organization Administrator, then logging out");
        DashboardPage dashboard = loginAs(Persona.ADMIN);
        dashboard.header().logout();
        assertTrue(login.isLoaded(), "Logout did not return the user to the login page");

        // The session must be dead server-side, not merely forgotten by the
        // client. A deep link is the cheapest way to prove it.
        step("Deep-linking straight back into the app after logout");
        BasePage.navigateTo(AppModule.EMPLOYEE_DIRECTORY);
        assertTrue(login.isLoaded(),
                "The session survived logout - deep-linking to "
                        + AppModule.EMPLOYEE_DIRECTORY.route()
                        + " reached the app instead of the login page");

        verified("Protected routes are unreachable without a session, before and after logout");
    }
}
