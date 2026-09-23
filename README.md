# MyWe HRMS — Selenium Automation Framework

End-to-end smoke and regression coverage for the MyWe HRMS web application
(`https://test.mywehr.com`), built as a hybrid **Page Object + Data Driven**
framework on Selenium 4 and TestNG 7.

---

## Quick start

```bash
mvn clean test                      # full smoke suite, Chrome, headed
mvn clean test -Pci                 # headless, 2 retries
mvn clean test -Prbac               # only the access-control suite
mvn clean test -Pintermodule        # only the cross-module reflection suite
mvn clean test -Pregression         # everything
mvn clean test -Pcritical-path      # only tests tagged "critical"
mvn clean test -Pauth               # one module: auth | navigation | configuration | employee | leave
mvn clean test -Dbrowser=firefox -Dheadless=true
```

Every suite lives in `src/test/resources/suites/` and can also be run from the
IDE (right-click the XML → Run). Module suites select their test class, not
individual methods, so renaming or merging tests never drops them from a
suite. After any run, `target/surefire-reports/testng-failed.xml` reruns just
the failures.

Reports land in `test-output/reports/`, failure screenshots in
`test-output/screenshots/`.

**Requirements:** JDK 17+, Maven 3.8+, Chrome/Firefox/Edge installed. Driver
binaries are resolved automatically by Selenium Manager — nothing to download
or keep in sync.

---

## ⚠️ One constraint you must know before running

**The application enforces a single active session per user account.**

Signing in as `mycompany@yopmail.com` anywhere invalidates every other session
for that account — including a suite that is mid-run. In practice:

- **Do not browse the app as Admin or HR while the suite is running.** Your
  browser tab will silently log the suite out, and tests will fail several
  steps later on a login page with errors that look nothing like the cause.
- **Never run two tests as the same persona concurrently.** This is the one
  real limit on parallelism here. The framework is otherwise parallel-ready —
  ThreadLocal driver, no shared state — but the *accounts* are the bottleneck,
  not the code. Every suite, `regression.xml` included, therefore ships with
  `thread-count="1"`; raise it only once each thread has its own login.
- **Give CI its own accounts.** Two personas is enough for the coverage, but
  not for concurrency. To parallelise properly, create one Admin and one HR
  account per thread and select them by thread index in `Persona`.

This was discovered the hard way during framework development; it is the single
most likely cause of an inexplicable red run.

---

## Test coverage

**25 test methods across 7 classes, about 30 executions per run.** Related
checks are combined into one test per page or flow, so the suite signs in far
less often than one-test-per-check would (the previous layout needed 105
executions).

### Hard and soft assertions

- **Hard** (`Assert`) for checks everything after them depends on: sign-in, a
  page/dialog/drawer being loaded, a record actually being saved, and every
  security boundary (a denied route, a hidden settings panel, a session after
  logout). The test stops there.
- **Soft** (`softly()`) for independent observations — cards, columns,
  controls, per-module loads, cross-module counts — so one run lists every
  one that is wrong. Each soft failure is logged in the HTML report with its
  own screenshot at the moment it happened.
- Every test that uses soft checks ends with `assertAllSoft()`. A safety net
  in `BaseTest` fails any test that recorded soft failures but forgot to call
  it.

