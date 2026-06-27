# Appium Mobile Test Automation Framework

**Android test automation | Appium · Java · TestNG · Allure**

---

## What This Project Is

An end-to-end mobile test automation framework built from scratch for the **SauceLabs Mobile Sample Android app**. It covers 15 test scenarios across Login, Inventory, Cart, Checkout, and Navigation — producing rich Allure reports with step-level tracing, failure screenshots, and severity classification.

The framework is designed to reflect real-world QA engineering standards: resilient to emulator instabilities (ANR dialogs, app crashes), fully data-driven with zero hardcoded test values, and organised into clean, independently testable layers.

---

## Technology Stack

| Category | Tool | Version |
|---|---|---|
| Mobile Automation | Appium Java Client | 9.1.0 |
| WebDriver Protocol | Selenium WebDriver | 4.18.1 (pinned) |
| Android Driver | UiAutomator2 | via Appium 2.0 |
| Test Framework | TestNG | 7.10.2 |
| Reporting | Allure TestNG | 2.27.0 |
| Aspect Weaving | AspectJ (compile-time) | 1.9.22 |
| Test Data | Jackson Databind | 2.17.1 |
| Build Tool | Maven | 3.x |
| Language | Java | 17 |
| Logging | SLF4J + Logback | 2.0.13 / 1.5.6 |
| Emulator | Android Emulator (API 34) | Android 14 |

---

## Architecture

The framework is layered into four tiers, each with a single responsibility. No layer reaches past its immediate neighbour.

```
┌──────────────────────────────────────────────────────────────────┐
│                        TEST LAYER                                │
│   LoginTest · InventoryTest · CartTest · CheckoutTest · NavTest  │
│   Business-readable assertions · @Allure annotations · Groups    │
├──────────────────────────────────────────────────────────────────┤
│                        PAGE LAYER                                │
│   LoginPage · InventoryPage · CartPage · CheckoutInfoPage        │
│   CheckoutOverviewPage · CheckoutCompletePage · SidebarPage      │
│   Screen interactions · locators · waitForPageLoad()             │
├──────────────────────────────────────────────────────────────────┤
│                       UTILITY LAYER                              │
│   WebDriverWaitUtils · GestureUtils · CommonUtils · JsonDataReader│
│   ConfigReader · AllureEnvironmentListener                       │
│   Explicit waits · W3C gestures · screenshots · JSON parsing     │
├──────────────────────────────────────────────────────────────────┤
│                       DRIVER LAYER                               │
│   DriverManager (ThreadLocal Singleton AndroidDriver)            │
│   Session lifecycle · ANR dismissal · UiAutomator2Options        │
└──────────────────────────────────────────────────────────────────┘
```

### Data Flow

```
 config.properties         users.json / checkout.json
        │                           │
        ▼                           ▼
  ConfigReader               JsonDataReader
        │                           │
        └──────────────┬────────────┘
                       ▼
              DriverManager  ──────────►  AndroidDriver (Appium session)
                       │
                       ▼
              Page Objects  ──────────►  BasePage helpers
                       │                      │
                       │                      ▼
                       │              WebDriverWaitUtils (explicit waits)
                       │              GestureUtils (W3C Actions API)
                       ▼
              Test Classes  ──────────►  Allure Reports + Screenshots
```

---

## Design Patterns

### Singleton — DriverManager

One `AndroidDriver` instance per thread. Created once before the suite, reused across all 15 tests, quit only after the last test. Avoids the 10–20 second Appium session startup cost on every test.

```java
// ThreadLocal keeps parallel execution safe — each thread owns its own driver
private static final ThreadLocal<AndroidDriver> driverThread = new ThreadLocal<>();

public static void initDriver() {
    if (driverThread.get() != null) return;  // guard: already initialised
    // build UiAutomator2Options → create driver → store in ThreadLocal
}
```

### Page Object Model — without Page Factory

Every screen has a dedicated class owning all locators and interactions. **Page Factory is intentionally not used** — it caches element references at init time, causing `StaleElementReferenceException` when app state changes. Every method finds elements live.

```java
// Locators are By constants — resolved fresh on each interaction
private final By LOGIN_BUTTON = byAccessibilityId("test-LOGIN");

public InventoryPage tapLogin() {
    click(LOGIN_BUTTON);       // finds element fresh, waits for clickable, clicks
    return new InventoryPage(driver);
}
```

