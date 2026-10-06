package com.mywehr.tests.leave;

import com.mywehr.base.BaseTest;
import com.mywehr.driver.DriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.*;

/**
 * BROKEN - ALWAYS FAILS. Worst-quality code on purpose.
 *
 * UNHEALABLE_*   the failure is NOT a stale locator: the feature, page or
 *                message does not exist in the app, or the test script itself
 *                crashes. A healer has nothing correct to heal towards and
 *                should report "could not heal" (or must not pick some random
 *                element and call it a heal).
 * NORMALFAIL_*   every locator is correct and every step works; the test
 *                fails on an assertion because its expectation is wrong
 *                about the app's behaviour/data. This is an ordinary test
 *                failure - healing must NOT change anything here.
 *
 * See FLAKY_HEALING_DEMO.md at the project root.
 * Run with:  mvn clean test -Pbroken
 */
public class LeaveBrokenTest extends BaseTest {

    public static WebDriver d;
    public static String s1, s2, s3;
    public static int i1, i2;
    public static Map<String, String> m;
    public static List<WebElement> l;

    @Test(priority = 1, groups = {"demo", "unhealable"},
            description = "[UNHEALABLE] UNHEALABLE_TC_LEAVE_01 - Admin bulk-approves every pending leave. "
                    + "Fails: the app has no 'Bulk Approve All' button - there is no element to heal to")
    public void UNHEALABLE_TC_LEAVE_01_bulkApproveAllPending_featureMissing_UNHEALABLE() throws Throwable {
        try {
            d = DriverManager.get();
            d.get("https://test.mywehr.com/login");
            Thread.sleep(2500);
            d.findElement(By.name("tenantCode")).sendKeys("myc001");
            d.findElement(By.name("email")).sendKeys("mycompany@yopmail.com");
            d.findElement(By.name("password")).sendKeys("mycompany@123");
            d.findElement(By.xpath("//button[@type='submit']")).click();
            Thread.sleep(5000);
            d.get("https://test.mywehr.com/leaves");
            Thread.sleep(5000);
            s1 = d.findElement(By.xpath("(//span[contains(@class,'MuiChip-label')])[2]")).getText();
            i1 = Integer.parseInt(s1.replaceAll("\\D", ""));
            System.out.println("pending=" + i1);
            d.findElement(By.xpath("(//span[contains(@class,'MuiChip-label')])[2]")).click();
            Thread.sleep(3000);
            d.findElement(By.xpath("//button[normalize-space(.)='Bulk Approve All']")).click();
            Thread.sleep(3000);
            d.findElement(By.xpath("//div[@role='dialog']//button[normalize-space(.)='Yes, approve " + i1 + "']")).click();
            Thread.sleep(5000);
            s1 = d.findElement(By.xpath("(//span[contains(@class,'MuiChip-label')])[2]")).getText();
            Assert.assertEquals(s1, "Pending (0)");
        } catch (Throwable t) {
            System.out.println("ERROR!!!! " + t.getMessage());
            throw t;
        }
    }

    @Test(priority = 2, groups = {"demo", "unhealable"},
            description = "[UNHEALABLE] UNHEALABLE_TC_LEAVE_02 - Admin opens the Leave Approvals v2 page. "
                    + "Fails: the route /leaves/approvals-v2 does not exist, so the page and its heading never render")
    public void UNHEALABLE_TC_LEAVE_02_leaveApprovalsV2Page_routeMissing_UNHEALABLE() throws Throwable {
        d = DriverManager.get();
        d.get("https://test.mywehr.com/login");
        Thread.sleep(2500);
        d.findElement(By.name("tenantCode")).sendKeys("myc001");
        d.findElement(By.name("email")).sendKeys("mycompany@yopmail.com");
        d.findElement(By.name("password")).sendKeys("mycompany@123");
        d.findElement(By.xpath("//button[@type='submit']")).click();
        Thread.sleep(5000);
        d.get("https://test.mywehr.com/leaves/approvals-v2");
        Thread.sleep(5000);
        WebElement h = new WebDriverWait(d, Duration.ofSeconds(15)).until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h4[normalize-space(.)='Leave Approvals']")));
        Assert.assertTrue(h.isDisplayed());
        l = d.findElements(By.xpath("//table[@id='approvals-table']//tr"));
        Assert.assertTrue(l.size() > 0, "no rows");
    }

    @Test(priority = 3, groups = {"demo", "unhealable"},
            description = "[UNHEALABLE] UNHEALABLE_TC_LEAVE_03 - Leave tab totals add up. "
                    + "Fails: the TEST SCRIPT crashes (NullPointerException on a map that is never created; "
                    + "past that, a NumberFormatException parsing 'All (n)') - "
                    + "every locator is fine, so there is nothing to heal")
    public void UNHEALABLE_TC_LEAVE_03_tabTotals_scriptBug_UNHEALABLE() throws Throwable {
        d = DriverManager.get();
        d.get("https://test.mywehr.com/login");
        Thread.sleep(2500);
        d.findElement(By.name("tenantCode")).sendKeys("myc001");
        d.findElement(By.name("email")).sendKeys("mycompany@yopmail.com");
        d.findElement(By.name("password")).sendKeys("mycompany@123");
        d.findElement(By.xpath("//button[@type='submit']")).click();
        Thread.sleep(5000);
        d.get("https://test.mywehr.com/leaves");
        Thread.sleep(5000);
        l = d.findElements(By.xpath("//span[contains(@class,'MuiChip-label')]"));
        int t = 0;
        for (int i = 1; i < l.size(); i++) {
            m.put("tab" + i, l.get(i).getText());
            t = t + Integer.valueOf(l.get(i).getText().split(" ")[1]);
        }
        i2 = Integer.parseInt(l.get(0).getText());
        Assert.assertEquals(t, i2);
    }

