package com.mywehr.pages.documents;

import com.mywehr.enums.AppModule;
import com.mywehr.pages.base.BasePage;
import com.mywehr.utils.ElementUtils;
import com.mywehr.utils.Log;
import com.mywehr.utils.MuiUtils;
import org.openqa.selenium.By;

import java.util.List;

/**
 * Company Documents - a MUI DataGrid of published documents with Active and
 * Archived tabs, plus the upload dialog.
 *
 * Both personas can read and upload here, which makes it a useful control in
 * the RBAC suite: it proves the suite distinguishes "HR is restricted" from
 * "HR is restricted everywhere".
 */
public class CompanyDocumentsPage extends BasePage {

    private static final By UPLOAD_BUTTON = MuiUtils.buttonByText("Upload Document");
    private static final By ACTIVE_TAB = MuiUtils.buttonByText("Active");
    private static final By ARCHIVED_TAB = MuiUtils.buttonByText("Archived");
    private static final By CATEGORY_FILTER = MuiUtils.selectByLabel("Category");
    private static final By GRID_SEARCH = By.cssSelector("input[placeholder*='Search']");

    private static final List<String> EXPECTED_COLUMNS = List.of(
            "Title", "Category", "Version", "Effective Date",
            "Expiry Date", "Uploaded By", "Size");

    @Override
    public String landingMarker() {
        return AppModule.COMPANY_DOCUMENTS.landingMarker();
    }

    @Override
    public String route() {
        return AppModule.COMPANY_DOCUMENTS.route();
    }

    // -------------------------------------------------------------- reading

    public boolean canUpload() {
        return ElementUtils.isDisplayed(UPLOAD_BUTTON);
    }

    public boolean hasActiveAndArchivedTabs() {
        return ElementUtils.isDisplayed(ACTIVE_TAB) && ElementUtils.isDisplayed(ARCHIVED_TAB);
    }

    public boolean hasCategoryFilter() {
        return ElementUtils.isDisplayed(CATEGORY_FILTER);
    }

    public boolean isGridDisplayed() {
        return ElementUtils.isDisplayed(MuiUtils.DATA_GRID);
    }

    /** True when every expected column header is rendered. */
    public boolean hasAllExpectedColumns() {
        String content = contentText();
        return EXPECTED_COLUMNS.stream().allMatch(content::contains);
    }

    public List<String> expectedColumns() {
        return EXPECTED_COLUMNS;
    }

    public List<String> missingColumns() {
        String content = contentText();
        return EXPECTED_COLUMNS.stream().filter(column -> !content.contains(column)).toList();
    }

    public int documentCount() {
        MuiUtils.waitForGrid();
        return MuiUtils.gridTotalFromPagination();
    }

    public boolean hasDocument(String title) {
        return MuiUtils.gridHasRowContaining(title);
    }

    // -------------------------------------------------------------- actions

    public CompanyDocumentsPage openArchivedTab() {
        Log.step("Switching to the Archived documents tab");
        ElementUtils.click(ARCHIVED_TAB);
        waitForSettled();
        return this;
    }

    public CompanyDocumentsPage openActiveTab() {
        ElementUtils.click(ACTIVE_TAB);
        waitForSettled();
        return this;
    }

    public CompanyDocumentsPage searchDocuments(String query) {
        ElementUtils.type(GRID_SEARCH, query);
        waitForSettled();
        return this;
    }

    /** Opens the upload dialog without submitting - upload needs a real file. */
    public CompanyDocumentsPage openUploadDialog() {
        Log.step("Opening the Upload Document dialog");
        MuiUtils.openDialogFrom(UPLOAD_BUTTON);
        return this;
    }

    public String uploadDialogTitle() {
        return ElementUtils.getText(MuiUtils.DIALOG_TITLE);
    }

    /** Field labels the upload dialog is expected to present. */
    public boolean uploadDialogHasAllFields() {
        String text = MuiUtils.dialogText();
        return text.contains("Title")
                && text.contains("Category")
                && text.contains("Effective Date")
                && text.contains("Share with All Employees");
    }

    public CompanyDocumentsPage closeUploadDialog() {
        MuiUtils.clickDialogButtonAndWaitForClose("Cancel");
        return this;
    }
}