### Template Method — BasePage

`BasePage` defines the contract (`waitForPageLoad()` is abstract) and provides shared helpers (`click`, `type`, `getText`, `isDisplayed`). Every page extends it and fills in the details.

### Fluent Interface — Method Chaining

Page methods return `this` or the next page, so tests read as natural language:

```java
loginPage
    .enterUsername(USERS.getText("validUser", "username"))
    .enterPassword(USERS.getText("validUser", "password"))
    .tapLogin();
```

### Factory Method — Page Navigation

Navigation is handled by factory-style methods — the caller never instantiates the next page directly:

```java
public CheckoutInfoPage proceedToCheckout() {
    gestureUtils.swipeToElement(CHECKOUT_BUTTON, 3);
    click(CHECKOUT_BUTTON);
    CheckoutInfoPage page = new CheckoutInfoPage(driver);
    page.waitForPageLoad();   // blocks until next screen is ready
    return page;
}
```

### Data-Driven — JSON + JsonDataReader

All test data lives in JSON files. Tests reference data by key path. No hardcoded strings in test logic.

```java
String username = USERS.getText("validUser", "username");
String item1    = CHECKOUT.getText("products", "item1");
String errMsg   = USERS.getText("errorMessages", "lockedOut");
```

---

## Folder Structure

```
appium-java-project/
│
├── pom.xml                          Maven build, dependencies, plugins
├── testng.xml                       Full suite (15 tests)
├── testng-smoke.xml                 Smoke suite (5 tests)
├── testng-regression.xml            Regression suite (10 tests)
├── .gitignore
│
└── src/
    ├── main/java/com/meghana/appium/
    │   │
    │   ├── driver/
    │   │   └── DriverManager.java        Singleton driver + ANR dismissal
    │   │
    │   ├── pages/
    │   │   ├── BasePage.java             Abstract base — helpers + ANR-safe wait
    │   │   ├── LoginPage.java
    │   │   ├── InventoryPage.java
    │   │   ├── CartPage.java
    │   │   ├── CheckoutInfoPage.java
    │   │   ├── CheckoutOverviewPage.java
    │   │   ├── CheckoutCompletePage.java
    │   │   └── SidebarPage.java
    │   │
    │   └── utils/
    │       ├── WebDriverWaitUtils.java   All explicit wait wrappers
    │       ├── GestureUtils.java         W3C Actions API gestures
    │       ├── CommonUtils.java          Screenshot, price parse, sort check
    │       ├── JsonDataReader.java       Jackson JSON test data reader
    │       └── ConfigReader.java         Singleton config.properties reader
    │
    └── test/
        ├── java/com/meghana/appium/
        │   ├── base/
        │   │   └── BaseTest.java         Lifecycle, state recovery, teardown
        │   ├── listeners/
        │   │   └── AllureEnvironmentListener.java  Writes environment.properties
        │   └── tests/
        │       ├── LoginTest.java        TC1, TC2, TC3
        │       ├── InventoryTest.java    TC4, TC5, TC6, TC7, TC8, TC9
        │       ├── CartTest.java         TC10, TC11
        │       ├── CheckoutTest.java     TC12, TC13, TC14
        │       └── NavigationTest.java   TC15
        │
        └── resources/
            ├── config.properties
            ├── allure.properties
            ├── categories.json           Allure failure classification
            ├── logback-test.xml
            └── testdata/
                ├── users.json
                └── checkout.json
```

---

## End-to-End Flow

### From `mvn clean test` to Allure Report

