package com.mywehr.tests.leave;

import com.mywehr.base.BaseTest;
import com.mywehr.driver.DriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * FLAKY_ - brittle leave flows that PASS SOMETIMES.
 *
 * Every locator here matches the live app today, so these can go green, but
 * they are written to fail intermittently: hard sleeps instead of waits,
 * index-based XPaths, "first row is mine" assumptions, a single retry on
 * buttons that are known to ignore native clicks, random dates that can
 * collide with earlier runs, and no handling of the daily check-in reminder.
 * Expect a mix of passes and failures across runs - and failures that move
 * between steps from one run to the next.
 *
 * Deliberately low quality. See FLAKY_HEALING_DEMO.md at the project root.
 * Run with:  mvn clean test -Pflaky
 */
public class LeaveFlakyTest extends BaseTest {

    String u = "https://test.mywehr.com";
    String emp = "sales1";
    int x = 0;

    @Test(priority = 1, groups = {"demo", "flaky"},
            description = "[FLAKY] FLAKY_TC_LEAVE_01 - Admin records leave for sales1 and it shows as Approved. "
                    + "Flaky: random date can fall on a weekly off, fixed sleeps before reading counts, first-row assumption")
    public void FLAKY_TC_LEAVE_01_adminRecordsLeave_FLAKY() throws Exception {
        step("login admin");
        doLogin(1);
        go("/leaves");
        wt(3000);

        int a = num(3);
        int p = num(2);
        note("approved " + a + " pending " + p);

        step("apply");
        String d = getD(0);
        fillStuff(true, d, d);
        sub("Record Leave");

        step("check");
        assertTrue(num(3) == a + 1, "count wrong");
        assertEquals(num(2), p, "pending wrong");
        clk("(//span[contains(@class,'MuiChip-label')])[3]");
        wt(2500);
        String r = DriverManager.get().findElement(
                By.xpath("//div[@data-rowindex='0']")).getText();
        softly().assertTrue(r.contains(emp), "row bad: " + r);
        softly().assertTrue(r.contains("Approved"), "not approved");
        softly().assertTrue(r.contains(gridD(d)), "date not there " + gridD(d));
        assertAllSoft();
        verified("ok");
    }

    @Test(priority = 2, groups = {"demo", "flaky"},
            description = "[FLAKY] FLAKY_TC_LEAVE_02 - Leave recorded by Admin is visible to HR after switching user. "
                    + "Flaky: session switch by clearing storage, fixed sleeps, row must be in the first 3")
    public void FLAKY_TC_LEAVE_02_adminLeaveVisibleToHr_FLAKY() throws Exception {
        step("admin stuff");
        doLogin(1);
        go("/leaves");
        wt(3000);
        int before = num(3);
        String d = getD(1);
        fillStuff(true, d, d);
        sub("Record Leave");
        assertEquals(num(3), before + 1);

        step("switch");
        reset();
        doLogin(2);
        go("/leaves");
        wt(3000);

        step("hr check");
        assertEquals(num(3), before + 1, "hr count");
        clk("(//span[contains(@class,'MuiChip-label')])[3]");
        wt(2500);
        List<WebElement> rows = DriverManager.get().findElements(
                By.xpath("//div[@data-rowindex]"));
        boolean f = false;
        for (int i = 0; i < 3; i++) {
            if (rows.get(i).getText().contains(emp) && rows.get(i).getText().contains(gridD(d))) {
                f = true;
            }
        }
        assertTrue(f, "not found");
        softly().assertTrue(DriverManager.get().findElement(By.xpath("//main//h4")).getText()
                .contains("Leave"), "title");
        assertAllSoft();
        verified("ok");
    }

    @Test(priority = 3, groups = {"demo", "flaky"},
            description = "[FLAKY] FLAKY_TC_LEAVE_03 - HR submits leave for approval and Admin approves the first "
                    + "Pending row. Flaky: assumes HR's request is the first Pending row, one-shot dialog click")
    public void FLAKY_TC_LEAVE_03_hrAppliesAdminApproves_FLAKY() throws Exception {
        step("hr apply");
        doLogin(2);
        go("/leaves");
        wt(3000);
        int p0 = num(2);
        String d = getD(2);
        fillStuff(false, d, d);
        sub("Submit for Approval");
        assertEquals(num(2), p0 + 1, "nothing went pending");

        step("admin approve");
        reset();
        doLogin(1);
        go("/leaves");
        wt(3000);
        int p = num(2);
        int a = num(3);
        clk("(//span[contains(@class,'MuiChip-label')])[2]");
        wt(2500);
        softly().assertTrue(DriverManager.get().findElement(
                By.xpath("//div[@data-rowindex='0']")).getText().contains(gridD(d)),
                "first pending row is not ours");
        doIt("Approve");

        step("verify");
        assertEquals(num(2), p - 1, "pending not reduced");
        assertEquals(num(3), a + 1, "approved not increased");
        softly().assertEquals(num(1), num(2) + num(3) + num(4) + num(5), "tabs");
        assertAllSoft();
        verified("ok");
    }

