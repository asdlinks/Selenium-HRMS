package com.mywehr.pages.timeleave;

import com.mywehr.enums.AppModule;
import com.mywehr.exceptions.FrameworkException;
import com.mywehr.pages.base.BasePage;
import com.mywehr.utils.ElementUtils;
import com.mywehr.utils.Log;
import com.mywehr.utils.MuiUtils;
import com.mywehr.utils.WaitUtils;
import org.openqa.selenium.By;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Leave Cancellation - self-service only.
 *
 * It lists the SIGNED-IN user's own upcoming leaves and nobody else's, so a
 * leave can only be cancelled by signing in as the person it belongs to. An
 * Administrator has no way to cancel an Approved leave recorded for someone
 * else: those rows carry no actions on Leave Management.
 */
public class LeaveCancellationPage extends BasePage {

    private static final By LEAVE_SELECT = MuiUtils.selectByLabel("Select Leave to Cancel");
    private static final By REASON = By.xpath(
            "//label[starts-with(normalize-space(.),'Reason for Cancellation')]"
                    + "/ancestor::div[contains(@class,'MuiFormControl-root')][1]"
                    + "//*[self::input or self::textarea][not(@aria-hidden='true')]");
    private static final By CANCEL_LEAVE_BUTTON = MuiUtils.buttonByText("Cancel Leave");

    @Override
    public String landingMarker() {
        return AppModule.LEAVE_CANCELLATION.landingMarker();
    }

    @Override
    public String route() {
        return AppModule.LEAVE_CANCELLATION.route();
    }

    /** The leaves the signed-in user can still cancel, as the dropdown words them. */
    public List<String> cancellableLeaves() {
        return MuiUtils.readOptions(LEAVE_SELECT);
    }

    /**
     * Cancels the upcoming leave whose dropdown option contains every fragment.
     *
     * @return the option that was cancelled
     * @throws FrameworkException when no option matches
     */
    public String cancelLeaveMatching(String reason, String... fragments) {
        List<String> options = cancellableLeaves();
        String target = options.stream()
                .filter(option -> Arrays.stream(fragments).allMatch(option::contains))
                .findFirst()
                .orElseThrow(() -> new FrameworkException("No upcoming leave matches "
                        + Arrays.toString(fragments) + ". Cancellable: " + options));

        Log.step("Cancelling the leave '" + target + "'");
        // selectOption matches on normalize-space(), so collapse line breaks too.
        MuiUtils.selectOption(LEAVE_SELECT, target.replaceAll("\\s+", " ").trim());
        ElementUtils.type(REASON, reason);
        ElementUtils.click(CANCEL_LEAVE_BUTTON);

        // The app may confirm first, the same way Approve / Reject can.
        if (WaitUtils.isVisibleWithin(MuiUtils.DIALOG, Duration.ofSeconds(3))) {
            MuiUtils.clickDialogButtonAndWaitForClose("Cancel Leave");
        }
        WaitUtils.waitForDataToSettle();
        return target;
    }

    /** True once no upcoming leave matches the fragments any more. */
    public boolean noLongerCancellable(String... fragments) {
        open();
        return cancellableLeaves().stream()
                .noneMatch(option -> Arrays.stream(fragments).allMatch(option::contains));
    }
}
