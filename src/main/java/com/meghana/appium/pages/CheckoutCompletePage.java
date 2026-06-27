package com.meghana.appium.pages;

import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

public class CheckoutCompletePage extends BasePage {

    /**
     * XPath text locator for the order success header.
     * No accessibility ID is provided for this element in the SauceLabs app, so
     * exact text match is the most stable locator strategy available.
     */
    private final By SUCCESS_HEADER  = byText("THANK YOU FOR YOUR ORDER");
    private final By BACK_HOME_BTN   = byAccessibilityId("test-BACK HOME");

    /** Wires up the shared driver, waitUtils, and gestureUtils via BasePage. */
    public CheckoutCompletePage(AndroidDriver driver) {
        super(driver);
    }

    /**
     * Waits for the success header to appear, confirming the order completion screen is loaded.
     * This is the definitive indicator that a purchase was successfully submitted.
     */
    @Override
    public void waitForPageLoad() {
        waitForVisibleDismissingDialogs(SUCCESS_HEADER);
        log.info("CheckoutCompletePage ready");
    }

    /**
     * Returns the text of the success header element.
     * Used by tests to assert the exact displayed string matches "THANK YOU FOR YOUR ORDER".
     */
    @Step("Get success header text")
    public String getSuccessHeader() {
        return getText(SUCCESS_HEADER);
    }

    /**
     * Returns true if the order success header is currently visible on screen.
     * Provides a boolean check that tests can assert without reading the header text.
     */
    public boolean isOrderSuccessful() {
        return isDisplayed(SUCCESS_HEADER);
    }

    /**
     * Clicks the BACK HOME button and waits for the InventoryPage to load.
     * Returns the user to the product list after a completed purchase,
     * which also resets the cart to empty in the SauceLabs demo app.
     */
    @Step("Tap Back Home")
    public InventoryPage tapBackHome() {
        click(BACK_HOME_BTN);
        InventoryPage page = new InventoryPage(driver);
        page.waitForPageLoad();
        return page;
    }
}
