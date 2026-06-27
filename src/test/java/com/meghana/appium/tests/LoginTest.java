package com.meghana.appium.tests;

import com.meghana.appium.base.BaseTest;
import com.meghana.appium.pages.InventoryPage;
import com.meghana.appium.pages.LoginPage;
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
@Feature("Login")
public class LoginTest extends BaseTest {

    /**
     * Loaded once at class level so the JSON file is parsed a single time
     * and reused across all test methods, rather than re-reading the file per test.
     */
    private static final JsonDataReader USERS = new JsonDataReader("testdata/users.json");

    /**
     * TC1 — Verifies that a valid user can log in and reach the inventory screen.
     * Credentials are read from users.json to keep the test data out of the test code.
     * Asserts product count > 0 as a proxy for a fully loaded inventory page.
     * Smoke: this is the gate test — if login is broken, all other tests are meaningless.
     */
    @Test(groups = {"login", "smoke"}, description = "TC1 - Successful Login")
    @TmsLink("TC1")
    @Story("Valid user can log in and reach the product inventory")
    @Description("Verify user can log in with standard_user and valid password")
    @Severity(SeverityLevel.BLOCKER)
    public void testSuccessfulLogin() {
        String username = USERS.getText("validUser", "username");
        String password = USERS.getText("validUser", "password");

        InventoryPage inventoryPage = getLoginPage()
                .enterUsername(username)
                .enterPassword(password)
                .tapLogin();

        Assert.assertTrue(inventoryPage.getProductCount() > 0,
                "Inventory should show products after successful login");
    }

    /**
     * TC2 — Verifies that the locked-out user receives the correct error message.
     * tapLoginExpectingError() is used instead of tapLogin() because we know login will fail,
     * so we wait for the error banner rather than the inventory page header.
     */
    @Test(groups = {"login", "regression"}, description = "TC2 - Locked Out User Login")
    @TmsLink("TC2")
    @Story("Locked-out user sees an appropriate error message on login attempt")
    @Description("Verify error message appears for locked_out_user")
    @Severity(SeverityLevel.CRITICAL)
    public void testLockedOutUserLogin() {
        String username      = USERS.getText("lockedOutUser", "username");
        String password      = USERS.getText("lockedOutUser", "password");
        String expectedError = USERS.getText("errorMessages", "lockedOut");

        LoginPage loginPage = getLoginPage()
                .enterUsername(username)
                .enterPassword(password)
                .tapLoginExpectingError();

        Assert.assertTrue(loginPage.isErrorDisplayed(), "Error banner should be displayed");
        Assert.assertTrue(loginPage.getErrorMessage().contains(expectedError),
                "Error message should contain: " + expectedError);
    }

    /**
     * TC3 — Verifies that a valid username with a wrong password produces an error.
     * Distinct from TC2 — the error message text differs between locked-out and invalid-password cases.
     */
    @Test(groups = {"login", "regression"}, description = "TC3 - Invalid Password Login")
    @TmsLink("TC3")
    @Story("Invalid credentials show a password mismatch error on the login screen")
    @Description("Verify error message for valid username but wrong password")
    @Severity(SeverityLevel.CRITICAL)
    public void testInvalidPasswordLogin() {
        String username      = USERS.getText("invalidPasswordUser", "username");
        String password      = USERS.getText("invalidPasswordUser", "password");
        String expectedError = USERS.getText("errorMessages", "invalidPassword");

        LoginPage loginPage = getLoginPage()
                .enterUsername(username)
                .enterPassword(password)
                .tapLoginExpectingError();

        Assert.assertTrue(loginPage.isErrorDisplayed(), "Error banner should be displayed");
        Assert.assertTrue(loginPage.getErrorMessage().contains(expectedError),
                "Error message should contain: " + expectedError);
    }
}