```
mvn clean test
│
├─ AspectJ CTW weaves @Step annotations into page method bytecode at compile time
│
├─ Surefire reads testng.xml → discovers 5 test classes, 15 methods
│
├─ AllureEnvironmentListener.onStart()
│   ├─ Writes device/platform info to target/allure-results/environment.properties
│   └─ Copies categories.json to target/allure-results/
│
├─ For each test method (in XML-defined order):
│   │
│   ├─ @BeforeMethod setUp()
│   │   ├─ isDriverAlive()          → probe session; if dead, reinitialise
│   │   ├─ ensureAppInForeground()  → detect home-screen crash, relaunch app
│   │   ├─ dismissSystemDialogs()   → clear ANR "Wait" dialog if present
│   │   └─ ensureLoginScreen()      → navigate/logout back to login from any state
│   │
│   ├─ Test method executes
│   │   ├─ Reads data from JSON (zero hardcoded strings)
│   │   ├─ Calls page methods (recorded as @Step in Allure)
│   │   ├─ Asserts with descriptive messages
│   │   └─ On FAIL → screenshot captured and attached to Allure report
│   │
│   └─ @AfterMethod tearDown()
│       └─ Driver kept alive (not quit between tests — reused for speed)
│
├─ @AfterSuite tearDownSuite()
│   └─ DriverManager.quitDriver() — single session end point
│
└─ target/allure-results/ populated → mvn allure:serve opens report in browser
```

### Single Test Deep Dive — TC14 Complete Checkout Journey

```
CheckoutTest.testCompleteCheckoutJourney()
│
├─ loginPage.login(username, password)
│   ├─ type(USERNAME_FIELD)           waitForVisible → clear → sendKeys
│   ├─ type(PASSWORD_FIELD)
│   ├─ click(LOGIN_BUTTON)            waitForClickable → click
│   └─ new InventoryPage()            waitForPageLoad()
│
├─ inventoryPage.addProductToCart("Sauce Labs Backpack")
│   ├─ GestureUtils.swipeToElement()  scrolls DOWN until ADD TO CART visible
│   └─ click(addBtn)
│
├─ inventoryPage.goToCart()
│   └─ click(CART_ICON) → new CartPage() → waitForPageLoad()
│
├─ cartPage.proceedToCheckout()
│   ├─ GestureUtils.swipeToElement(CHECKOUT_BUTTON)
│   └─ new CheckoutInfoPage() → waitForPageLoad()
│
├─ infoPage.fillAndContinue(firstName, lastName, postalCode)
│   └─ new CheckoutOverviewPage() → waitForPageLoad()
│
├─ overviewPage.isTotalCorrect()      subtotal + tax == total (rounded, IEEE-754 safe)
│
├─ overviewPage.tapFinish()
│   ├─ GestureUtils.swipeToElement(FINISH_BUTTON)
│   └─ new CheckoutCompletePage() → waitForPageLoad()
│
└─ Assert.assertEquals(completePage.getSuccessHeader(),
                       CHECKOUT.getText("completionPage", "header"))
```

---

## Appium Setup

### Appium Server

Appium 2.0 runs as a local HTTP server. The framework connects to it at `http://127.0.0.1:4723`.

```bash
# Install Appium 2.0
npm install -g appium

# Install the UiAutomator2 driver for Android
appium driver install uiautomator2

# Start the server
appium

# Verify UiAutomator2 driver is installed
appium driver list --installed
```

Appium receives W3C WebDriver protocol commands from the Java client and translates them into ADB calls and UiAutomator2 instrumentation commands on the Android device.

### Appium Inspector

Appium Inspector is a standalone GUI tool for inspecting element hierarchies on a live device or emulator. It was used throughout this project to identify and verify locators before writing them into page classes.

**Workflow:**
1. Start the Appium server (`appium`)
2. Launch Appium Inspector (desktop app)
3. Enter the same capabilities from `config.properties`
4. Click **Start Session** — the device screen mirrors in Inspector
5. Click any element to see its `content-desc`, `resource-id`, `class`, and `text`
6. Copy the `content-desc` value → use as `byAccessibilityId("test-*")` in the page class

**Why `content-desc` (accessibility ID)?**  
The SauceLabs app uses consistent `test-*` content-description attributes on all interactive elements. These are stable across app versions, semantic, and faster to resolve than XPath.

### Android Platform

```
Mac (host)
│
├─ Android SDK + Platform Tools
│   └─ adb — Android Debug Bridge (device communication layer)
│
├─ Android Emulator
│   └─ Pixel 6a — API 34 (Android 14) — arm64-v8a
│       ├─ com.swaglabsmobileapp (app under test)
│       ├─ io.appium.settings (Appium helper APK)
│       └─ io.appium.uiautomator2.server (instrumentation server)
│
└─ Appium Server (localhost:4723)
    └─ UiAutomator2 Driver
        └─ Translates W3C commands → ADB → UiAutomator2 instrumentation
```