| ID | Area | What it proves |
|----|------|----------------|
| `TC_AUTH_01` | Auth | Login page renders all credential fields, branding, masked password |
| `TC_AUTH_02` | Auth | Every invalid credential combination is rejected generically *(5 datasets, one test)* |
| `TC_AUTH_03` | Auth | Each persona signs in, owns its session, gets account self-service *(×2 personas)* |
| `TC_AUTH_04` | Auth | Protected routes are unreachable before sign-in and after logout |
| `TC_NAV_01` | Navigation | Header is complete and every permitted module loads cleanly *(×2 personas; 18 / 14 modules)* |
| `TC_NAV_02` | Navigation | Employees and Time & Leave rails link to every sub-module |
| `TC_RBAC_01` | **RBAC** | HR is denied every restricted route and org-configuration panel; permitted panels open |
| `TC_RBAC_02` | **RBAC** | Admin reaches every HR-denied route, sees all 12 panels, governs roles with live counts |
| `TC_RBAC_03` | **RBAC** | Both personas retain people-ops rights and employee row actions *(×2 personas)* |
| `TC_CFG_01` | Configuration | Organization Structure reference data + Company Profile locale / financial year |
| `TC_CFG_02` | Configuration | Shifts, work modes, attendance policies, Daily Check-In and Holiday Calendar |
| `TC_CFG_03` | Configuration | Reports workspace aggregates figures consistent with the Directory |
| `TC_EMP_01` | Employee | Directory renders KPIs, filters, controls; search and department filter narrow results |
| `TC_EMP_02` | Employee | **E2E** register an employee; directory lists it and headcount rises |
| `TC_EMP_03` | Employee | Registration form rejects every invalid dataset and saves nothing *(5 datasets, one test)* |
| `TC_EMP_04` | Employee | **E2E** department create → delete, with count tracking and the empty-only rule |
| `TC_LEAVE_01` | Leave | Entitlement cards and grid render; status tab counts reconcile *(×2 personas)* |
| `TC_LEAVE_02` | Leave | **E2E cross-persona** Admin records leave → saved as Approved → HR sees it in a separate session |
| `TC_LEAVE_03` | Leave | Leave without mandatory dates is rejected; nothing is recorded |
| `TC_LEAVE_04` | Leave | Entitlement cards are scoped to the signed-in user, not the tenant |
| `TC_XMOD_01` | **Inter-module** | New department → Directory KPI + Directory filter options |
| `TC_XMOD_02` | **Inter-module** | New employee → Directory + Dashboard + their Department all agree |
| `TC_XMOD_03` | **Inter-module** | New holiday → Calendar + Dashboard next-holiday widget |
| `TC_XMOD_04` | **Inter-module** | Dashboard pending approvals and workforce agree with Leave, Directory and Department |
| `TC_XMOD_05` | **Inter-module** | Published documents and the upload right reach both personas |

---

## The two personas

| | Organization Administrator | HR Administrator |
|---|---|---|
| Company code | `myc001` | `myc001` |
| Email | `mycompany@yopmail.com` | `hr@yopmail.com` |
| Display name | Surag | hr |
| Settings panels | all 12 | 4 |

### Verified access matrix

Encoded in `AppModule` and `SettingsPanel`, so the navigation and RBAC suites
are generated from it — adding a module to the product means adding one enum
line, not writing two more tests.

**Routes denied to HR** (silently redirect to `/dashboard`):
`/work-modes`, `/attendance/policies`, `/attendance/kiosk-devices`,
`/attendance/face-enrollment`

**Settings panels hidden from HR:**
Company Profile, General Config, Attendance Rules, Work Modes, Menu Management,
Roles & Permissions, Salary Grades, Audit & Compliance

**Shared by both:** Dashboard, Employees, Department, Organization Structure,
My Team, Daily Check-In, Leaves, Leave Cancellation, Holiday Calendar, Shifts,
Company Documents, Reports, Payroll, and 4 settings panels.

---

## Project layout

```
src/main/java/com/mywehr/
├── config/ConfigManager           system property → env var → config.properties
├── driver/                        DriverFactory + ThreadLocal DriverManager
├── enums/
│   ├── AppModule                  route map + landing markers + access matrix
│   ├── SettingsPanel              rail labels, panel headings, visibility
│   ├── Persona, LeaveType, LeaveStatus, WaitStrategy
├── data/
│   ├── TestDataFactory            unique runtime data ("AT_" prefixed)
│   ├── model/                     EmployeeData, LeaveRequestData, Credential
│   └── reader/JsonDataReader      cached dataset loading
├── pages/                         page objects, one per screen
│   ├── base/BasePage              navigation, landing checks, KPI reader
│   ├── components/                HeaderComponent, SideNavComponent
│   └── auth|dashboard|employees|timeleave|documents|settings/
└── utils/
    ├── WaitUtils                  SPA-aware waits (bootstrap splash, skeletons)
    ├── ElementUtils               interaction + React-safe input handling
    ├── MuiUtils                   Material-UI component strategies
    └── DateUtils, ScreenshotUtils, Log

src/test/java/com/mywehr/
├── base/BaseTest                  fresh browser + login per test
├── listeners/                     TestListener, RetryAnalyzer, ExtentReportManager
├── dataproviders/TestDataProviders
└── tests/auth|navigation|rbac|configuration|employee|leave|intermodule/

src/test/resources/
├── config.properties
├── testdata/*.json
└── suites/*.xml
```

