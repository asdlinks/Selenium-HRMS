package com.mywehr.pages.components;

import com.mywehr.utils.ElementUtils;
import com.mywehr.utils.Log;
import com.mywehr.utils.MuiUtils;
import com.mywehr.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * The persistent application header: primary nav, global employee search,
 * theme toggle, notifications and the avatar menu.
 *
 * The avatar button carries no accessible name, so it is located positionally
 * as the last button in the header bar. That is documented here rather than
 * buried in a test, because it is the one locator in the framework that would
 * break if a button were appended to the header.
 */
public class HeaderComponent {

    private static final By HEADER = By.cssSelector("header, .MuiAppBar-root");
    private static final By NAV_LINKS = By.cssSelector("header a[href], .MuiAppBar-root a[href]");
    private static final By GLOBAL_SEARCH = By.cssSelector("input[placeholder*='Search employees']");
    private static final By THEME_TOGGLE =
            By.cssSelector("button[aria-label*='light mode'], button[aria-label*='dark mode']");
    private static final By AVATAR_BUTTON =
            By.xpath("(//header//button | //*[contains(@class,'MuiAppBar-root')]//button)[last()]");

    private static final By MENU_UPDATE_PHOTO =
            By.xpath("//*[@role='menuitem'][contains(.,'Update Photo')]");
    private static final By MENU_CHANGE_PASSWORD =
            By.xpath("//*[@role='menuitem'][contains(.,'Change Password')]");
    private static final By MENU_LOGOUT =
            By.xpath("//*[@role='menuitem'][contains(.,'Logout')]");

    // ------------------------------------------------------------ presence

    public boolean isDisplayed() {
        return ElementUtils.isDisplayed(HEADER);
    }

    public boolean hasGlobalSearch() {
        return ElementUtils.isDisplayed(GLOBAL_SEARCH);
    }

    public boolean hasThemeToggle() {
        return ElementUtils.isDisplayed(THEME_TOGGLE);
    }

    /** Primary nav labels: Dashboard, Time & Leave, Employees, Documents, Settings. */
    public List<String> navigationLabels() {
        return ElementUtils.findAll(NAV_LINKS).stream()
                .map(WebElement::getText)
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .distinct()
                .toList();
    }

    public List<String> navigationHrefs() {
        return ElementUtils.findAll(NAV_LINKS).stream()
                .map(element -> element.getDomAttribute("href"))
                .filter(href -> href != null && !href.isBlank())
                .distinct()
                .toList();
    }

    public boolean hasNavigationItem(String label) {
        return navigationLabels().stream().anyMatch(item -> item.equalsIgnoreCase(label));
    }

    public void clickNavigationItem(String label) {
        Log.step("Header navigation -> " + label);
        ElementUtils.click(By.xpath("//header//a[normalize-space(.)="
                + MuiUtils.escapeForXPath(label) + "]"));
        WaitUtils.waitForDataToSettle();
    }

    // ------------------------------------------------------- global search

    public void searchEmployee(String query) {
        Log.step("Global search for '" + query + "'");
        ElementUtils.type(GLOBAL_SEARCH, query);
        WaitUtils.sleepQuietly(Duration.ofMillis(800));
    }

    // -------------------------------------------------------- avatar menu

    /**
     * Opens the account menu.
     *
     * Two hazards, both seen in practice:
     *  - an open dialog or drawer swallows the click, so overlays are cleared
     *    first;
     *  - the avatar is an icon button in a header that re-renders as
     *    notifications resolve, so a native click is sometimes absorbed. The
     *    menu appearing is the post-condition, and the scripted pointer
     *    sequence is the retry.
     */
    public void openAvatarMenu() {
        MuiUtils.dismissOverlays();

        ElementUtils.click(AVATAR_BUTTON);
        if (WaitUtils.isVisibleWithin(MuiUtils.MENU_ITEM, Duration.ofSeconds(5))) {
            return;
        }

        Log.warn("The account menu did not open on the native click - "
                + "replaying the full pointer sequence");
        ElementUtils.clickViaScript(AVATAR_BUTTON);
        WaitUtils.waitForVisible(MuiUtils.MENU_ITEM);
    }

    public List<String> avatarMenuItems() {
        openAvatarMenu();
        List<String> items = ElementUtils.findAll(MuiUtils.MENU_ITEM).stream()
                .map(WebElement::getText)
                .map(String::trim)
                .toList();
        ElementUtils.pressEscape();
        return items;
    }

    public boolean hasChangePasswordOption() {
        return ElementUtils.isDisplayed(MENU_CHANGE_PASSWORD);
    }

    public boolean hasUpdatePhotoOption() {
        return ElementUtils.isDisplayed(MENU_UPDATE_PHOTO);
    }

    /**
     * Signs out and waits for the login page.
     *
     * Logout clears the auth cookie server-side and the SPA replaces the shell,
     * so waiting for the login form - not just a URL change - is what makes
     * this reliable.
     */
    public void logout() {
        Log.step("Logging out");
        By companyCodeField = By.cssSelector("input[name='tenantCode']");

        openAvatarMenu();
        ElementUtils.click(MENU_LOGOUT);

        if (WaitUtils.isVisibleWithin(companyCodeField, Duration.ofSeconds(10))) {
            return;
        }

        // The menu item click can be absorbed like every other click in this
        // app. Reopening and replaying the pointer sequence is cheap; ending up
        // still signed in is not - the next persona's sign-in would silently
        // run against the previous session.
        Log.warn("Sign-out did not take on the native click - retrying");
        if (!ElementUtils.isDisplayed(MuiUtils.MENU_ITEM)) {
            openAvatarMenu();
        }
        ElementUtils.clickViaScript(MENU_LOGOUT);
        WaitUtils.waitForVisible(companyCodeField);
    }
}
