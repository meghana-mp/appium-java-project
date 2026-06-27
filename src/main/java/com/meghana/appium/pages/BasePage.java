package com.meghana.appium.pages;

import com.meghana.appium.driver.DriverManager;
import com.meghana.appium.utils.GestureUtils;
import com.meghana.appium.utils.WebDriverWaitUtils;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Base class for all Page Objects.
 * No Page Factory — every find call hits the driver fresh to avoid stale references.
 */
public abstract class BasePage {

    protected final AndroidDriver driver;
    protected final WebDriverWaitUtils waitUtils;
    protected final GestureUtils gestureUtils;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Wires up the shared driver, wait utilities, and gesture utilities for every page.
     * All page subclasses call super(driver) so they inherit these helpers without duplication.
     */
    protected BasePage(AndroidDriver driver) {
        this.driver = driver;
        this.waitUtils = new WebDriverWaitUtils(driver);
        this.gestureUtils = new GestureUtils(driver);
    }

    // ─── Core interaction helpers ────────────────────────────────────────────

    /**
     * Waits for the element to be clickable before clicking it.
     * Avoids ElementClickInterceptedException that occurs when elements exist in DOM but are obscured.
     */
    protected void click(By locator) {
        waitUtils.waitForClickable(locator).click();
    }

    /**
     * Same as click(By) but with a custom timeout — used when a specific interaction
     * needs a shorter or longer wait window than the 15-second default.
     */
    protected void click(By locator, int timeoutSeconds) {
        waitUtils.waitForClickable(locator, timeoutSeconds).click();
    }

    /**
     * Clears an input field then sends the given text.
     * Waits for visibility first to ensure the field is rendered and accepts input.
     * clear() prevents stale text from a previous test run when noReset=true keeps app state.
     */
    protected void type(By locator, String text) {
        WebElement el = waitUtils.waitForVisible(locator);
        el.clear();
        el.sendKeys(text);
    }

    /**
     * Waits for element visibility and returns its text content.
     * Used by page objects to extract displayed labels, titles, and messages.
     */
    protected String getText(By locator) {
        return waitUtils.waitForVisible(locator).getText();
    }

    /**
     * Returns the value of the specified attribute (e.g. "content-desc", "text", "checked")
     * from the first element matching the locator, waiting for visibility first.
     */
    protected String getAttribute(By locator, String attribute) {
        return waitUtils.waitForVisible(locator).getAttribute(attribute);
    }

    /**
     * Returns true if the element is present and visible within the SHORT_TIMEOUT (5s).
     * Used for conditional assertions where absence is a valid state (e.g. cart badge hidden).
     */
    protected boolean isDisplayed(By locator) {
        return waitUtils.isElementPresent(locator);
    }

    /**
     * Returns all elements matching the locator without any wait.
     * Appropriate when the caller already knows the page is loaded and elements may be 0-N.
     */
    protected List<WebElement> findAll(By locator) {
        return driver.findElements(locator);
    }

    /**
     * Waits for a single element to be visible and returns it.
     * Preferred over driver.findElement() to avoid NoSuchElementException on slow renders.
     */
    protected WebElement find(By locator) {
        return waitUtils.waitForVisible(locator);
    }

    // ─── Locator factory helpers ─────────────────────────────────────────────

    /**
     * Creates an accessibility ID locator using Appium's content-desc strategy.
     * The SauceLabs app uses consistent "test-*" content-desc attributes — this is the
     * primary and most reliable locator strategy throughout the framework.
     */
    protected By byAccessibilityId(String id) {
        return AppiumBy.accessibilityId(id);
    }

    /**
     * Creates an XPath locator matching elements whose @text attribute equals the given value.
     * Used for sort modal options and page headers where no accessibilityId is set.
     */
    protected By byText(String text) {
        return By.xpath("//*[@text='" + text + "']");
    }

    /**
     * Creates an XPath locator for partial text match.
     * Useful for dynamic text that includes values (e.g. price labels containing "Total:").
     */
    protected By byTextContains(String partialText) {
        return By.xpath("//*[contains(@text,'" + partialText + "')]");
    }

    /**
     * Creates a locator by Android resource-id.
     * Used as a fallback when content-desc is not available on a given element.
     */
    protected By byResourceId(String resourceId) {
        return By.id(resourceId);
    }

    // ─── ANR-safe page load ──────────────────────────────────────────────────

    /**
     * Waits for the element, and if the first attempt fails, dismisses any system dialog
     * (such as an ANR popup) and retries once. Used in every waitForPageLoad() to prevent
     * test failures caused by transient OS-level interrupts during page transitions.
     */
    protected WebElement waitForVisibleDismissingDialogs(By locator) {
        try {
            return waitUtils.waitForVisible(locator);
        } catch (Exception first) {
            // ANR / system dialog may be blocking — dismiss and retry once
            DriverManager.dismissSystemDialogs();
            return waitUtils.waitForVisible(locator);
        }
    }

    // ─── Subclass contract ───────────────────────────────────────────────────

    /**
     * Each page implements this to block until its defining element is visible.
     * Called immediately after instantiation to guarantee the page is ready before any
     * test interaction begins, replacing the need for arbitrary Thread.sleep() calls.
     */
    public abstract void waitForPageLoad();
}