---

## Dataset management

Two kinds of dataset, split by who should be able to change them:

**File-based** — a tester extends these without touching Java:

| File | Feeds |
|---|---|
| `credentials.json` | persona logins |
| `invalid-logins.json` | 5 negative auth scenarios + expected message |
| `employee-validation.json` | 5 invalid employee payloads + expected message |
| `reference-data.json` | what the seeded tenant already contains |

**Generated at runtime** — `TestDataFactory`. The HRMS rejects duplicate
employee emails and IDs, so a committed fixture passes once and then fails
forever against the same tenant. Every created record carries a timestamp and a
per-JVM counter, and is prefixed `AT` so automation data is identifiable.

**Model-based** — the navigation and RBAC datasets are generated from
`AppModule` and `SettingsPanel`, so coverage cannot drift from the route map
the page objects navigate by.

---

## Notes on this application (earned the hard way)

Worth reading before extending the framework — each of these caused a real
failure during development.

**No `data-testid`, and MUI ids are generated per render** (`id="_r_f_"`).
Locators use, in order of preference: the `name` attribute where the app sets
one (login form, Register Employee dialog), the visible label resolved through
MUI's `FormControl` wrapper, then ARIA roles.

**MUI Selects open on `mousedown`, not `click`.** A scripted
`element.click()` does *nothing* — no error, no menu. `ElementUtils.clickViaScript`
therefore replays the whole pointer sequence
(`pointerdown → mousedown → pointerup → mouseup → click`). This one behaviour
was the hidden cause of several unrelated-looking failures.

**Native clicks are intermittently absorbed** while a view re-renders around
them — this affects dialogs, dropdowns, chip filters, settings-rail entries,
dialog submit/cancel buttons and the avatar menu. Every interaction with an
observable outcome (dialog opens, menu closes, grid filters, panel switches)
tries the native click first, then retries with the scripted sequence. The
native click stays first on purpose — it is the only one of the two that would
catch a genuinely unclickable control. When you add an interaction, give it a
post-condition rather than assuming the click landed.

**Icon-only controls carry their meaning in `aria-label`/`title`.** The
employee row actions (View Profile, Edit, Reset Password, Delete) have no text
at all — use `MuiUtils.controlByAccessibleName`, not `buttonByText`.

**The Employee Directory defaults to an org-chart view that renders one
department at a time** (HR on load), and the name search narrows within it. An
employee created into Sales is therefore genuinely off-screen even though the
record saved perfectly — which reads as "creation failed" and sends you hunting
for a bug that is not there. The KPI strip is the honest check that a record
exists; to *see* the person, switch department group or use the table view.

**KPI cards render a placeholder before their fetch resolves.** A baseline read
taken straight after navigation can capture that placeholder, which makes a
later "this count must not have changed" assertion fail against a number that
was never real. Baselines go through `readSettledKpiCard`.

**Some labels are lowercase in the DOM and uppercased by CSS.** The leave
balance card holds `casual`; the designation is `Brand head`, not `BRAND HEAD`.
Match the DOM, not the screenshot.

**There are no toasts anywhere.** Success is never announced — it is only
observable as changed state. Every mutation is verified against the resulting
list or counter.

**The app boots behind a `Loading Mywe HRMS…` splash**, and routing is
client-side, so the URL updates before the view has data. Page objects assert a
content *landing marker*, never just the URL.

**Rail and panel labels differ.** "Locations / Offices" opens "Locations
Management"; "Account Security" opens "Password Management". Verifying a panel
opened requires the heading, because the rail label is on screen regardless.

**"Audit & Compliance" is a link, not a panel** — it routes to `/reports/audit`.

---

## Design decisions