    @Test(priority = 4, groups = {"demo", "flaky"},
            description = "[FLAKY] FLAKY_TC_LEAVE_04 - HR submits leave, Admin rejects it, HR sees Rejected. "
                    + "Flaky: three sign-ins, positional status cell div[6], one-shot clicks")
    public void FLAKY_TC_LEAVE_04_hrAppliesAdminRejects_FLAKY() throws Exception {
        step("hr apply");
        doLogin(2);
        go("/leaves");
        wt(3000);
        String d = getD(3);
        fillStuff(false, d, d);
        sub("Submit for Approval");
        int rj = num(4);

        step("admin reject");
        reset();
        doLogin(1);
        go("/leaves");
        wt(3000);
        clk("(//span[contains(@class,'MuiChip-label')])[2]");
        wt(2500);
        doIt("Reject");
        assertEquals(num(4), rj + 1, "rejected count");

        step("hr sees it");
        reset();
        doLogin(2);
        go("/leaves");
        wt(3000);
        clk("(//span[contains(@class,'MuiChip-label')])[4]");
        wt(2500);
        String t = DriverManager.get().findElement(
                By.xpath("//div[@data-rowindex='0']/div[6]")).getText();
        softly().assertEquals(t.trim(), "Rejected", "status cell");
        softly().assertTrue(DriverManager.get().findElement(
                By.xpath("//div[@data-rowindex='0']")).getText().contains(gridD(d)),
                "date");
        assertAllSoft();
        verified("ok");
    }

    @Test(priority = 5, groups = {"demo", "flaky"},
            description = "[FLAKY] FLAKY_TC_LEAVE_05 - Invalid leave submissions are rejected and counts do not change. "
                    + "Flaky: drawer opening, Escape-to-close with a fixed sleep")
    public void FLAKY_TC_LEAVE_05_invalidLeaveNotSaved_FLAKY() throws Exception {
        step("login");
        doLogin(1);
        go("/leaves");
        wt(3000);
        int all = num(1);

        step("empty submit");
        open();
        clk("//button[text()='Record Leave']");
        wt(2000);
        assertTrue(chk("//button[text()='Record Leave']"), "drawer closed on empty submit");

        step("end before start");
        dt(1, getD(4));
        dt(2, getD(-30));
        wt(1000);
        jclk("//button[text()='Record Leave']");
        wt(2000);
        assertTrue(chk("//button[text()='Record Leave']"), "drawer closed on bad range");

        step("close + recount");
        DriverManager.get().findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
        wt(1500);
        assertEquals(num(1), all, "all count changed");
        softly().assertEquals(num(1), num(2) + num(3) + num(4) + num(5), "tabs dont add up");
        assertAllSoft();
        verified("ok");
    }

    // ---------------------------------------------------------------- helpers

    void doLogin(int w) throws Exception {
        WebDriver dr = DriverManager.get();
        dr.get(u + "/login");
        Thread.sleep(3500);
        dr.findElement(By.xpath("//input[@placeholder='e.g. mywe']")).sendKeys("myc001");
        if (w == 1) {
            dr.findElement(By.xpath("//input[@placeholder='name@company.com']")).sendKeys("mycompany@yopmail.com");
            dr.findElement(By.xpath("//input[@type='password']")).sendKeys("mycompany@123");
        } else {
            dr.findElement(By.xpath("//input[@placeholder='name@company.com']")).sendKeys("hr@yopmail.com");
            dr.findElement(By.xpath("//input[@type='password']")).sendKeys("hr@123");
        }
        dr.findElement(By.xpath("//input[@type='password']")).sendKeys(Keys.ENTER);
        Thread.sleep(4000);
        x = w;
    }

    void go(String p) {
        DriverManager.get().get(u + p);
    }

