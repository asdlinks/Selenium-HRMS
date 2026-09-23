package com.mywehr.pages.components;

import com.mywehr.driver.DriverManager;
import com.mywehr.utils.ElementUtils;
import com.mywehr.utils.Log;
import com.mywehr.utils.MuiUtils;
import com.mywehr.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * The contextual left rail that appears inside a module.
 *
 * Its contents change with the section - Employees shows Department /
 * Organization Structure / My Team, Time &amp; Leave shows Leaves / Shifts /
 * Kiosk Devices and so on - and entries are filtered by permission, which
 * makes this component a second RBAC surface alongside route guards.
 */
public class SideNavComponent {

    private static final By SIDE_NAV_LINKS =
            By.xpath("//main/preceding-sibling::*//a[@href] | //aside//a[@href] | //nav//a[@href]");
    private static final By COLLAPSE_BUTTON =
            By.cssSelector("button[aria-label='Collapse'], button[title='Collapse']");

    public boolean isDisplayed() {
        return !ElementUtils.findAll(SIDE_NAV_LINKS).isEmpty();
    }

    public List<String> itemLabels() {
        return ElementUtils.findAll(SIDE_NAV_LINKS).stream()
                .map(WebElement::getText)
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .distinct()
                .toList();
    }

    public List<String> itemRoutes() {
        return ElementUtils.findAll(SIDE_NAV_LINKS).stream()
                .map(element -> element.getDomAttribute("href"))
                .filter(href -> href != null && href.startsWith("/"))
                .distinct()
                .toList();
    }

    public boolean hasItem(String label) {
        return itemLabels().stream().anyMatch(item -> item.equalsIgnoreCase(label));
    }

    public boolean linksToRoute(String route) {
        return itemRoutes().contains(route);
    }

    public void clickItem(String label) {
        Log.step("Side navigation -> " + label);
        By link = By.xpath("//a[@href][normalize-space(.)="
                + MuiUtils.escapeForXPath(label) + "]");
        String urlBefore = DriverManager.get().getCurrentUrl();
        ElementUtils.click(link);

        // A native click can be absorbed without effect; the URL not changing
        // is the observable sign, so replay the full pointer sequence once.
        if (!urlChangesFrom(urlBefore)) {
            Log.warn("The rail link '" + label + "' did not navigate on the native click - "
                    + "replaying the full pointer sequence");
            ElementUtils.clickViaScript(link);
            urlChangesFrom(urlBefore);
        }
        WaitUtils.waitForDataToSettle();
    }

    private boolean urlChangesFrom(String urlBefore) {
        try {
            WaitUtils.fluently(d -> !d.getCurrentUrl().equals(urlBefore),
                    Duration.ofSeconds(6), "URL did not change");
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }

    public void collapse() {
        if (ElementUtils.isDisplayed(COLLAPSE_BUTTON)) {
            ElementUtils.click(COLLAPSE_BUTTON);
        }
    }
}