**A fresh browser and login per test.** Costs a few seconds each; buys
order-independence, no cascading failures through leftover session state, and
parallel-readiness. In an RBAC suite that constantly switches persona, a shared
session would be actively dangerous — a stale admin cookie would silently turn
an HR test green.

**Implicit wait is pinned at zero.** Mixing implicit and explicit waits makes
every negative assertion ("this element must NOT be there") pay the full
implicit timeout, and the RBAC suite is full of them.

**Retries are conservative.** One retry by default, and `AssertionError` is
never retried — only infrastructure-shaped failures (timeout, stale element,
dead session). A failed assertion did its job; re-running it only delays the
bad news.

**Page objects never assert.** They expose intent and return values or other
page objects. Assertions live in tests, so the same page object serves positive
and negative cases.

**Tests clean up after themselves.** Departments and holidays are removed in a
`finally` block; cleanup helpers never throw, so a cleanup problem cannot mask
the real failure a test already reported. Employees are left in place
deliberately — the app offers deactivation rather than deletion, and removing a
person is not something an unattended suite should do.

### Test data the suite creates

Everything it creates is prefixed **`AT`** so it is identifiable at a glance:
`AT Dept <stamp>`, `AT Holiday <stamp>`, `AT Tester <n>`, employee IDs
`AT_<stamp>_<n>`, emails `at.auto.<stamp>@yopmail.com`.

| Record | Cleaned up? |
|---|---|
| Departments | Yes, in `finally` |
| Holidays | Yes, in `finally` |
| Employees | **No** — deactivation only; remove manually if the tenant gets noisy |
| Leave requests | **No** — `TC_LEAVE_02` leaves one approved request per run |

A crashed run can leave an `AT` department or holiday behind; both are safe to
delete from their own module. If the tenant has accumulated `AT Tester`
employees from repeated runs, the headcount assertions still hold — they are
all relative (before/after), never absolute.

**The tenant's plan caps headcount (currently 10).** Every run of `TC_EMP_02`
and `TC_XMOD_02` consumes a seat. Once the cap is reached the directory shows
*"You've reached your subscription plan's employee limit"*, and both tests
**skip** with that message rather than fail — it is an environment condition,
not a defect. Delete a few `AT Tester` employees to free seats and re-run.

---

## Suites

| File | Contents | Profile |
|---|---|---|
| `smoke.xml` | everything, ordered auth → nav → RBAC → config → modules → inter-module | *(default)* |
| `critical-path.xml` | `critical` group only — the build gate | `-Dsuite.file=…` |
| `rbac.xml` | access matrix only | `-Prbac` |
| `intermodule.xml` | cross-module reflection only | `-Pintermodule` |
| `regression.xml` | everything (serial; parallel-capable — see the file) | `-Pregression` |

**Groups:** `smoke`, `critical`, `auth`, `navigation`, `rbac`, `security`,
`configuration`, `employee`, `leave`, `intermodule`, `e2e`, `negative`,
`validation`, `documents`, `reports`.

---

## Configuration

Every key in `config.properties` is overridable by `-Dkey=value` or an
env var (`BASE_URL`, `BROWSER`, …), so one committed file serves local runs,
the `ci` profile and a Grid run without ever being edited.

| Key | Default | Notes |
|---|---|---|
| `base.url` | `https://test.mywehr.com` | |
| `tenant.code` | `myc001` | |
| `browser` | `chrome` | `chrome`, `firefox`, `edge`, `remote-chrome`, `remote-firefox` |
| `headless` | `false` | |
| `explicit.wait` | `25` | seconds |
| `spa.settle.wait` | `20` | bootstrap splash timeout |
| `implicit.wait` | `0` | **leave at zero** — see Design decisions |
| `retry.count` | `1` | |
| `grid.url` | `http://localhost:4444/wd/hub` | used by `-Pgrid` |

---

## Extending it

**A new module:** add one line to `AppModule` with its route, landing marker
and permitted personas. The navigation and RBAC suites pick it up automatically.

**A new settings panel:** add one line to `SettingsPanel` with its rail label,
its *panel heading* and its personas.

**A new negative case:** add an object to the relevant JSON file. No Java.

**A new page:** extend `BasePage`, implement `landingMarker()` and `route()`,
and keep assertions out of it.
