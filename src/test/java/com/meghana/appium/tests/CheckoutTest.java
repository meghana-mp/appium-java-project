package com.meghana.appium.tests;

import com.meghana.appium.base.BaseTest;
import com.meghana.appium.pages.CartPage;
import com.meghana.appium.pages.CheckoutCompletePage;
import com.meghana.appium.pages.CheckoutInfoPage;
import com.meghana.appium.pages.CheckoutOverviewPage;
import com.meghana.appium.pages.InventoryPage;
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
@Feature("Checkout")
public class CheckoutTest extends BaseTest {

    /**
     * Loaded once at class level so JSON files are parsed a single time and shared across tests.
     */
    private static final JsonDataReader USERS    = new JsonDataReader("testdata/users.json");
    private static final JsonDataReader CHECKOUT = new JsonDataReader("testdata/checkout.json");

    /**
     * Shared helper that logs in as the standard user and returns the InventoryPage.
     * Extracted to avoid duplicating the login sequence in every checkout test method.
     */
    private InventoryPage loginAsStandardUser() {
        return getLoginPage().login(
                USERS.getText("validUser", "username"),
                USERS.getText("validUser", "password"));
    }

    /**
     * Shared helper that logs in, adds one item to the cart, and navigates to the cart page.
     * Three checkout tests share this precondition so it is extracted to keep each test
     * focused on its specific scenario without repeating the setup steps.
     */
    private CartPage addItemAndGoToCart() {
        InventoryPage inventoryPage = loginAsStandardUser();
        inventoryPage.addProductToCart(CHECKOUT.getText("products", "item1"));
        return inventoryPage.goToCart();
    }

    /**
     * TC12 — Verifies that the checkout info form shows the correct validation errors for blank fields.
     * Tests the three progressive error states: all blank → first name only → first + last name.
     * tapContinueExpectingError() waits for the error banner rather than navigating forward,
     * and the form retains previously entered values so only missing fields trigger new errors.
     */
    @Test(groups = {"checkout", "regression"}, description = "TC12 - Checkout Information Validation")
    @TmsLink("TC12")
    @Story("Validate checkout form requires all fields before allowing progression")
    @Description("Verify form validation on the Checkout Information screen with blank fields")
    @Severity(SeverityLevel.CRITICAL)
    public void testCheckoutInfoValidation() {
        CheckoutInfoPage infoPage = addItemAndGoToCart().proceedToCheckout();

        // All fields blank
        infoPage.tapContinueExpectingError();
        Assert.assertTrue(infoPage.isErrorDisplayed(), "Error should show for blank first name");
        Assert.assertTrue(
                infoPage.getErrorMessage().contains(CHECKOUT.getText("errorMessages", "firstNameRequired")),
                "Error should mention First Name");

        // First name only
        infoPage.enterFirstName(CHECKOUT.getText("validCheckout", "firstName"))
                .tapContinueExpectingError();
        Assert.assertTrue(
                infoPage.getErrorMessage().contains(CHECKOUT.getText("errorMessages", "lastNameRequired")),
                "Error should mention Last Name after entering first name");

        // First + Last name
        infoPage.enterLastName(CHECKOUT.getText("validCheckout", "lastName"))
                .tapContinueExpectingError();
        Assert.assertTrue(
                infoPage.getErrorMessage().contains(CHECKOUT.getText("errorMessages", "postalCodeRequired")),
                "Error should mention Postal Code after entering first and last name");
    }

    /**
     * TC13 — Verifies that the checkout overview page displays a mathematically correct total.
     * isTotalCorrect() compares subtotal + tax against the displayed total after rounding both
     * to 2 decimal places, avoiding floating-point precision failures on values like 0.1 + 0.2.
     * Both positive assertions for itemTotal and tax guard against the app rendering "$0.00".
     */
    @Test(groups = {"checkout", "regression"}, description = "TC13 - Checkout Overview & Total Calculation")
    @TmsLink("TC13")
    @Story("Verify order summary shows correct subtotal, tax, and grand total")
    @Description("Verify item total, tax, and final total are calculated correctly")
    @Severity(SeverityLevel.CRITICAL)
    public void testCheckoutOverviewTotalCalculation() {
        CheckoutOverviewPage overviewPage = addItemAndGoToCart()
                .proceedToCheckout()
                .fillAndContinue(
                        CHECKOUT.getText("validCheckout", "firstName"),
                        CHECKOUT.getText("validCheckout", "lastName"),
                        CHECKOUT.getText("validCheckout", "postalCode"));

        double itemTotal = overviewPage.getItemTotal();
        double tax       = overviewPage.getTax();
        double total     = overviewPage.getTotal();

        Assert.assertTrue(itemTotal > 0, "Item total should be positive");
        Assert.assertTrue(tax > 0, "Tax should be positive");
        Assert.assertTrue(overviewPage.isTotalCorrect(),
                String.format("Total (%.2f) should equal subtotal (%.2f) + tax (%.2f)",
                        total, itemTotal, tax));
    }

    /**
     * TC14 — Verifies the full end-to-end purchase flow from login through order confirmation.
     * Two items are added to test that the cart correctly carries multiple items into checkout.
     * tapFinish() scrolls to the FINISH button before clicking it because it may be below the fold.
     * Both isOrderSuccessful() and getSuccessHeader() are asserted for belt-and-suspenders
     * coverage of the success screen. Expected strings read from checkout.json, not hardcoded.
     */
    @Test(groups = {"checkout", "smoke"}, description = "TC14 - Complete Checkout Journey")
    @TmsLink("TC14")
    @Story("User completes full purchase from product selection to order confirmation")
    @Description("Verify a user can successfully finish a purchase end-to-end")
    @Severity(SeverityLevel.BLOCKER)
    public void testCompleteCheckoutJourney() {
        InventoryPage inventoryPage = loginAsStandardUser();
        inventoryPage
                .addProductToCart(CHECKOUT.getText("products", "item1"))
                .addProductToCart(CHECKOUT.getText("products", "item2"));

        CartPage cartPage = inventoryPage.goToCart();
        Assert.assertEquals(cartPage.getCartItemCount(), 2, "Cart should have 2 items before checkout");

        CheckoutCompletePage completePage = cartPage
                .proceedToCheckout()
                .fillAndContinue(
                        CHECKOUT.getText("validCheckout", "firstName"),
                        CHECKOUT.getText("validCheckout", "lastName"),
                        CHECKOUT.getText("validCheckout", "postalCode"))
                .tapFinish();

        Assert.assertTrue(completePage.isOrderSuccessful(),
                "Order success screen should be displayed");
        Assert.assertEquals(completePage.getSuccessHeader(),
                CHECKOUT.getText("completionPage", "header"),
                "Success header text mismatch");
    }
}