**Emulator start command used in this project:**

```bash
emulator -avd Pixel_6a_Edited_API_34 \
  -no-snapshot-load \
  -gpu host \
  -no-boot-anim \
  -no-audio \
  -memory 1536
```

---

## Gestures — W3C Actions API

All touch gestures use the W3C Actions API (`PointerInput` + `Sequence`). The deprecated Appium `TouchAction` was removed in Java Client 8+.

```java
// Scroll DOWN by 40% of screen height
PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
Sequence scroll = new Sequence(finger, 0)
    .addAction(finger.createPointerMove(Duration.ZERO, VIEWPORT, startX, startY))
    .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
    .addAction(finger.createPointerMove(Duration.ofMillis(600), VIEWPORT, endX, endY))
    .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
driver.perform(List.of(scroll));
```

**Available gestures:**

| Method | Description |
|---|---|
| `tap(x, y)` / `tap(element)` | Single finger tap |
| `doubleTap(x, y)` | Two rapid taps with 100ms gap |
| `longPress(element, duration)` | Press and hold (default 1500ms) |
| `swipe(startX, startY, endX, endY)` | Linear drag between points |
| `scroll(Direction, fraction)` | Scroll by fraction of screen height |
| `swipeToElement(By, maxSwipes)` | Scroll until element is visible |
| `pinchOrZoom(centerX, centerY, distance, zoomIn)` | Two-finger gesture |

---

## ANR Resilience

Android ANR (App Not Responding) dialogs appear under emulator load and block all automation. The framework handles them at three levels:

**Level 1 — DriverManager** dismisses any ANR dialog immediately after session creation.

**Level 2 — BasePage** wraps every `waitForPageLoad()` in an ANR-aware method:

```java
protected WebElement waitForVisibleDismissingDialogs(By locator) {
    try {
        return waitUtils.waitForVisible(locator);      // 15s attempt
    } catch (Exception first) {
        DriverManager.dismissSystemDialogs();           // tap "Wait" button
        return waitUtils.waitForVisible(locator);      // 15s retry
    }
}
```

**Level 3 — GestureUtils** dismisses dialogs between each scroll attempt in `swipeToElement()`.

---

## Test Suite Structure

Three TestNG XML suites select tests by group:

| Suite | Groups | Tests | Estimated Time |
|---|---|---|---|
| `testng.xml` | all | 15 | ~35–45 min |
| `testng-smoke.xml` | smoke | 5 | ~10–15 min |
| `testng-regression.xml` | regression | 10 | ~20–30 min |

**Smoke tests (5):** TC1 Login, TC4 Inventory Loads, TC7 Add to Cart, TC14 Full Checkout, TC15 Logout

**Regression tests (10):** TC2–3 Login errors, TC5–6 Sorting, TC8–9 Cart operations, TC10–11 Cart page, TC12–13 Checkout validation

```bash
mvn clean test                                         # full suite
mvn clean test -DsuiteXmlFile=testng-smoke.xml         # smoke
mvn clean test -DsuiteXmlFile=testng-regression.xml    # regression
```

---

## Allure Reporting

### How It Works

Allure uses **compile-time AspectJ weaving** (CTW). During `mvn compile`, the `dev.aspectj:aspectj-maven-plugin` weaves `@Step` aspects into page method bytecode. Load-time weaving (LTW) is not used — it fails on Java 17+ due to classloader restrictions.

The `AllureTestNg` listener captures every test lifecycle event and writes JSON result files to `target/allure-results/`. The `AllureEnvironmentListener` (ISuiteListener) runs before any test to write `environment.properties` and `categories.json` into that directory.

### Annotations

| Annotation | Where | Purpose |
|---|---|---|
| `@Epic("SauceLabs Mobile App")` | Test class | Top-level grouping |
| `@Feature("Login")` | Test class | Feature grouping in Behaviors tab |
| `@Story("...")` | Test method | Individual scenario name |
| `@TmsLink("TC1")` | Test method | Test case reference |
| `@Severity(BLOCKER)` | Test method | Priority — BLOCKER / CRITICAL / NORMAL |
| `@Step("Enter username: {u}")` | Page method | Named step in Allure timeline |
| `Allure.addAttachment(...)` | CommonUtils | Attaches PNG screenshot on failure |

