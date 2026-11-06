package com.mywehr.pages.base;

import com.mywehr.enums.WaitStrategy;
import com.mywehr.utils.ElementUtils;
import com.mywehr.utils.MuiUtils;
import com.mywehr.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * A MUI modal dialog (role="dialog"), identified by its title.
 *
 * Holds what every dialog shares - "is it open", its text, its inline
 * validation messages and a way to dismiss it - so each concrete dialog only
 * adds the fields and actions that are specific to it.
 */
public abstract class BaseDialog extends BaseComponent {

    /** Text in the dialog's header that identifies this dialog. */
    protected abstract String title();

    @Override
    public boolean isDisplayed() {
        return MuiUtils.isDialogOpen() && MuiUtils.dialogText().contains(title());
    }

    /** Alias of {@link #isDisplayed()} that reads naturally for a dialog. */
    public boolean isOpen() {
        return isDisplayed();
    }

    /** Visible text of the dialog. Excludes the values typed into inputs. */
    public String text() {
        return MuiUtils.dialogText();
    }

    /** Inline field errors currently shown, de-duplicated, in screen order. */
    public List<String> validationMessages() {
        return ElementUtils.findAll(MuiUtils.FIELD_ERROR).stream()
                .map(WebElement::getText).map(String::trim)
                .filter(t -> !t.isEmpty()).distinct().toList();
    }

    /**
     * Closes the dialog without saving: Cancel or Close, whichever the
     * current footer offers, then Escape and overlay clean-up as a fallback.
     */
    public void cancel() {
        if (MuiUtils.isDialogOpen()) {
            WebElement dialog = ElementUtils.find(MuiUtils.DIALOG, WaitStrategy.VISIBLE);
            dialog.findElements(By.xpath(".//button")).stream()
                    .filter(b -> b.getText().trim().matches("(?i)cancel|close"))
                    .findFirst()
                    .ifPresent(ElementUtils::clickViaScript);
            WaitUtils.waitForInvisibleQuietly(MuiUtils.DIALOG, Duration.ofSeconds(5));
        }
        MuiUtils.dismissOverlays();
    }
}
