package com.meghana.appium.pages;

import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class LoginPage extends BasePage {

    private final By USERNAME_FIELD = byAccessibilityId("test-Username");
    private final By PASSWORD_FIELD = byAccessibilityId("test-Password");
    private final By LOGIN_BUTTON   = byAccessibilityId("test-LOGIN");
    private final By ERROR_BANNER   = byAccessibilityId("test-Error message");
    /**
     * XPath that finds a non-empty TextView inside the error banner container.
     * The banner ViewGroup exists in DOM even on a clean login screen (no error), so checking
     * for the ViewGroup alone always returns true. This locator requires an actual text-bearing
     * child element, which only appears when a validation error is present.
     */
    private final By ERROR_TEXT     = By.xpath(
            "//*[@content-desc='test-Error message']//android.widget.TextView[string-length(@text)>0]");

    /** Wires up the shared driver, waitUtils, and gestureUtils via BasePage. */
    public LoginPage(AndroidDriver driver) {
        super(driver);
    }

    /**
     * Waits for the Username field to appear, confirming the login screen is fully rendered.
     * Uses waitForVisibleDismissingDialogs to tolerate ANR popups during transitions.
     */
    @Override
    public void waitForPageLoad() {
        waitForVisibleDismissingDialogs(USERNAME_FIELD);
        log.info("LoginPage ready");
    }

    /**
     * Types the given username into the username field.
     * Returns this to support method chaining (fluent interface).
     */
    @Step("Enter username: {username}")
    public LoginPage enterUsername(String username) {
        type(USERNAME_FIELD, username);
        return this;
    }

    /**
     * Types the given password into the password field.
     * Returns this to support method chaining.
     */
    @Step("Enter password")
    public LoginPage enterPassword(String password) {
        type(PASSWORD_FIELD, password);
        return this;
    }

    /**
     * Clicks the LOGIN button and waits for the InventoryPage to load.
     * Use this overload when a successful login is expected — it will fail if login fails
     * because waitForPageLoad() on InventoryPage will time out waiting for PRODUCTS header.
     */
    @Step("Tap Login — expecting success")
    public InventoryPage tapLogin() {
        click(LOGIN_BUTTON);
        InventoryPage page = new InventoryPage(driver);
        page.waitForPageLoad();
        return page;
    }

    /**
     * Clicks LOGIN and waits for the error banner to appear.
     * Use this overload when a failed login is expected (wrong credentials, locked user).
     * The 500ms sleep absorbs a brief animation before the banner text is readable.
     */
    @Step("Tap Login — expecting error")
    public LoginPage tapLoginExpectingError() {
        click(LOGIN_BUTTON);
        waitUtils.waitForVisible(ERROR_BANNER);
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        return this;
    }

    /**
     * Reads the text from the child TextView inside the error banner.
     * Uses ERROR_TEXT (not ERROR_BANNER) so the locator targets the actual message string,
     * not the containing ViewGroup which may be empty.
     */
    @Step("Get error message text")
    public String getErrorMessage() {
        return getText(ERROR_TEXT);
    }

    /**
     * Returns true if an error message is currently displayed on the login screen.
     * Checks for a child TextView with non-empty text inside the banner container rather than
     * the banner ViewGroup itself, which is always present in the DOM even when no error is shown.
     */
    public boolean isErrorDisplayed() {
        return !driver.findElements(ERROR_TEXT).isEmpty();
    }

    /**
     * Convenience method that chains enterUsername, enterPassword, and tapLogin.
     * Used when the test only cares about reaching the InventoryPage, not each individual step.
     */
    @Step("Login as {username}")
    public InventoryPage login(String username, String password) {
        return enterUsername(username)
                .enterPassword(password)
                .tapLogin();
    }
}
