package com.meghana.appium.base;

import com.meghana.appium.driver.DriverManager;
import com.meghana.appium.pages.LoginPage;
import com.meghana.appium.utils.CommonUtils;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.testng.AllureTestNg;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

import java.util.List;

@Listeners({AllureTestNg.class})
public class BaseTest {

    private static final Logger log = LoggerFactory.getLogger(BaseTest.class);
    private static final String APP_PACKAGE = "com.swaglabsmobileapp";

    /**
     * Runs before every test method to ensure a clean, stable starting state.
     * The driver is reused across all tests in the suite (quit only in @AfterSuite) to avoid
     * the overhead of creating a new Appium session for each test. Before each test:
     * 1. Checks driver health with isDriverAlive(); re-inits only if the session has died.
     * 2. Calls ensureAppInForeground() to detect and recover from app crashes to home screen.
     * 3. Dismisses any pending ANR/system dialogs.
     * 4. Navigates to the login screen from whatever page the previous test left the app on.
     */
    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        if (!isDriverAlive()) {
            DriverManager.quitDriver();
            DriverManager.initDriver();
        }
        ensureAppInForeground();
        DriverManager.dismissSystemDialogs();
        ensureLoginScreen();
    }

    /**
     * Probes the driver session by calling getCurrentPackage().
     * If the session has died (Appium server restarted, emulator rebooted) the call throws,
     * which this method catches and converts to false — triggering re-initialisation in setUp().
     */
    private boolean isDriverAlive() {
        try {
            AndroidDriver driver = DriverManager.getDriver();
            if (driver == null) return false;
            driver.getCurrentPackage();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Detects when the app has crashed or been backgrounded to the Android home screen and
     * relaunches it. Without this, tests run against the launcher activity and all element
     * lookups fail silently on incorrect app state. getCurrentPackage() identifies the current
     * foreground app; activateApp() brings our app back to the foreground.
     */
    private void ensureAppInForeground() {
        try {
            AndroidDriver driver = DriverManager.getDriver();
            String pkg = driver.getCurrentPackage();
            if (!APP_PACKAGE.equals(pkg)) {
                log.warn("App not in foreground (current: {}), relaunching", pkg);
                driver.activateApp(APP_PACKAGE);
                Thread.sleep(2000);
                DriverManager.dismissSystemDialogs();
            }
        } catch (Exception e) {
            log.warn("ensureAppInForeground: {}", e.getMessage());
        }
    }

    /**
     * Takes a screenshot and attaches it to the Allure report on test failure.
     * The driver is kept alive (not quit) so subsequent tests can reuse the session.
     * Only failure screenshots are taken to avoid inflating report size on passing tests.
     */
    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            try {
                CommonUtils.takeScreenshot(DriverManager.getDriver(), "FAILED_" + result.getName());
            } catch (Exception e) {
                // Session may have died before tearDown runs (socket hang up); log and continue
                // so the exception here does not mask the actual test failure in the report.
                log.warn("Could not capture failure screenshot for '{}': {}", result.getName(), e.getMessage());
            }
        }
        // Driver stays alive between tests — quit only after the full suite
    }

    /**
     * Quits the Appium session after all tests in the suite have run.
     * alwaysRun=true ensures this executes even if some tests fail, preventing orphaned sessions.
     */
    @AfterSuite(alwaysRun = true)
    public void tearDownSuite() {
        DriverManager.quitDriver();
    }

    /**
     * Creates a LoginPage instance and waits for it to be ready.
     * Used by test classes to obtain a verified LoginPage as the starting point for each test.
     */
    protected LoginPage getLoginPage() {
        LoginPage page = new LoginPage(DriverManager.getDriver());
        page.waitForPageLoad();
        return page;
    }

    /**
     * Navigates from any app screen back to the login screen.
     * Handles three cases:
     * 1. Already on login screen — returns immediately.
     * 2. On a page with the hamburger menu visible — logs out via the sidebar.
     * 3. Deep inside checkout pages (no menu) — presses Back repeatedly up to 10 times,
     *    checking for login screen or menu availability after each press.
     * If pressing Back causes the app to exit to the home screen, ensureAppInForeground()
     * relaunches it so the next setUp() can retry.
     */
    private void ensureLoginScreen() {
        AndroidDriver driver = DriverManager.getDriver();

        // Already on login page
        if (!driver.findElements(AppiumBy.accessibilityId("test-Username")).isEmpty()) return;

        // Try sidebar logout if the hamburger menu is visible
        if (tryLogoutViaSidebar(driver)) return;

        // Not on inventory/cart page — navigate back page-by-page (handles checkout pages)
        // After each back, check for login page OR a menu to logout from
        for (int i = 0; i < 10; i++) {
            driver.navigate().back();
            try { Thread.sleep(400); } catch (InterruptedException ignored) {}
            DriverManager.dismissSystemDialogs();

            if (!driver.findElements(AppiumBy.accessibilityId("test-Username")).isEmpty()) return;
            if (tryLogoutViaSidebar(driver)) return;
        }

        // If app exited to home screen during backs, relaunch and let next setUp handle it
        ensureAppInForeground();
    }

    /**
     * Attempts to log out by opening the hamburger menu and tapping LOGOUT.
     * Returns true if logout succeeded (menu was found and LOGOUT was tapped).
     * Returns false silently if the menu is not visible on the current screen — the caller
     * then falls back to pressing Back buttons to navigate to a different screen.
     */
    private boolean tryLogoutViaSidebar(AndroidDriver driver) {
        try {
            List<WebElement> menu = driver.findElements(AppiumBy.accessibilityId("test-Menu"));
            if (!menu.isEmpty()) {
                menu.get(0).click();
                Thread.sleep(500);
                driver.findElement(AppiumBy.accessibilityId("test-LOGOUT")).click();
                Thread.sleep(800);
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }
}
