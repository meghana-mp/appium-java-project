package com.meghana.appium.driver;

import com.meghana.appium.utils.ConfigReader;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.List;

public class DriverManager {

    private static final Logger log = LoggerFactory.getLogger(DriverManager.class);

    /**
     * ThreadLocal stores a separate AndroidDriver per thread, enabling safe parallel test
     * execution without shared driver state across threads.
     */
    private static final ThreadLocal<AndroidDriver> driverThread = new ThreadLocal<>();

    /** Prevents instantiation — all methods are static; driver lifecycle is managed at the class level. */
    private DriverManager() {}

    /**
     * Returns the AndroidDriver for the calling thread.
     * Returns null if initDriver() has not been called on this thread.
     */
    public static AndroidDriver getDriver() {
        return driverThread.get();
    }

    /**
     * Creates a new Appium session for this thread using capabilities from config.properties.
     * Uses UiAutomator2Options (type-safe Appium 2.0 API) instead of deprecated DesiredCapabilities.
     * noReset=true keeps the app installed between sessions to avoid slow reinstall overhead.
     * Implicit wait is set to 0 so all waits go through WebDriverWait with explicit timeouts.
     */
    public static void initDriver() {
        if (driverThread.get() != null) {
            log.warn("Driver already initialised for this thread — skipping re-init");
            return;
        }
        ConfigReader config = ConfigReader.getInstance();

        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformName(config.get("platformName"))
                .setAutomationName(config.get("automationName"))
                .setDeviceName(config.get("deviceName"))
                .setAppPackage(config.get("appPackage"))
                .setAppActivity(config.get("appActivity"))
                .setNewCommandTimeout(Duration.ofSeconds(config.getInt("newCommandTimeout")))
                .setNoReset(config.getBoolean("noReset"));

        options.setCapability("appium:ensureWebviewsHavePages", config.getBoolean("ensureWebviewsHavePages"));
        options.setCapability("appium:nativeWebScreenshot", config.getBoolean("nativeWebScreenshot"));
        options.setCapability("appium:connectHardwareKeyboard", config.getBoolean("connectHardwareKeyboard"));
        options.setCapability("appium:uiautomator2ServerLaunchTimeout", 60000);
        options.setCapability("appium:uiautomator2ServerInstallTimeout", 60000);
        options.setCapability("appium:adbExecTimeout", 120000);
        options.setCapability("appium:androidInstallTimeout", 90000);
        options.setCapability("appium:disableWindowAnimation", true);
        // When the emulator's settings ContentProvider is unavailable (cmd: Can't find service: settings),
        // this flag tells UiAutomator2 to skip the hidden_api_policy configuration step rather than
        // failing the session. Safe for standard app testing that doesn't rely on hidden APIs.
        options.setCapability("appium:ignoreHiddenApiPolicyError", true);

        String serverUrl = config.get("appiumServerUrl");
        URL url;
        try {
            url = new URL(serverUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Appium server URL: " + serverUrl, e);
        }

        // Retry up to 3 times: after a session dies abnormally the emulator's `settings`
        // ContentProvider is briefly unavailable, causing "Can't find service: settings" on
        // the very next session creation. A short delay lets the system recover.
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                AndroidDriver driver = new AndroidDriver(url, options);
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
                dismissSystemDialogs(driver);
                driverThread.set(driver);
                log.info("AndroidDriver initialised (attempt {}) — device: {}, server: {}",
                        attempt, config.get("deviceName"), serverUrl);
                return;
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    throw new RuntimeException(
                            "Failed to create Appium session after " + maxAttempts + " attempts", e);
                }
                log.warn("Session creation attempt {} failed: {}. Retrying in 20s…", attempt, e.getMessage());
                try { Thread.sleep(20000); } catch (InterruptedException ignored) {}
            }
        }
    }

    /**
     * Dismisses Android ANR (Application Not Responding) dialogs by tapping the "Wait" button.
     * Called before each test in BaseTest.setUp() to prevent dialog-blocked UI interactions.
     * Uses findElements (not findElement) so it silently does nothing when no dialog is present.
     */
    public static void dismissSystemDialogs() {
        AndroidDriver driver = driverThread.get();
        if (driver == null) return;
        try {
            List<WebElement> waitButtons = driver.findElements(
                    By.xpath("//*[@text='Wait' or @content-desc='Wait']"));
            if (!waitButtons.isEmpty()) {
                waitButtons.get(0).click();
                log.info("Dismissed system ANR dialog");
            }
        } catch (Exception e) {
            log.debug("No system dialog to dismiss: {}", e.getMessage());
        }
    }

    /**
     * Internal overload used during initDriver() — temporarily sets the driver into ThreadLocal
     * so dismissSystemDialogs() can read it before the session is fully committed.
     */
    private static void dismissSystemDialogs(AndroidDriver driver) {
        driverThread.set(driver);
        dismissSystemDialogs();
    }

    /**
     * Ends the Appium session and removes the driver from ThreadLocal.
     * Always called in BaseTest @AfterSuite so the emulator session is cleanly released.
     * Uses try/finally to ensure ThreadLocal.remove() runs even if quit() throws.
     */
    public static void quitDriver() {
        AndroidDriver driver = driverThread.get();
        if (driver != null) {
            try {
                driver.quit();
                log.info("AndroidDriver quit successfully");
            } catch (Exception e) {
                log.warn("Error quitting driver: {}", e.getMessage());
            } finally {
                driverThread.remove();
            }
        }
    }
}
