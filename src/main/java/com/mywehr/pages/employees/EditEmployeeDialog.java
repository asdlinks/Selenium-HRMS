package com.mywehr.pages.employees;

import com.mywehr.driver.DriverManager;
import com.mywehr.utils.Log;
import com.mywehr.utils.MuiUtils;
import com.mywehr.utils.WaitUtils;
import org.openqa.selenium.JavascriptExecutor;

import java.time.Duration;
import java.util.List;

/**
 * The "Edit Employee Details" dialog, used here read-only: it is the one
 * screen that shows back every value the onboarding wizard stored, split
 * across tabs (Basic Details, Identity Information, Banking Information,
 * Education, Employment History, References).
 *
 * Values are read as a flat snapshot - every input's value, every select's
 * displayed text, and the tab's visible text - rather than field by field.
 * The edit form labels some fields differently from the wizard ("Postal
 * Code" vs "PIN Code", "State / Province" vs "State / Union Territory"), so
 * asking "is this stored value shown on the tab" is the robust question.
 */
public class EditEmployeeDialog {

    private static final String TITLE = "Edit Employee Details";

    public boolean isOpen() {
        return MuiUtils.isDialogOpen() && MuiUtils.dialogText().contains(TITLE);
    }

    public EditEmployeeDialog openTab(String tabName) {
        Log.step("Edit dialog: opening the '" + tabName + "' tab");
        MuiUtils.clickDialogButtonViaScript(tabName);
        WaitUtils.sleepQuietly(Duration.ofMillis(800));
        return this;
    }

    /**
     * Everything the current tab shows: input/textarea values (including
     * masked ones - a password-type input still holds the real value),
     * select captions, and the rendered text of list entries.
     */
    @SuppressWarnings("unchecked")
    public List<String> snapshot() {
        return (List<String>) ((JavascriptExecutor) DriverManager.get()).executeScript(
                """
                const dialog = document.querySelector('.MuiDialog-root');
                if (!dialog) return [];
                const values = [...dialog.querySelectorAll('input, textarea')]
                    .filter(e => e.type !== 'file' && e.type !== 'checkbox'
                              && e.getAttribute('aria-hidden') !== 'true')
                    .map(e => e.value);
                const selects = [...dialog.querySelectorAll('[role=combobox]')]
                    .map(e => e.value || e.textContent);
                return values.concat(selects, [dialog.innerText])
                    .map(v => (v || '').trim()).filter(v => v.length > 0);
                """);
    }

    /** True when the current tab shows the value anywhere. */
    public boolean shows(String expected) {
        String needle = expected.trim().toLowerCase();
        return snapshot().stream().anyMatch(v -> v.toLowerCase().contains(needle));
    }

    /**
     * Closes the dialog without saving.
     *
     * The footer differs per tab - Basic Details has Cancel / Update Record,
     * the list tabs (Education, References ...) do not offer Cancel - so
     * whichever dismiss button is present is used, then Escape.
     */
    public void cancel() {
        if (MuiUtils.isDialogOpen()) {
            org.openqa.selenium.WebElement dialog = com.mywehr.utils.ElementUtils.find(
                    MuiUtils.DIALOG, com.mywehr.enums.WaitStrategy.VISIBLE);
            dialog.findElements(org.openqa.selenium.By.xpath(".//button")).stream()
                    .filter(b -> b.getText().trim().matches("(?i)cancel|close"))
                    .findFirst()
                    .ifPresent(com.mywehr.utils.ElementUtils::clickViaScript);
            WaitUtils.waitForInvisibleQuietly(MuiUtils.DIALOG, Duration.ofSeconds(5));
        }
        MuiUtils.dismissOverlays();
    }
}