### Failure Categories (`categories.json`)

Allure classifies failures automatically:

| Category | Matched On |
|---|---|
| Product Defects | `AssertionError` in message |
| App Not Responding (ANR) | `ANR` / `Application Not Responding` in message |
| Element Not Found / Timeout | `NoSuchElementException` / `TimeoutException` |
| Driver / Connection Issues | `WebDriverException` / `connection refused` |
| Skipped Tests | Any skipped status |

### Environment Panel

The Allure Environment panel is populated automatically by `AllureEnvironmentListener`:

```
Device       emulator-5554
Platform     Android 14 (API 34)
App Package  com.swaglabsmobileapp
Appium       http://127.0.0.1:4723
Suite        testng-regression.xml
```

### Report Screenshots

**Overview — pass rate, severity breakdown, suite summary**

![Allure Overview](docs/allure-screenshots/allure%20overview.png)

**Suites — tests grouped by class with step-level detail**

![Allure Suites](docs/allure-screenshots/suites.png)

**Test Case Detail — full step trace, parameters, failure screenshot attached**

![Allure Test Case Detail](docs/allure-screenshots/Test%20case%20details.png)

### Generate and View Report

```bash
# Open live report in browser (recommended)
mvn allure:serve

# Generate static HTML report
mvn allure:report

# Using Allure CLI directly
allure serve target/allure-results
```

**Report sections:**

- **Overview** — pass/rate donut, trend chart, severity breakdown
- **Behaviors** — Epic → Feature → Story hierarchy, all 15 tests
- **Suites** — tests grouped by class
- **Timeline** — execution order and duration per test
- **Categories** — failure classification from `categories.json`
- **Environment** — device and platform details

---

## Key Configuration

**`src/test/resources/config.properties`**

```properties
appiumServerUrl=http://127.0.0.1:4723
platformName=Android
automationName=UiAutomator2
deviceName=emulator-5554
appPackage=com.swaglabsmobileapp
appActivity=com.swaglabsmobileapp.MainActivity
noReset=true
newCommandTimeout=3600
ensureWebviewsHavePages=true
nativeWebScreenshot=true
connectHardwareKeyboard=true
```

**Key Appium capabilities set in `DriverManager`:**

| Capability | Value | Reason |
|---|---|---|
| `noReset` | true | Skip reinstall — avoids `pm clear` ANR |
| `disableWindowAnimation` | true | Faster transitions, fewer timeouts |
| `newCommandTimeout` | 3600s | Keep session alive across all tests |
| `implicitlyWait` | 0 | All waits explicit via `WebDriverWaitUtils` |
| `ignoreHiddenApiPolicyError` | true | Skip hidden API config on restricted emulators |
| `uiautomator2ServerLaunchTimeout` | 60 000ms | Allow for slow emulator startup |

---

## How to Run

### Prerequisites

```bash
# 1. Java 17+
java -version

# 2. Maven
mvn -version

# 3. Android SDK + emulator
echo $ANDROID_HOME
emulator -list-avds

# 4. Appium 2.0 + UiAutomator2 driver
appium --version
appium driver list --installed

# 5. SauceLabs APK installed on emulator (path set in config.properties)
```

### Step-by-Step

```bash
# Terminal 1 — start emulator
emulator -avd <your-avd-name> -no-snapshot-load -gpu host -no-boot-anim -no-audio -memory 1536 &

# Wait for full boot
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null)" = "1" ]; do sleep 3; done && echo "Ready"

# Terminal 2 — start Appium
appium

# Terminal 3 — run tests
cd appium-java-project
mvn clean test -DsuiteXmlFile=testng-regression.xml

# View report
mvn allure:serve
```

### ADB Troubleshooting

```bash
# ADB unresponsive
adb kill-server && adb start-server

# Remove stale UiAutomator2 APKs (fixes instrumentation crash)
adb uninstall io.appium.uiautomator2.server
adb uninstall io.appium.uiautomator2.server.test
adb uninstall io.appium.settings

# Check what's on screen
adb shell dumpsys window | grep mCurrentFocus

# Dismiss ANR dialog
adb shell input keyevent 82
```

---

## Design Decisions Worth Noting

