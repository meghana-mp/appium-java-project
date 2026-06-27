package com.meghana.appium.pages;

import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

public class SidebarPage extends BasePage {

    private final By ALL_ITEMS_LINK    = byAccessibilityId("test-ALL ITEMS");
    private final By LOGOUT_LINK       = byAccessibilityId("test-LOGOUT");
    private final By RESET_LINK        = byAccessibilityId("test-RESET APP STATE");
    private final By CLOSE_BUTTON      = byAccessibilityId("test-Close");

    /** Wires up the shared driver, waitUtils, and gestureUtils via BasePage. */
    public SidebarPage(AndroidDriver driver) {
        super(driver);
    }

    /**
     * Waits for the LOGOUT link to appear, confirming the sidebar drawer is fully open.
     * LOGOUT is always the last item in the menu so its visibility implies all menu items rendered.
     */
    @Override
    public void waitForPageLoad() {
        waitForVisibleDismissingDialogs(LOGOUT_LINK);
        log.info("SidebarPage ready");
    }

    /**
     * Taps LOGOUT and waits for the LoginPage to load.
     * Preferred logout path in tests — more reliable than pressing Back multiple times because
     * it always lands on the login screen regardless of which page the test started from.
     */
    @Step("Tap Logout")
    public LoginPage logout() {
        click(LOGOUT_LINK);
        LoginPage page = new LoginPage(driver);
        page.waitForPageLoad();
        return page;
    }

    /**
     * Taps ALL ITEMS and waits for the InventoryPage to load.
     * Used to navigate back to the product list from the sidebar without logging out.
     */
    @Step("Tap All Items")
    public InventoryPage goToAllItems() {
        click(ALL_ITEMS_LINK);
        InventoryPage page = new InventoryPage(driver);
        page.waitForPageLoad();
        return page;
    }

    /**
     * Taps RESET APP STATE and returns this SidebarPage instance.
     * Resets the cart and any other persisted app state via the app's built-in reset function.
     * Returns this (not InventoryPage) because the sidebar remains open after reset.
     */
    @Step("Tap Reset App State")
    public SidebarPage resetAppState() {
        click(RESET_LINK);
        return this;
    }

    /**
     * Taps the Close button to dismiss the sidebar and waits for the InventoryPage.
     * Used when a test opens the sidebar to verify menu content but does not want to take action.
     */
    @Step("Close sidebar")
    public InventoryPage closeSidebar() {
        click(CLOSE_BUTTON);
        InventoryPage page = new InventoryPage(driver);
        page.waitForPageLoad();
        return page;
    }

    /**
     * Returns true if the LOGOUT link is visible in the sidebar.
     * Used by NavigationTest to assert the sidebar opened correctly before attempting logout.
     */
    public boolean isLogoutVisible() {
        return isDisplayed(LOGOUT_LINK);
    }
}