    @Test(priority = 4, groups = {"demo", "unhealable"},
            description = "[UNHEALABLE] UNHEALABLE_TC_LEAVE_04 - HR submits leave and sees a 'Leave applied "
                    + "successfully' toast. Fails: the app shows no toast at all after submitting - nothing to heal to")
    public void UNHEALABLE_TC_LEAVE_04_successToastAfterSubmit_messageNeverShown_UNHEALABLE() throws Throwable {
        d = DriverManager.get();
        d.get("https://test.mywehr.com/login");
        Thread.sleep(2500);
        d.findElement(By.name("tenantCode")).sendKeys("myc001");
        d.findElement(By.name("email")).sendKeys("hr@yopmail.com");
        d.findElement(By.name("password")).sendKeys("hr@123");
        d.findElement(By.xpath("//button[@type='submit']")).click();
        Thread.sleep(5000);
        d.get("https://test.mywehr.com/leaves");
        Thread.sleep(5000);
        for (int k = 0; k < 3 && d.findElements(By.xpath("//textarea")).isEmpty(); k++) {
            d.findElement(By.xpath("//button[contains(.,'Apply for Leave')]")).click();
            Thread.sleep(2000);
        }
        // submit an EMPTY form - no dates - and still expect a success toast
        d.findElement(By.xpath("//button[contains(.,'Submit for Approval')]")).click();
        WebElement toast = new WebDriverWait(d, Duration.ofSeconds(10)).until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'Toastify__toast--success')][contains(.,'Leave applied successfully')]")));
        Assert.assertTrue(toast.isDisplayed());
    }

    @Test(priority = 5, groups = {"demo", "normal-fail"},
            description = "[NORMAL-FAIL] NORMALFAIL_TC_LEAVE_05 - Leave the Admin records for sales1 should wait in "
                    + "Pending. Fails on the ASSERTION: the app auto-approves admin-recorded leave (Approved +1, "
                    + "Pending unchanged). All locators are correct - do not heal")
    public void NORMALFAIL_TC_LEAVE_05_adminRecordedLeaveExpectedPending_wrongExpectation_ASSERTION() throws Throwable {
        d = DriverManager.get();
        d.get("https://test.mywehr.com/login");
        Thread.sleep(2500);
        d.findElement(By.name("tenantCode")).sendKeys("myc001");
        d.findElement(By.name("email")).sendKeys("mycompany@yopmail.com");
        d.findElement(By.name("password")).sendKeys("mycompany@123");
        d.findElement(By.xpath("//button[@type='submit']")).click();
        Thread.sleep(5000);
        d.get("https://test.mywehr.com/leaves");
        Thread.sleep(5000);
        s1 = d.findElement(By.xpath("//span[contains(@class,'MuiChip-label')][starts-with(.,'Pending')]")).getText();
        i1 = Integer.parseInt(s1.replaceAll("\\D", ""));
        com.mywehr.data.model.LeaveRequestData r = com.mywehr.data.TestDataFactory.singleDayLeave("sales1");
        for (int k = 0; k < 3 && d.findElements(By.xpath("//textarea")).isEmpty(); k++) {
            com.mywehr.utils.ElementUtils.clickViaScript(By.xpath("//button[contains(.,'Apply for Leave')]"));
            Thread.sleep(2000);
        }
        Thread.sleep(700);
        new com.mywehr.pages.timeleave.ApplyLeaveDrawer().fill(r).submitExpectingSuccess();
        Thread.sleep(4000);
        s2 = d.findElement(By.xpath("//span[contains(@class,'MuiChip-label')][starts-with(.,'Pending')]")).getText();
        i2 = Integer.parseInt(s2.replaceAll("\\D", ""));
        System.out.println(i1 + " -> " + i2);
        Assert.assertEquals(i2, i1 + 1, "admin-recorded leave should go to Pending for approval");
    }

    @Test(priority = 6, groups = {"demo", "normal-fail"},
            description = "[NORMAL-FAIL] NORMALFAIL_TC_LEAVE_06 - Admin's Casual leave card shows a 20-day "
                    + "entitlement. Fails on the ASSERTION: the tenant policy is 15 days (card reads n/15). "
                    + "All locators are correct - do not heal")
    public void NORMALFAIL_TC_LEAVE_06_casualEntitlementIs20_staleTestData_ASSERTION() throws Throwable {
        d = DriverManager.get();
        d.get("https://test.mywehr.com/login");
        Thread.sleep(2500);
        d.findElement(By.name("tenantCode")).sendKeys("myc001");
        d.findElement(By.name("email")).sendKeys("mycompany@yopmail.com");
        d.findElement(By.name("password")).sendKeys("mycompany@123");
        d.findElement(By.xpath("//button[@type='submit']")).click();
        Thread.sleep(5000);
        d.get("https://test.mywehr.com/leaves");
        Thread.sleep(5000);
        i1 = new com.mywehr.pages.timeleave.LeavesPage().totalEntitlement(com.mywehr.enums.LeaveType.CASUAL);
        System.out.println("casual total: " + i1);
        Assert.assertEquals(i1, 20, "casual entitlement");
    }
}
