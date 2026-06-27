package com.meghana.appium.tests;

import com.meghana.appium.base.BaseTest;
import com.meghana.appium.pages.CartPage;
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

import java.util.List;

@Epic("SauceLabs Mobile App")
@Feature("Cart")
public class CartTest extends BaseTest {

    /**
     * Loaded once at class level so JSON files are parsed a single time and shared across tests.
     */
    private static final JsonDataReader USERS    = new JsonDataReader("testdata/users.json");
    private static final JsonDataReader CHECKOUT = new JsonDataReader("testdata/checkout.json");

    /**
     * Shared helper that logs in as the standard user and returns the InventoryPage.
     * Extracted to avoid duplicating the login sequence in every cart test method.
     */
    private InventoryPage loginAsStandardUser() {
        return getLoginPage().login(
                USERS.getText("validUser", "username"),
                USERS.getText("validUser", "password"));
    }

    /**
     * TC10 — Verifies that items added from the inventory screen appear in the cart with correct names.
     * getCartItemNames() uses a dual-strategy approach to handle variations in how the SauceLabs
     * app renders item title elements on the cart screen vs the inventory screen.
     * Both item count and name presence are asserted to catch partial rendering failures.
     */
    @Test(groups = {"cart", "regression"}, description = "TC10 - Cart Page Validation")
    @TmsLink("TC10")
    @Story("Cart displays correct items and names matching what was added from inventory")
    @Description("Verify items in the cart match the items that were added from inventory")
    @Severity(SeverityLevel.CRITICAL)
    public void testCartPageValidation() {
        String item1 = CHECKOUT.getText("products", "item1");
        String item2 = CHECKOUT.getText("products", "item2");

        InventoryPage inventoryPage = loginAsStandardUser();
        inventoryPage.addProductToCart(item1);
        inventoryPage.addProductToCart(item2);

        CartPage cartPage = inventoryPage.goToCart();

        List<String> cartItems = cartPage.getCartItemNames();
        Assert.assertEquals(cartPage.getCartItemCount(), 2,
                "Cart should contain 2 items");
        Assert.assertTrue(cartItems.contains(item1),
                "Cart should contain: " + item1);
        Assert.assertTrue(cartItems.contains(item2),
                "Cart should contain: " + item2);
    }

    /**
     * TC11 — Verifies that removing one item from the cart leaves the other item intact.
     * Asserts count, absence of removed item, and presence of remaining item to fully validate
     * the remove operation without navigating away from the cart.
     */
    @Test(groups = {"cart", "regression"}, description = "TC11 - Remove Item via Cart Page")
    @TmsLink("TC11")
    @Story("User removes one item from the cart and the remaining item is preserved")
    @Description("Verify removing an item from inside the cart updates the cart view")
    @Severity(SeverityLevel.CRITICAL)
    public void testRemoveItemViaCartPage() {
        String item1 = CHECKOUT.getText("products", "item1");
        String item2 = CHECKOUT.getText("products", "item2");

        InventoryPage inventoryPage = loginAsStandardUser();
        inventoryPage.addProductToCart(item1);
        inventoryPage.addProductToCart(item2);

        CartPage cartPage = inventoryPage.goToCart();
        Assert.assertEquals(cartPage.getCartItemCount(), 2, "Cart should start with 2 items");

        cartPage.removeItem(item1);

        Assert.assertEquals(cartPage.getCartItemCount(), 1,
                "Cart should have 1 item after removal");
        Assert.assertFalse(cartPage.isItemInCart(item1),
                item1 + " should be removed from cart");
        Assert.assertTrue(cartPage.isItemInCart(item2),
                item2 + " should still be in cart");
    }
}
