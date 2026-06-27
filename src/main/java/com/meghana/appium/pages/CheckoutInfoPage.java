package com.meghana.appium.pages;

import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

public class CheckoutInfoPage extends BasePage {

    private final By FIRST_NAME_FIELD  = byAccessibilityId("test-First Name");
    private final By LAST_NAME_FIELD   = byAccessibilityId("test-Last Name");
    private final By POSTAL_CODE_FIELD = byAccessibilityId("test-Zip/Postal Code");
    private final By CONTINUE_BUTTON   = byAccessibilityId("test-CONTINUE");
    private final By ERROR_BANNER      = byAccessibilityId("test-Error message");
    /**
     * XPath targeting a non-empty TextView inside the error banner container.
     * Same pattern as LoginPage — the banner ViewGroup is always in the DOM,
     * so we must look for an actual text-bearing child to detect a real error.
     */
    private final By ERROR_TEXT        = By.xpath(
            "//*[@content-desc='test-Error message']//android.widget.TextView[string-length(@text)>0]");
    private final By CANCEL_BUTTON     = byAccessibilityId("test-CANCEL");

    /** Wires up the shared driver, waitUtils, and gestureUtils via BasePage. */
    public CheckoutInfoPage(AndroidDriver driver) {
        super(driver);
    }

    /**
     * Waits for the First Name field to appear, confirming the checkout info form is loaded.
     * Uses waitForVisibleDismissingDialogs to tolerate ANR popups during page transitions.
     */
    @Override
    public void waitForPageLoad() {
        waitForVisibleDismissingDialogs(FIRST_NAME_FIELD);
        log.info("CheckoutInfoPage ready");
    }

    /**
     * Types the given first name into the First Name field.
     * Returns this to support method chaining in the fluent interface.
     */
    @Step("Enter first name: {firstName}")
    public CheckoutInfoPage enterFirstName(String firstName) {
        type(FIRST_NAME_FIELD, firstName);
        return this;
    }

    /**
     * Types the given last name into the Last Name field.
     * Returns this to support method chaining.
     */
    @Step("Enter last name: {lastName}")
    public CheckoutInfoPage enterLastName(String lastName) {
        type(LAST_NAME_FIELD, lastName);
        return this;
    }

    /**
     * Types the given postal code into the Zip/Postal Code field.
     * Returns this to support method chaining.
     */
    @Step("Enter postal code: {postalCode}")
    public CheckoutInfoPage enterPostalCode(String postalCode) {
        type(POSTAL_CODE_FIELD, postalCode);
        return this;
    }

    /**
     * Clicks CONTINUE and waits for the CheckoutOverviewPage to load.
     * Use this overload when all required fields are filled and successful navigation is expected.
     */
    @Step("Tap Continue — expecting success")
    public CheckoutOverviewPage tapContinue() {
        click(CONTINUE_BUTTON);
        CheckoutOverviewPage page = new CheckoutOverviewPage(driver);
        page.waitForPageLoad();
        return page;
    }

    /**
     * Clicks CONTINUE when a validation error is expected (e.g. one or more fields are blank).
     * Waits for the error banner to appear, then pauses briefly for the banner animation to settle
     * so that getErrorMessage() can immediately read stable text.
     */
    @Step("Tap Continue — expecting error")
    public CheckoutInfoPage tapContinueExpectingError() {
        click(CONTINUE_BUTTON);
        waitUtils.waitForVisible(ERROR_BANNER);
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        return this;
    }

    /**
     * Reads the validation error message text from the error banner.
     * Uses ERROR_TEXT (child TextView) rather than ERROR_BANNER (container ViewGroup)
     * to get the actual message string, consistent with the LoginPage strategy.
     */
    @Step("Get error message")
    public String getErrorMessage() {
        return getText(ERROR_TEXT);
    }

    /**
     * Returns true if the error banner element is present in the DOM.
     * Checks for the banner ViewGroup itself (not its text child) because on this page
     * the container reliably disappears when no error is present — unlike the LoginPage.
     */
    public boolean isErrorDisplayed() {
        return !driver.findElements(ERROR_BANNER).isEmpty();
    }

    /**
     * Convenience method that fills all three required fields and submits the form.
     * Used in tests that need to reach the overview page without verifying individual field steps.
     */
    @Step("Fill checkout info: {firstName} {lastName}, {postalCode}")
    public CheckoutOverviewPage fillAndContinue(String firstName, String lastName, String postalCode) {
        return enterFirstName(firstName)
                .enterLastName(lastName)
                .enterPostalCode(postalCode)
                .tapContinue();
    }

    /**
     * Clicks CANCEL and waits for the CartPage to load.
     * Allows the user to return to the cart without completing checkout.
     */
    @Step("Cancel checkout")
    public CartPage cancel() {
        click(CANCEL_BUTTON);
        CartPage page = new CartPage(driver);
        page.waitForPageLoad();
        return page;
    }
}
