# Appium Mobile Test Automation Framework
### SauceLabs Mobile Sample App — Android

---

## Table of Contents

1. [Overview](#1-overview)
2. [Technology Stack](#2-technology-stack)
3. [Architecture](#3-architecture)
4. [Design Patterns](#4-design-patterns)
5. [Folder Structure](#5-folder-structure)
6. [Core Components](#6-core-components)
7. [Test Data Management](#7-test-data-management)
8. [Reporting — Allure](#8-reporting--allure)
9. [Configuration](#9-configuration)
10. [End-to-End Flow](#10-end-to-end-flow)
11. [Test Scenarios](#11-test-scenarios)
12. [How to Run](#12-how-to-run)
13. [Key Design Decisions](#13-key-design-decisions)

---

## 1. Overview

This is a **production-quality mobile test automation framework** built with Appium 2.0 and Java. It automates the end-to-end testing of the **SauceLabs Mobile Sample Android application** (v2.7.1) running on an Android emulator.

The framework covers 15 test scenarios across five functional areas: Login, Inventory, Cart, Checkout, and Navigation. It produces rich Allure reports with screenshots, step logs, and severity tags, and is designed to be maintainable, readable, and resilient to real-world emulator instabilities like ANR dialogs and app crashes.

**App under test:** `com.swaglabsmobileapp`  
**Device:** `emulator-5554` (Android 14, API 34)  
**Appium server:** `http://127.0.0.1:4723`

---

## 2. Technology Stack

| Layer | Tool / Library | Version |
|---|---|---|
| Mobile automation | Appium Java Client | 9.1.0 |
| Browser/driver protocol | Selenium WebDriver | 4.18.1 (pinned) |
| Android driver | UiAutomator2 | Latest via Appium |
| Test framework | TestNG | 7.10.2 |
| Reporting | Allure TestNG | 2.27.0 |
| Aspect weaving (Allure steps) | AspectJ (compile-time) | 1.9.22 |
| Test data | Jackson Databind | 2.17.1 |
| Build tool | Maven | 3.x |
| Language | Java | 17 |
| Logging | SLF4J + Logback | 2.0.13 / 1.5.6 |

---

## 3. Architecture

The framework is organised into four horizontal layers, each with a single responsibility:

```
┌─────────────────────────────────────────────────────┐
│  TEST LAYER  (LoginTest, InventoryTest, CartTest…)  │
│  Business-readable assertions, @Allure annotations  │
├─────────────────────────────────────────────────────┤
│  PAGE LAYER  (LoginPage, InventoryPage, CartPage…)  │
│  Screen interactions, locators, step-level methods  │
├─────────────────────────────────────────────────────┤
│  UTILITY LAYER  (Wait / Gesture / Config / JSON)    │
│  Reusable cross-cutting infrastructure              │
├─────────────────────────────────────────────────────┤
│  DRIVER LAYER  (DriverManager)                      │
│  Singleton AndroidDriver, session lifecycle         │
└─────────────────────────────────────────────────────┘
```

### Data Flow

```
config.properties          users.json / checkout.json
       │                           │
       ▼                           ▼
  ConfigReader               JsonDataReader
       │                           │
       └──────────────┬────────────┘
                      ▼
               DriverManager ──► AndroidDriver (Appium session)
                      │
                      ▼
               Page Objects ──► BasePage helpers ──► WebDriverWaitUtils
                      │                                    │
                      ▼                              GestureUtils (W3C)
               Test Classes
                      │
                      ▼
              Allure Reports + Screenshots
```

---

## 4. Design Patterns

### 4.1 Singleton Pattern — DriverManager

`DriverManager` ensures exactly **one** `AndroidDriver` instance exists per thread. The session is created once before the entire suite runs and kept alive across all 15 tests, avoiding the overhead of starting a new Appium session for each test.

```
First call to initDriver()  ──►  Creates AndroidDriver  ──►  Stored in ThreadLocal
All subsequent calls        ──►  Return the same driver (early-return guard)
After all tests finish      ──►  quitDriver() called in @AfterSuite
```

**ThreadLocal** is used as the container so the same class is safe for parallel execution — each thread gets its own independent driver.

```java
// Pattern: one driver per thread, created once
private static final ThreadLocal<AndroidDriver> driverThread = new ThreadLocal<>();

public static void initDriver() {
    if (driverThread.get() != null) return;   // guard: already initialised
    // ... build options, create driver, set into ThreadLocal
}
```

---

### 4.2 Page Object Model (POM) — without Page Factory

Every screen in the app has a dedicated **Page class** that owns:
- All element locators (as `private final By` fields)
- All interaction methods for that screen
- A `waitForPageLoad()` that blocks until the screen is ready

Tests never touch locators or `driver.findElement` directly — they only call readable page methods.

**Why not Page Factory?** Page Factory caches element references at class creation time. On mobile, elements frequently go stale after navigation or orientation changes. Without Page Factory, every `click()`/`type()`/`getText()` call finds the element fresh, avoiding `StaleElementReferenceException`.

```java
// Locators are By constants — found fresh on each interaction
private final By LOGIN_BUTTON = byAccessibilityId("test-LOGIN");

public InventoryPage tapLogin() {
    click(LOGIN_BUTTON);          // always finds the button live from DOM
    InventoryPage page = new InventoryPage(driver);
    page.waitForPageLoad();
    return page;
}
```

---

### 4.3 Fluent Interface — Method Chaining

Page methods return `this` (or the next page) so test steps read like natural language:

```java
// Test code reads as a story
loginPage
    .enterFirstName("Meghana")
    .enterLastName("MP")
    .enterPostalCode("560001")
    .tapContinue();
```

Methods that stay on the same page return `this`. Methods that navigate return the new page object.

---

### 4.4 Factory Method — Page Navigation

Navigation between pages is handled by factory-style methods on the current page. The caller never instantiates the next page directly:

```java
// CartPage returns a CheckoutInfoPage — caller gets back a ready page object
public CheckoutInfoPage proceedToCheckout() {
    gestureUtils.swipeToElement(CHECKOUT_BUTTON, 3);
    click(CHECKOUT_BUTTON);
    CheckoutInfoPage page = new CheckoutInfoPage(driver);
    page.waitForPageLoad();   // blocks until next page is ready
    return page;
}
```

---

### 4.5 Template Method — BasePage

`BasePage` defines the **contract** that all page objects must fulfil (`waitForPageLoad()` is abstract) and provides protected helper methods that all pages share. Concrete pages fill in the details.

```
BasePage (abstract)
│  protected click(By)        ← all pages use this
│  protected type(By, String) ← all pages use this
│  protected getText(By)       ← all pages use this
│  waitForVisibleDismissingDialogs(By) ← ANR-safe wait
│  abstract waitForPageLoad()  ← each page must implement
│
├── LoginPage
├── InventoryPage
├── CartPage
├── CheckoutInfoPage
├── CheckoutOverviewPage
├── CheckoutCompletePage
└── SidebarPage
```

---

### 4.6 Data-Driven Testing — JSON + JsonDataReader

Test data lives in JSON files, not in test code. `JsonDataReader` wraps Jackson and provides typed accessors. Tests reference data by key path, so changing a test user or product name requires editing one JSON file, not hunting through test classes.

```java
// Test reads data symbolically — no hardcoded strings
String username = USERS.getText("validUser", "username");
String item1    = CHECKOUT.getText("products", "item1");
```

---

## 5. Folder Structure

```
appium-java-project/
│
├── pom.xml                          Maven build: deps, plugins, Surefire, Allure
├── testng.xml                       Suite definition: test order, groups, listeners
│
└── src/
    ├── main/
    │   └── java/com/meghana/appium/
    │       │
    │       ├── driver/
    │       │   └── DriverManager.java          Singleton AndroidDriver + ANR dialog handler
    │       │
    │       ├── pages/
    │       │   ├── BasePage.java               Abstract base: helpers + ANR-safe wait
    │       │   ├── LoginPage.java              Login screen
    │       │   ├── InventoryPage.java          Product listing screen
    │       │   ├── CartPage.java               Shopping cart screen
    │       │   ├── CheckoutInfoPage.java       Checkout form (name/postal)
    │       │   ├── CheckoutOverviewPage.java   Order summary screen
    │       │   ├── CheckoutCompletePage.java   Order confirmation screen
    │       │   └── SidebarPage.java            Burger-menu sidebar
    │       │
    │       └── utils/
    │           ├── ConfigReader.java           Singleton config.properties reader
    │           ├── WebDriverWaitUtils.java      All explicit wait wrappers
    │           ├── GestureUtils.java           Touch gestures via W3C Actions API
    │           ├── CommonUtils.java            Screenshot, price parse, sort check
    │           └── JsonDataReader.java         Jackson-based JSON test data reader
    │
    └── test/
        ├── java/com/meghana/appium/
        │   ├── base/
        │   │   └── BaseTest.java               @Before/@After lifecycle, app-state recovery
        │   │
        │   └── tests/
        │       ├── LoginTest.java              TC1, TC2, TC3
        │       ├── InventoryTest.java          TC4, TC5, TC6, TC7, TC8, TC9
        │       ├── CartTest.java               TC10, TC11
        │       ├── CheckoutTest.java           TC12, TC13, TC14
        │       └── NavigationTest.java         TC15
        │
        └── resources/
            ├── config.properties               Device, app, Appium server config
            ├── allure.properties               Allure results directory
            ├── logback-test.xml                Console + file logging config
            └── testdata/
                ├── users.json                  Test user credentials + error messages
                └── checkout.json               Product names, checkout data, error messages
```

---

## 6. Core Components

### 6.1 DriverManager

**File:** `src/main/java/com/meghana/appium/driver/DriverManager.java`

The central hub for the Appium session. Responsibilities:
- Build `UiAutomator2Options` from `config.properties`
- Create the `AndroidDriver` once and store it in `ThreadLocal`
- Expose `getDriver()` for use throughout the framework
- Dismiss ANR ("App not responding") dialogs via `dismissSystemDialogs()`
- Quit the driver cleanly after the suite

Key capabilities set on the driver:

| Capability | Value | Purpose |
|---|---|---|
| `noReset` | true | Don't reinstall/clear app between sessions |
| `disableWindowAnimation` | true | Faster transitions, fewer ANRs |
| `uiautomator2ServerLaunchTimeout` | 60 000 ms | Allow slow emulator startup |
| `adbExecTimeout` | 120 000 ms | Handle slow ADB commands |
| `newCommandTimeout` | 3600 s | Keep session alive across all 15 tests |
| `implicitlyWait` | 0 | Disabled — all waits are explicit via WebDriverWaitUtils |

---

### 6.2 BasePage

**File:** `src/main/java/com/meghana/appium/pages/BasePage.java`

The abstract foundation that every page class extends. It provides:

**Core interaction helpers** (all using explicit waits internally):
```
click(By)                  — waits for clickable, then clicks
type(By, String)           — waits for visible, clears, sends keys
getText(By)                — waits for visible, returns text
isDisplayed(By)            — returns true if visible within SHORT_TIMEOUT
findAll(By)                — driver.findElements (no wait, for list retrieval)
```

**Locator factory helpers:**
```
byAccessibilityId(String)  — AppiumBy.accessibilityId(id) — primary strategy
byText(String)             — XPath @text exact match
byTextContains(String)     — XPath contains(@text)
byResourceId(String)       — By.id
```

**ANR-safe page load:**
```java
protected WebElement waitForVisibleDismissingDialogs(By locator) {
    try {
        return waitUtils.waitForVisible(locator);      // 15 s attempt
    } catch (Exception first) {
        DriverManager.dismissSystemDialogs();          // dismiss ANR "Wait" button
        return waitUtils.waitForVisible(locator);      // 15 s retry
    }
}
```

Every `waitForPageLoad()` in every page class uses this method, so a single ANR dialog appearing during a page transition is automatically dismissed and recovered from.

---

### 6.3 WebDriverWaitUtils

**File:** `src/main/java/com/meghana/appium/utils/WebDriverWaitUtils.java`

Centralises all explicit waits. `implicitlyWait` is set to 0 on the driver so that all timing control is deliberate and in one place.

| Method | Condition | Default timeout |
|---|---|---|
| `waitForVisible(By)` | `visibilityOfElementLocated` | 15 s |
| `waitForClickable(By)` | `elementToBeClickable` | 15 s |
| `waitForPresence(By)` | `presenceOfElementLocated` | 15 s |
| `waitForText(By, String)` | `textToBe` | 15 s |
| `waitForElementCount(By, int)` | custom — list size ≥ min | 15 s |
| `waitForInvisibility(By)` | `invisibilityOfElementLocated` | 15 s |
| `isElementPresent(By)` | tries `waitForVisible` | 5 s (returns bool) |

All methods accept an optional `int timeoutSeconds` override for cases that need tighter or looser windows (e.g., `swipeToElement` uses a 2 s probe per swipe attempt).

---

### 6.4 GestureUtils

**File:** `src/main/java/com/meghana/appium/utils/GestureUtils.java`

All touch gestures use the **W3C Actions API** (`PointerInput` + `Sequence`). The deprecated Appium `TouchAction` and `MultiTouchAction` APIs were removed in Java client 8+.

**Available gestures:**

| Method | Description |
|---|---|
| `tap(int x, int y)` | Single tap at screen coordinates |
| `tap(WebElement)` | Tap at element centre |
| `doubleTap(int x, int y)` | Two rapid taps |
| `longPress(WebElement, Duration)` | Press and hold |
| `swipe(startX, startY, endX, endY)` | Single swipe between two points |
| `scroll(Direction, double fraction)` | Scroll viewport by fraction of screen height |
| `swipeToElement(By, maxSwipes)` | Scroll DOWN until element visible (used to reveal off-screen buttons) |
| `pinchOrZoom(centerX, centerY, distance, zoomIn)` | Two-finger pinch or spread |

**How swipeToElement works:**
```
For each attempt (up to maxSwipes):
  1. Try waitForVisible(locator, 2s)
  2. If found → return the element
  3. If not found → dismissSystemDialogs() (in case ANR is blocking)
                  → scroll DOWN by 40% of screen height
                  → repeat
After maxSwipes → throw RuntimeException
```

This is used before clicking the CHECKOUT button and FINISH button on the overview page, both of which are below the fold when the cart has multiple items.

---

### 6.5 BaseTest

**File:** `src/test/java/com/meghana/appium/base/BaseTest.java`

Controls the lifecycle for every test method:

**`@BeforeMethod` — setUp():**
```
1. isDriverAlive()         → if session died, quit and reinitialise
2. ensureAppInForeground() → if app crashed to home screen, activateApp()
3. dismissSystemDialogs()  → clear any ANR dialogs at test start
4. ensureLoginScreen()     → navigate back to login page regardless of prior test state
```

**`ensureLoginScreen()` — state machine:**
```
Is test-Username visible?         → already on login, done
Is test-Menu visible?             → logout via sidebar, done
Navigate back (up to 10 times):
  After each back:
    Is test-Username visible?     → done
    Is test-Menu visible?         → logout via sidebar, done
Still not on login after 10 backs → ensureAppInForeground() to relaunch
```

**`@AfterMethod` — tearDown():**
- If test FAILED → take screenshot and attach to Allure report
- Driver is NOT quit (kept alive for the next test)

**`@AfterSuite` — tearDownSuite():**
- Calls `DriverManager.quitDriver()` — the only place the session ends

---

### 6.6 ConfigReader

**File:** `src/main/java/com/meghana/appium/utils/ConfigReader.java`

A lazy-initialised singleton that loads `config.properties` from the classpath once. All other classes call `ConfigReader.getInstance().get("key")` instead of reading properties files directly.

---

## 7. Test Data Management

### Structure

```
testdata/
├── users.json
└── checkout.json
```

**`users.json`**
```json
{
  "validUser":       { "username": "standard_user",   "password": "secret_sauce" },
  "lockedOutUser":   { "username": "locked_out_user", "password": "secret_sauce" },
  "invalidPassword": { "username": "standard_user",   "password": "wrong_password" },
  "errorMessages": {
    "lockedOut":       "Sorry, this user has been locked out.",
    "invalidPassword": "Username and password do not match any user in this service."
  }
}
```

**`checkout.json`**
```json
{
  "validCheckout": { "firstName": "Test", "lastName": "User", "postalCode": "10001" },
  "products": {
    "item1": "Sauce Labs Backpack",
    "item2": "Sauce Labs Bike Light",
    "item3": "Sauce Labs Bolt T-Shirt"
  },
  "errorMessages": {
    "firstNameRequired":  "First Name is required",
    "lastNameRequired":   "Last Name is required",
    "postalCodeRequired": "Postal Code is required"
  }
}
```

### How it is used in tests

```java
// JsonDataReader is created once per test class
private static final JsonDataReader USERS    = new JsonDataReader("testdata/users.json");
private static final JsonDataReader CHECKOUT = new JsonDataReader("testdata/checkout.json");

// Values are read symbolically — no hardcoded strings in test logic
String username = USERS.getText("validUser", "username");
String item1    = CHECKOUT.getText("products", "item1");
String errMsg   = USERS.getText("errorMessages", "lockedOut");
```

---

## 8. Reporting — Allure

### How it works

Allure reporting uses **compile-time AspectJ weaving** (CTW). During `mvn compile`, the `dev.aspectj:aspectj-maven-plugin` weaves Allure's `@Step` and `@Feature` annotations into the bytecode. This is more reliable than the load-time weaving (LTW) javaagent approach, which fails on Java 17 due to classloader restrictions.

The `AllureTestNg` listener (declared in both `testng.xml` and `@Listeners` on `BaseTest`) captures test lifecycle events and writes result JSON files to `allure-results/`.

### Annotations used

| Annotation | Applied on | Purpose |
|---|---|---|
| `@Feature("Login")` | Test class | Groups tests by feature in report |
| `@Description("...")` | Test method | Human-readable test purpose |
| `@Severity(SeverityLevel.BLOCKER)` | Test method | Priority tagging (BLOCKER / CRITICAL / NORMAL) |
| `@Step("Enter username: {username}")` | Page method | Appears as a named step in the report |
| `Allure.addAttachment(...)` | CommonUtils | Attaches screenshot PNG to the test |

### Generating the report

```bash
# Run tests and generate raw results
mvn clean test

# Open live report in browser (starts local server)
mvn allure:serve

# Generate static HTML report
allure generate allure-results --clean -o allure-report
allure open allure-report
```

---

## 9. Configuration

**File:** `src/test/resources/config.properties`

```properties
# Appium server
appiumServerUrl=http://127.0.0.1:4723

# Device
platformName=Android
automationName=UiAutomator2
deviceName=emulator-5554

# App
appPackage=com.swaglabsmobileapp
appActivity=com.swaglabsmobileapp.MainActivity
noReset=true

# Timeouts
newCommandTimeout=3600
```

All values are read via `ConfigReader` — changing the device, app package, or server URL requires editing only this file.

**`allure.properties`**
```properties
allure.results.directory=allure-results
```

**`logback-test.xml`**  
Configures console output at INFO level and writes a rolling log file during test runs.

---

## 10. End-to-End Flow

### Complete journey from `mvn clean test` to report

```
mvn clean test
│
├─ AspectJ CTW weaves @Step into page method bytecodes
│
├─ Surefire reads testng.xml → discovers 5 test classes, 15 methods
│
├─ @BeforeSuite (implicit) — driver not yet created
│
├─ For each test method (in XML order):
│   │
│   ├─ @BeforeMethod setUp()
│   │   ├─ isDriverAlive() → first call creates AndroidDriver via DriverManager.initDriver()
│   │   ├─ ensureAppInForeground() → verify app package is active
│   │   ├─ dismissSystemDialogs() → clear any ANR dialogs
│   │   └─ ensureLoginScreen() → navigate/logout back to login page
│   │
│   ├─ Test method executes
│   │   ├─ Reads test data from JSON
│   │   ├─ Calls page methods (interactions recorded as @Step in Allure)
│   │   ├─ Performs assertions with descriptive messages
│   │   └─ On failure → screenshot attached to Allure
│   │
│   └─ @AfterMethod tearDown()
│       ├─ FAIL → takeScreenshot() → Allure.addAttachment()
│       └─ Driver kept alive (no quit)
│
├─ @AfterSuite tearDownSuite()
│   └─ DriverManager.quitDriver() — single session end
│
└─ allure-results/ populated → mvn allure:serve opens report
```

### How a single test flows through the layers

Taking `testCompleteCheckoutJourney` (TC14) as an example:

```
CheckoutTest.testCompleteCheckoutJourney()
│
├─ loginAsStandardUser()
│   └─ getLoginPage()                        → LoginPage.waitForPageLoad()
│       └─ waitForVisibleDismissingDialogs()  → WebDriverWaitUtils.waitForVisible()
│   └─ loginPage.login(user, pass)
│       ├─ type(USERNAME_FIELD, user)         → WebDriverWaitUtils.waitForVisible() + sendKeys
│       ├─ type(PASSWORD_FIELD, pass)
│       ├─ click(LOGIN_BUTTON)                → WebDriverWaitUtils.waitForClickable() + click()
│       └─ new InventoryPage() → waitForPageLoad()
│
├─ inventoryPage.addProductToCart("Sauce Labs Backpack")
│   ├─ GestureUtils.swipeToElement(addBtn, 5) → scrolls until button visible
│   └─ click(addBtn)
│
├─ inventoryPage.addProductToCart("Sauce Labs Bike Light")
│
├─ inventoryPage.goToCart()
│   └─ click(CART_ICON) → new CartPage() → waitForPageLoad()
│
├─ cartPage.getCartItemCount()                → driver.findElements(CART_ITEM).size()
├─ Assert.assertEquals(count, 2)
│
├─ cartPage.proceedToCheckout()
│   ├─ GestureUtils.swipeToElement(CHECKOUT_BUTTON, 3)
│   ├─ click(CHECKOUT_BUTTON)
│   └─ new CheckoutInfoPage() → waitForPageLoad()
│
├─ infoPage.fillAndContinue("Meghana", "MP", "560001")
│   └─ new CheckoutOverviewPage() → waitForPageLoad()
│
├─ overviewPage.tapFinish()
│   ├─ GestureUtils.swipeToElement(FINISH_BUTTON, 3)
│   ├─ click(FINISH_BUTTON)
│   └─ new CheckoutCompletePage() → waitForPageLoad()
│
├─ Assert.assertTrue(completePage.isOrderSuccessful())
└─ Assert.assertEquals(completePage.getSuccessHeader(), "THANK YOU FOR YOUR ORDER")
```

---

## 11. Test Scenarios

| TC | Test Method | Feature | Severity | What it verifies |
|---|---|---|---|---|
| 1 | `testSuccessfulLogin` | Login | BLOCKER | Valid credentials → inventory loads |
| 2 | `testLockedOutUserLogin` | Login | CRITICAL | Locked account → correct error message |
| 3 | `testInvalidPasswordLogin` | Login | CRITICAL | Wrong password → correct error message |
| 4 | `testInventoryLoads` | Inventory | CRITICAL | Products list loads with names and prices |
| 5 | `testProductSortingAlphabetical` | Inventory | NORMAL | Sort A→Z and Z→A produces correct order |
| 6 | `testProductSortingPriceLowToHigh` | Inventory | NORMAL | Sort price ascending produces correct order |
| 7 | `testAddSingleItemToCart` | Inventory | CRITICAL | Add one item → cart badge shows "1" |
| 8 | `testRemoveItemFromInventory` | Inventory | NORMAL | Remove item → cart badge disappears |
| 9 | `testAddMultipleItemsToCart` | Inventory | CRITICAL | Add three items → cart badge shows "3" |
| 10 | `testCartPageValidation` | Cart | CRITICAL | Cart shows correct items and names |
| 11 | `testRemoveItemViaCartPage` | Cart | CRITICAL | Remove from cart → count decreases, item gone |
| 12 | `testCheckoutInfoValidation` | Checkout | CRITICAL | Blank/partial form fields show correct errors |
| 13 | `testCheckoutOverviewTotalCalculation` | Checkout | CRITICAL | subtotal + tax = total displayed on overview |
| 14 | `testCompleteCheckoutJourney` | Checkout | BLOCKER | Full purchase end-to-end → success screen |
| 15 | `testSidebarNavigationAndLogout` | Navigation | CRITICAL | Sidebar opens, logout returns to clean login |

---

## 12. How to Run

### Prerequisites

1. **Java 17+** — `java -version`
2. **Maven 3.x** — `mvn -version`
3. **Android SDK + emulator** — `emulator -list-avds`
4. **Appium 2.0** — `appium --version`
5. **UiAutomator2 driver** — `appium driver list --installed`
6. **SauceLabs APK** at the path referenced in `config.properties`

### Step-by-step

```bash
# 1. Start the Android emulator (if not already running)
emulator @<your-avd-name> &

# 2. Start the Appium server (in a separate terminal)
appium

# 3. Verify device is connected
adb devices

# 4. Run the full test suite
cd appium-java-project
mvn clean test

# 5. Open the Allure report
mvn allure:serve
```

### Run a specific group only

```bash
# Only login tests
mvn test -Dgroups=login

# Only checkout tests
mvn test -Dgroups=checkout
```

### ADB troubleshooting commands

```bash
# If ADB becomes unresponsive
adb kill-server && adb start-server

# Remove stale UiAutomator2 APKs (fixes instrumentation crash)
adb uninstall io.appium.uiautomator2.server
adb uninstall io.appium.uiautomator2.server.test
adb uninstall io.appium.settings
```

---

## 13. Key Design Decisions

### Why one driver session for all 15 tests?

Starting an Appium session takes 10–20 seconds on an emulator (instrumentation install, server start). With 15 tests, creating a new session per test would add 3–5 minutes of pure overhead. The single-session approach keeps the suite under 10 minutes.

The tradeoff is that `ensureLoginScreen()` must actively restore the app to a known state before each test. This is more complex than per-test sessions but produces a dramatically faster suite.

### Why `noReset=true`?

Setting `noReset=false` causes Appium to run `pm clear com.swaglabsmobileapp` before each session. On an emulator under load, this ADB command frequently triggers ANR dialogs in the system UI, causing cascade failures. `noReset=true` avoids this entirely. Clean state is maintained instead by the UI-level logout in `ensureLoginScreen()`.

### Why compile-time AspectJ weaving?

Allure's `@Step` annotation is processed by an AspectJ aspect (`StepsAspects`). On Java 17, the load-time weaving (LTW) javaagent approach fails because the JVM restricts unnamed module access to internal APIs. Compile-time weaving (CTW) runs entirely at `mvn compile` time and has no runtime classloader dependency, making it fully compatible with Java 17+.

### Why no Page Factory?

`PageFactory.initElements()` caches `WebElement` references at the point of initialisation. On mobile, page content can change from one test to the next (different cart states, scroll positions), making cached references stale. Every interaction in this framework finds elements live, which is slightly slower but never produces `StaleElementReferenceException`.

### Why `content-desc` (accessibility ID) as the primary locator strategy?

The SauceLabs demo app uses `test-*` content-description attributes on all interactive elements (e.g., `test-LOGIN`, `test-Item title`, `test-CHECKOUT`). These are:
- **Stable** — won't change with UI layout refactors
- **Semantic** — describe what the element IS, not where it is
- **Fast** — accessibility ID lookup in UiAutomator2 is faster than XPath

XPath is used only when accessibility IDs aren't available (e.g., finding text content within a ViewGroup, or matching elements by their visible text).

### Why Selenium pinned to 4.18.1?

Appium Java Client 9.1.0 depends on Selenium 4.x. Selenium 4.21+ removed the 3-parameter `ProtocolHandshake` constructor that Appium's internal code used. Without the explicit `dependencyManagement` pin in `pom.xml`, Maven would resolve a newer Selenium version transitively, breaking the Appium session initialisation with a `NoSuchMethodError`.