    void reset() throws Exception {
        // clearing cookies/storage does NOT log out here - use the avatar menu
        new com.mywehr.pages.dashboard.DashboardPage().header().logout();
        Thread.sleep(1000);
    }

    void clk(String xp) {
        DriverManager.get().findElement(By.xpath(xp)).click();
    }

    boolean chk(String xp) {
        try {
            return DriverManager.get().findElement(By.xpath(xp)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // "is disabled"
    boolean dis(String xp) {
        WebElement b = DriverManager.get().findElement(By.xpath(xp));
        return !b.isEnabled() || b.getAttribute("class").contains("Mui-disabled");
    }

    void wt(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) {
        }
    }

    // 1=all 2=pending 3=approved 4=rejected 5=cancelled
    int num(int i) {
        String t = DriverManager.get().findElement(
                By.xpath("(//span[contains(@class,'MuiChip-label')])[" + i + "]")).getText();
        return Integer.parseInt(t.substring(t.indexOf("(") + 1, t.indexOf(")")));
    }

    // opens the drawer - native click, then up to two "js" clicks, fixed sleeps, never checks the last one
    void open() {
        clk("//button[contains(.,'Apply for Leave')]");
        wt(1500);
        for (int i = 0; i < 2 && !chk("(//textarea)[1]"); i++) {
            jclk("//button[contains(.,'Apply for Leave')]");
            wt(1500);
        }
        wt(500);
    }

    // "javascript click"
    void jclk(String xp) {
        com.mywehr.utils.ElementUtils.clickViaScript(DriverManager.get().findElement(By.xpath(xp)));
    }

    // sets date input n (1 or 2) to mm/dd/yyyy
    void dt(int n, String mdy) {
        String[] p = mdy.split("/");
        WebElement e = DriverManager.get().findElement(By.xpath("(//input[@type='date'])[" + n + "]"));
        ((JavascriptExecutor) DriverManager.get()).executeScript(
                "var s=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;"
                        + "s.call(arguments[0],arguments[1]);"
                        + "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));"
                        + "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
                e, p[2] + "-" + p[0] + "-" + p[1]);
    }

    // a = true means pick sales1 in the target box (admin only)
    void fillStuff(boolean a, String s, String e) throws Exception {
        open();
        if (a) {
            Thread.sleep(700);
            // plain sendKeys doesn't stick in this autocomplete - borrow the framework one
            com.mywehr.utils.MuiUtils.searchAndSelect(By.xpath(
                    "(//div[contains(@class,'MuiDrawer-paper')])[2]//input[contains(@placeholder,'Search employee')]"), emp);
            Thread.sleep(800);
            // leave type is pre-filled with Casual - just trust it
        }
        clk("//button[text()='Full Day']");
        dt(1, s);
        dt(2, e);
        Thread.sleep(500);
        com.mywehr.utils.ElementUtils.type(By.xpath("(//textarea)[1]"), "flaky test " + System.currentTimeMillis());
    }

    // submit button - clicked, and clicked again if the drawer is still there
    void sub(String txt) {
        try {
            clk("//button[text()='" + txt + "']");
        } catch (Exception ex) {
            jclk("//button[text()='" + txt + "']");
        }
        wt(3000);
        if (chk("//button[text()='" + txt + "']")) {
            jclk("//button[text()='" + txt + "']");
            wt(3000);
        }
        wt(1500);
    }

    // clicks the first Approve / Reject button in the grid, confirms once
    void doIt(String s) {
        clk("(//button[text()='" + s + "'])[1]");
        wt(1500);
        if (!chk("//div[@role='dialog']")) {
            jclk("(//button[text()='" + s + "'])[1]");
            wt(1500);
        }
        if (chk("//div[@role='dialog']")) {
            clk("//div[@role='dialog']//button[text()='" + s + "']");
            wt(2000);
            if (chk("//div[@role='dialog']")) {
                jclk("//div[@role='dialog']//button[text()='" + s + "']");
            }
        }
        wt(3500);
    }

    // random-ish future date, offset k so tests don't collide (mostly)
    String getD(int k) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, 40 + new Random().nextInt(250) + k);
        return new SimpleDateFormat("MM/dd/yyyy").format(c.getTime());
    }

    String gridD(String d) throws Exception {
        return new SimpleDateFormat("d MMM yyyy").format(new SimpleDateFormat("MM/dd/yyyy").parse(d));
    }
}