**Single session for all 15 tests** — Starting a new Appium session costs 10–20 seconds on an emulator. One session with UI-level state reset (`ensureLoginScreen`) keeps the suite under 10 minutes instead of ~5 minutes of pure session overhead.

**`noReset=true` instead of `pm clear`** — `pm clear` on an emulator under load consistently triggers ANR dialogs in the system UI. UI-level logout is slower but eliminates that failure mode entirely.

**Compile-time AspectJ weaving** — Load-time weaving (LTW javaagent) fails on Java 17 due to module encapsulation. CTW runs entirely at `mvn compile` with no runtime classloader dependency.

**Accessibility ID as primary locator** — The `test-*` content-description attributes in the SauceLabs app are stable, semantic, and faster to resolve in UiAutomator2 than XPath. XPath is only used when no `content-desc` is available (text-content matching inside ViewGroups).

**Selenium pinned to 4.18.1** — Appium Java Client 9.1.0 has a `[4.17.0,5.0)` Selenium version range. Selenium 4.21+ removed an internal constructor Appium's session handshake depends on. The explicit `dependencyManagement` pin prevents Maven from resolving a breaking version transitively.

---

## Cloud Execution — BrowserStack

The framework is architected to be cloud-ready. Switching from a local emulator to BrowserStack requires **capability changes only** — no test code, no page objects, no utilities change.

### What Changes

Only `DriverManager.initDriver()` needs updating. Replace the local Appium URL and device capabilities with BrowserStack credentials and target device:

```java
// Local emulator (current)
UiAutomator2Options options = new UiAutomator2Options()
    .setDeviceName("emulator-5554")
    .setAppPackage("com.swaglabsmobileapp");
AndroidDriver driver = new AndroidDriver(new URL("http://127.0.0.1:4723"), options);

// BrowserStack (swap in)
UiAutomator2Options options = new UiAutomator2Options()
    .setCapability("browserstack.user",   System.getenv("BS_USERNAME"))
    .setCapability("browserstack.key",    System.getenv("BS_ACCESS_KEY"))
    .setCapability("app",                 "bs://your-app-upload-id")
    .setCapability("device",              "Samsung Galaxy S23")
    .setCapability("os_version",          "13.0")
    .setCapability("project",             "SauceLabs Mobile")
    .setCapability("build",               "Regression - " + LocalDate.now())
    .setCapability("name",                testName);
AndroidDriver driver = new AndroidDriver(new URL("https://hub.browserstack.com/wd/hub"), options);
```

### What Stays the Same

| Component | Local | BrowserStack |
|---|---|---|
| All 15 test classes | unchanged | unchanged |
| All 7 page objects | unchanged | unchanged |
| WebDriverWaitUtils | unchanged | unchanged |
| GestureUtils (W3C) | unchanged | unchanged |
| JSON test data | unchanged | unchanged |
| Allure annotations | unchanged | unchanged |
| TestNG suites | unchanged | unchanged |

### Why It Works Without Changes

- **W3C Actions API** — gestures are protocol-level; BrowserStack's UiAutomator2 grid speaks the same W3C standard
- **Accessibility ID locators** — `content-desc` attributes are resolved by UiAutomator2 on the device, not by the local driver
- **ThreadLocal driver** — `DriverManager` hands the same `AndroidDriver` instance to every layer regardless of where the session lives
- **No hardcoded URLs in tests** — the server URL lives only in `config.properties`

### iOS Extension Path

The same design supports iOS with minimal additions:

```
config.properties  →  add iosAppiumServerUrl, bundleId, XCUITest caps
DriverManager      →  add initIOSDriver() using XCUITestOptions
Page objects       →  subclass or parallel iOS versions with XCUITest locators
testng-ios.xml     →  new suite pointing at iOS test classes
```

The `ThreadLocal<AndroidDriver>` pattern generalises to `ThreadLocal<AppiumDriver>` (the parent class) enabling Android and iOS sessions to coexist in a parallel run.

---

## Project Stats

| Metric | Value |
|---|---|
| Test scenarios | 15 |
| Page classes | 7 |
| Utility classes | 5 |
| Lines of production code | ~1 500 |
| Test suites | 3 (full / smoke / regression) |
| Allure steps per test | 8–25 |
| Locator strategy | Accessibility ID (primary), XPath (fallback) |
| Hardcoded test data strings | 0 |
