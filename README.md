# MyWe HRMS — Page Object Model Reference Framework

A small, working Selenium 4 + TestNG framework for the MyWe HRMS web app
(`https://test.mywehr.com`), kept deliberately compact so it can serve as a
**reference for a clean Page Object Model (POM) structure**.

It contains a handful of real, passing tests chosen to show each pattern once:
UI rendering, data-driven negatives, security boundaries, role-based access,
and full create → verify → delete journeys that clean up after themselves.

---

## Quick start

```bash
mvn clean test                  # the full suite (smoke.xml), Chrome, headed
mvn clean test -Pci             # headless, 2 retries
mvn clean test -Pauth           # one module: auth | rbac | employee | leave
mvn clean test -Dbrowser=firefox -Dheadless=true
```

**Requirements:** JDK 17+, Maven 3.8+, Chrome / Firefox / Edge. Driver binaries
are resolved by Selenium Manager.

**Report:** `test-output/reports/Run_<timestamp>/HRMS_Automation_Report_<timestamp>.html`
(open it from that folder so the screenshots next to it load).

> ⚠️ The app allows **one active session per account**. Don't browse the app as
> Admin or HR while the suite runs — it silently logs the suite out.

---

## The POM rules this framework follows

| # | Rule | Where to see it |
|---|------|-----------------|
| 1 | **Locators are private to the page.** Tests never see a `By`, a `WebElement` or the driver. | Every class under `pages/` |
| 2 | **Methods express intent**, not mechanics: `deleteEmployee(name)`, `approveRequestFor(name)`. | `EmployeeDirectoryPage`, `LeavesPage` |
| 3 | **Actions return the next page object.** Staying on the page returns `this`; landing somewhere else returns that page or component. | `LoginPage.loginAs()` → `DashboardPage`, `openEditFor()` → `EditEmployeeDialog`, `header().logout()` → `LoginPage` |
| 4 | **Queries return plain values** (`boolean`, `int`, `String`) for the test to assert. | `totalEmployees()`, `hasEmployeeNamed()` |
| 5 | **Pages never assert.** Assertions live only in tests. | No `org.testng` import anywhere in `src/main` |
| 6 | **Opening a page returns it, typed.** | `new EmployeeDirectoryPage().open()`, `loginAndOpen(Persona.ADMIN, new LeavesPage())` |
| 7 | **Shared behaviour lives in base classes**: pages extend `BasePage`, overlays extend `BaseComponent` / `BaseDialog`. | `pages/base/` |
| 8 | **Low-level mechanics live in utilities**, used by pages only: waits, MUI widgets, scripted clicks, network recording. | `utils/` |
| 9 | **Test data lives outside the code**: JSON fixtures for realistic records, a factory for unique names. | `testdata/`, `TestDataFactory` |

### A test reads like a user story

```java
EmployeeDirectoryPage directory = loginAndOpen(Persona.ADMIN, new EmployeeDirectoryPage());

EmployeeOnboardingWizard wizard = directory.openOnboardingWizard();
wizard.fillBasicDetails(basic).next();
wizard.fillContactDetails(contact).next();
wizard.next().next().next();                     // optional steps skipped
String outcome = wizard.create();

assertTrue(directory.hasEmployeeNamed(name));
EditEmployeeDialog edit = directory.openEditFor(name);
softly().assertTrue(edit.openTab("Basic Details").shows(basic.email()));
edit.cancel();
directory.deleteEmployee(name);
```

No locator, no wait, no `driver` — only intent and assertions.

> `open()` is a generic method, so assign its result to a typed variable
> (`EmployeeDirectoryPage d = new EmployeeDirectoryPage().open();`) rather than
> chaining straight off it.

---

## Project layout

