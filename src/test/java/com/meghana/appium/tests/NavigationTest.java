package com.meghana.appium.tests;

import com.meghana.appium.base.BaseTest;
import com.meghana.appium.pages.InventoryPage;
import com.meghana.appium.pages.LoginPage;
import com.meghana.appium.pages.SidebarPage;
import com.meghana.appium.utils.JsonDataReader;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import org.testng.Assert;
import org.testng.annotations.Test;

@Epic("SauceLabs Mobile App")
@Feature("Navigation")
public class NavigationTest extends BaseTest {

    /**
     * Loaded once at class level so the JSON file is parsed a single time and reused per test.
     */
    private static final JsonDataReader USERS = new JsonDataReader("testdata/users.json");

    /**
     * TC15 — Verifies that the sidebar opens correctly and that logout returns the user to
     * a clean login screen with no pre-existing error banner.
     * Smoke: logout is part of the critical path — BaseTest.setUp() relies on it to reset state.
     *
     * isLogoutVisible() asserts the sidebar opened and the menu rendered (not just the drawer animation).
     * sidebarPage.logout() waits for LoginPage.waitForPageLoad() which confirms the Username field
     * is visible — so by the time the assertion runs, we know we are on the login screen.
     * The final assertion checks isErrorDisplayed() == false to confirm the login page is clean
     * (no lingering error banner from a previous failed login or session state).
     */
    @Test(groups = {"navigation", "smoke"}, description = "TC15 - App Sidebar Navigation & Logout")
    @TmsLink("TC15")
    @Story("User opens the sidebar menu and successfully logs out to the clean login screen")
    @Description("Verify the burger menu opens and the user can log out successfully")
    @Severity(SeverityLevel.CRITICAL)
    public void testSidebarNavigationAndLogout() {
        InventoryPage inventoryPage = getLoginPage().login(
                USERS.getText("validUser", "username"),
                USERS.getText("validUser", "password"));

        SidebarPage sidebarPage = inventoryPage.openMenu();
        Assert.assertTrue(sidebarPage.isLogoutVisible(),
                "Logout option should be visible in the sidebar");

        LoginPage loginPage = sidebarPage.logout();

        Assert.assertFalse(loginPage.isErrorDisplayed(),
                "Login page should load cleanly after logout (no error)");
    }
}
