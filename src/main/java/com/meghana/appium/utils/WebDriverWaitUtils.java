package com.meghana.appium.utils;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

public class WebDriverWaitUtils {

    private static final Logger log = LoggerFactory.getLogger(WebDriverWaitUtils.class);
    private static final int DEFAULT_TIMEOUT = 15;
    private static final int SHORT_TIMEOUT = 5;

    private final AndroidDriver driver;

    /** Stores the driver instance used to build WebDriverWait objects for each call. */
    public WebDriverWaitUtils(AndroidDriver driver) {
        this.driver = driver;
    }

    /**
     * Creates a WebDriverWait configured with the given timeout.
     * Centralised here so every wait method consistently uses the same construction pattern.
     */
    private WebDriverWait wait(int timeoutSeconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
    }

    /**
     * Waits up to DEFAULT_TIMEOUT (15s) for the element to become visible in the viewport.
     * Visibility means the element exists in DOM and has non-zero size — not just present.
     */
    public WebElement waitForVisible(By locator) {
        log.debug("Waiting for visible: {}", locator);
        return wait(DEFAULT_TIMEOUT).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Same as waitForVisible(By) with a caller-specified timeout.
     * Used by swipeToElement() with a short timeout to detect element visibility between swipes.
     */
    public WebElement waitForVisible(By locator, int timeoutSeconds) {
        log.debug("Waiting {}s for visible: {}", timeoutSeconds, locator);
        return wait(timeoutSeconds).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Waits until the element is both visible and enabled for interaction.
     * Preferred over waitForVisible for buttons and links to prevent premature click attempts
     * on elements that render before they accept input.
     */
    public WebElement waitForClickable(By locator) {
        log.debug("Waiting for clickable: {}", locator);
        return wait(DEFAULT_TIMEOUT).until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Same as waitForClickable(By) with a caller-specified timeout.
     * Used in BasePage.click(By, int) when a non-default window is needed.
     */
    public WebElement waitForClickable(By locator, int timeoutSeconds) {
        log.debug("Waiting {}s for clickable: {}", timeoutSeconds, locator);
        return wait(timeoutSeconds).until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Waits for the element to exist anywhere in the DOM, visible or not.
     * Useful for elements that are in DOM but off-screen (e.g. inside a hidden container).
     */
    public WebElement waitForPresence(By locator) {
        log.debug("Waiting for presence: {}", locator);
        return wait(DEFAULT_TIMEOUT).until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    /**
     * Same as waitForPresence(By) with a caller-specified timeout.
     */
    public WebElement waitForPresence(By locator, int timeoutSeconds) {
        return wait(timeoutSeconds).until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    /**
     * Waits until the element's visible text equals expectedText exactly.
     * Used for assertions that rely on a label updating to a specific value.
     */
    public boolean waitForText(By locator, String expectedText) {
        log.debug("Waiting for text '{}' on: {}", expectedText, locator);
        return wait(DEFAULT_TIMEOUT).until(ExpectedConditions.textToBe(locator, expectedText));
    }

    /**
     * Waits until the element's text contains the given substring.
     * Looser than waitForText — useful for messages that include dynamic values.
     */
    public boolean waitForTextContains(By locator, String partialText) {
        log.debug("Waiting for text containing '{}' on: {}", partialText, locator);
        return wait(DEFAULT_TIMEOUT).until(ExpectedConditions.textToBePresentInElementLocated(locator, partialText));
    }

    /**
     * Polls until at least minimumCount elements matching the locator exist.
     * Used after sort operations (InventoryPage.sortBy) and after page navigations where the
     * list re-renders asynchronously — prevents reading stale elements before the update completes.
     */
    public List<WebElement> waitForElementCount(By locator, int minimumCount) {
        log.debug("Waiting for at least {} elements: {}", minimumCount, locator);
        return wait(DEFAULT_TIMEOUT).until(driver -> {
            List<WebElement> elements = driver.findElements(locator);
            return elements.size() >= minimumCount ? elements : null;
        });
    }

    /**
     * Waits until the element is no longer visible in the viewport (or not present in DOM).
     * Used to confirm loading spinners or transient dialogs have cleared before proceeding.
     */
    public boolean waitForInvisibility(By locator) {
        log.debug("Waiting for invisibility: {}", locator);
        return wait(DEFAULT_TIMEOUT).until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /**
     * Same as waitForInvisibility(By) with a caller-specified timeout.
     */
    public boolean waitForInvisibility(By locator, int timeoutSeconds) {
        return wait(timeoutSeconds).until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /**
     * Returns true if the element becomes visible within SHORT_TIMEOUT (5s), false otherwise.
     * Swallows the timeout exception so callers get a boolean rather than an exception,
     * making it safe for conditional logic (e.g. isCartBadgeDisplayed, isErrorDisplayed).
     */
    public boolean isElementPresent(By locator) {
        try {
            waitForVisible(locator, SHORT_TIMEOUT);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Waits until every element matching the locator is visible.
     * Used when an assertion needs to verify the complete rendered set (e.g. all product cards loaded).
     */
    public List<WebElement> waitForAllVisible(By locator) {
        log.debug("Waiting for all visible: {}", locator);
        return wait(DEFAULT_TIMEOUT).until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }
}