```
src/main/java/com/mywehr/
├── config/        ConfigManager            properties + -D / env overrides
├── driver/        DriverFactory, DriverManager (ThreadLocal driver)
├── enums/         AppModule (routes + access matrix), Persona, LeaveType, ...
├── exceptions/    FrameworkException
├── data/
│   ├── model/     Credential, OnboardingEmployee, LeaveRequestData
│   ├── reader/    JsonDataReader           cached JSON loading
│   └── TestDataFactory                     unique names for created records
├── pages/
│   ├── base/      BasePage                 route, open(), isLoaded(), header(), sideNav(), KPI cards
│   │              BaseComponent            anything without a route (header, drawer, dialog)
│   │              BaseDialog               MUI dialogs: isOpen(), text(), validationMessages(), cancel()
│   │              GenericModulePage        load-only pages addressed by AppModule
│   ├── components/ HeaderComponent, SideNavComponent
│   ├── auth/      LoginPage
│   ├── dashboard/ DashboardPage
│   ├── employees/ EmployeeDirectoryPage, DepartmentPage, EmployeeProfilePage,
│   │              EmployeeOnboardingWizard, EditEmployeeDialog
│   ├── timeleave/ LeavesPage, ApplyLeaveDrawer
│   └── settings/  SettingsPage
└── utils/         ElementUtils, WaitUtils, MuiUtils, NetworkRecorder, DateUtils, ScreenshotUtils, Log

src/test/java/com/mywehr/
├── base/          BaseTest (browser per test, login helpers, soft asserts, report steps)
├── dataproviders/ TestDataProviders
├── listeners/     TestListener, ExtentReportManager, RetryAnalyzer, RetryListener
└── tests/         auth/ rbac/ employee/ leave/

src/test/resources/
├── config.properties
├── suites/        smoke.xml (everything) + one suite per module
└── testdata/      credentials, invalid-logins, employee-onboarding(-quick)
```

### Class hierarchy

```
BasePage (route, open, isLoaded)          BaseComponent (no route)
 ├── LoginPage                              ├── HeaderComponent
 ├── DashboardPage                          ├── SideNavComponent
 ├── EmployeeDirectoryPage                  ├── ApplyLeaveDrawer
 ├── EmployeeProfilePage                    └── BaseDialog (title, isOpen, cancel)
 ├── DepartmentPage                              ├── EditEmployeeDialog
 ├── LeavesPage                                  └── EmployeeOnboardingWizard
 ├── SettingsPage
 └── GenericModulePage
```

---

## The tests

| ID | Test | Shows |
|----|------|-------|
| TC_AUTH_01 | Login page renders every field and masks the password | Soft assertions over one screen |
| TC_AUTH_02 | Every invalid credential combination is rejected | Data-driven loop over `invalid-logins.json` |
| TC_AUTH_03 | Each persona signs in and owns its session | `@DataProvider` per persona |
| TC_AUTH_04 | Protected routes are unreachable signed-out and after logout | Security checks as hard assertions |
| TC_RBAC_01 | HR is redirected away from every restricted route and settings panel | Access matrix in an enum |
| TC_RBAC_02 | Both personas keep their shared people-operations rights | Positive pairing for the denials |
| TC_EMP_01 | Directory renders; search and filter narrow results | Fluent page actions |
| TC_EMP_02 | A department is created and deleted, with the count tracking both | Cleanup in `finally` |
| TC_EMP_03 | Full 6-step onboarding, every stored value read back, then deleted | Wizard + dialog components, JSON fixture |
| TC_EMP_04 | Quick onboarding (basic + contact only), verified, then deleted | Reusing the same page objects for a shorter flow |
| TC_LEAVE_01 | Leave Management renders and its status counts reconcile | Per-persona data provider |
| TC_LEAVE_02 | A leave request without dates is rejected and nothing is saved | Negative path through a drawer component |

**Hard vs soft assertions.** Hard asserts guard what the rest of the test depends
on (signed in, page loaded, record saved) and security boundaries. Soft asserts
(`softly()` … `assertAllSoft()`) cover independent observations so one run
reports every problem on a screen.

**Known app defect.** The onboarding wizard rejects every 6-digit PIN code.
TC_EMP_03 records this as a **WARNING** in the report and continues without a PIN;
TC_EMP_04 leaves the PIN blank.

---

## Adding a page — checklist

1. Create `XPage extends BasePage` in the right `pages/<module>/` package.
2. Implement `route()` and `landingMarker()` (text that proves the page rendered).
3. Declare locators as `private static final By`.
4. Write intent-level methods. Return `this`, the next page, or a plain value.
5. Overlays get their own class extending `BaseComponent` / `BaseDialog`,
   returned by the page method that opens them.
6. In the test: `XPage page = loginAndOpen(Persona.ADMIN, new XPage());` and assert.

---

## Configuration

Every key in `config.properties` can be overridden with `-Dkey=value` or an env var.

| Key | Default | Notes |
|---|---|---|
| `base.url` | `https://test.mywehr.com` | |
| `tenant.code` | `myc001` | |
| `browser` | `chrome` | `chrome`, `firefox`, `edge`, `remote-chrome`, `remote-firefox` |
| `headless` | `false` | |
| `explicit.wait` | `25` | seconds |
| `implicit.wait` | `0` | keep at zero — explicit waits only |
| `retry.count` | `1` | |
| `grid.url` | `http://localhost:4444/wd/hub` | used by `-Pgrid` |
