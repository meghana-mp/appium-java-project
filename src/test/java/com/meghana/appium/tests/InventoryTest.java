package com.meghana.appium.tests;

import com.meghana.appium.base.BaseTest;
import com.meghana.appium.pages.InventoryPage;
import com.meghana.appium.utils.CommonUtils;
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
@Feature("Inventory")
public class InventoryTest extends BaseTest {

    /**
     * Loaded once at class level so JSON files are parsed a single time and shared across tests.
     */
    private static final JsonDataReader USERS    = new JsonDataReader("testdata/users.json");
    private static final JsonDataReader CHECKOUT = new JsonDataReader("testdata/checkout.json");

    /**
     * Shared helper that logs in as the standard user and returns the InventoryPage.
     * Extracted to avoid duplicating the login sequence in every inventory test method.
     */
    private InventoryPage loginAsStandardUser() {
        return getLoginPage().login(
                USERS.getText("validUser", "username"),
                USERS.getText("validUser", "password"));
    }

    /**
     * TC4 — Verifies that the inventory screen loads and displays at least one product with a name.
     * A count > 0 confirms the product list rendered; iterating names confirms no null values
     * that would indicate a data-binding failure on individual product cards.
     * Smoke: basic sanity that the app's core screen has content after login.
     */
    @Test(groups = {"inventory", "smoke"}, description = "TC4 - Dynamic Inventory Loading")
    @TmsLink("TC4")
    @Story("Product inventory loads with all items visible after login")
    @Description("Verify products load correctly on the inventory screen")
    @Severity(SeverityLevel.CRITICAL)
    public void testInventoryLoads() {
        InventoryPage inventoryPage = loginAsStandardUser();

        int count = inventoryPage.getProductCount();
        Assert.assertTrue(count > 0, "Inventory should have at least one product. Found: " + count);

        List<String> names = inventoryPage.getProductNames();
        Assert.assertFalse(names.isEmpty(), "Product names list should not be empty");
        names.forEach(name -> Assert.assertNotNull(name, "Product name should not be null"));
    }

    /**
     * TC5 — Verifies both A-Z and Z-A sort options produce correctly ordered lists.
     * sortBy("az") and sortBy("za") each wait for the list to re-render before getProductNames()
     * is called, preventing the assertion from running against the pre-sort order.
     */
    @Test(groups = {"inventory", "regression"}, description = "TC5 - Product Sorting A-Z and Z-A")
    @TmsLink("TC5")
    @Story("User can sort products alphabetically in ascending and descending order")
    @Description("Verify product sorting functionality via the filter dropdown")
    @Severity(SeverityLevel.NORMAL)
    public void testProductSortingAlphabetical() {
        InventoryPage inventoryPage = loginAsStandardUser();

        inventoryPage.sortBy("az");
        List<String> sortedAZ = inventoryPage.getProductNames();
        Assert.assertTrue(CommonUtils.isSortedAscending(sortedAZ),
                "Products should be sorted A to Z. Got: " + sortedAZ);

        inventoryPage.sortBy("za");
        List<String> sortedZA = inventoryPage.getProductNames();
        Assert.assertTrue(CommonUtils.isSortedDescending(sortedZA),
                "Products should be sorted Z to A. Got: " + sortedZA);
    }

    /**
     * TC6 — Verifies that price-low-to-high sort returns an ascending numeric sequence.
     * Prices are parsed to double before comparison so the assertion is numeric, not lexicographic
     * (lexicographic sort would put "9.99" after "29.99" when we want it before).
     */
    @Test(groups = {"inventory", "regression"}, description = "TC6 - Product Sorting Price Low to High")
    @TmsLink("TC6")
    @Story("User can sort products by price from lowest to highest")
    @Description("Verify sorting by price in ascending order")
    @Severity(SeverityLevel.NORMAL)
    public void testProductSortingPriceLowToHigh() {
        InventoryPage inventoryPage = loginAsStandardUser();

        inventoryPage.sortBy("price_low");
        List<Double> prices = inventoryPage.getProductPrices();
        Assert.assertTrue(CommonUtils.isSortedAscendingDouble(prices),
                "Products should be sorted by price low to high. Got: " + prices);
    }

    /**
     * TC7 — Verifies that adding a single item to the cart updates the badge to "1".
     * isCartBadgeDisplayed() confirms the badge appeared (it is hidden when cart is empty),
     * and getCartBadgeCount() confirms the count string matches the expected value.
     * Smoke: core cart functionality — if this breaks, checkout tests will all fail.
     */
    @Test(groups = {"inventory", "smoke"}, description = "TC7 - Add Single Item to Cart")
    @TmsLink("TC7")
    @Story("User adds a product to cart and sees the cart badge update to 1")
    @Description("Verify adding one item to the cart updates the cart badge count to 1")
    @Severity(SeverityLevel.CRITICAL)
    public void testAddSingleItemToCart() {
        InventoryPage inventoryPage = loginAsStandardUser();
        String product = CHECKOUT.getText("products", "item1");

        inventoryPage.addProductToCart(product);

        Assert.assertTrue(inventoryPage.isCartBadgeDisplayed(), "Cart badge should be visible");
        Assert.assertEquals(inventoryPage.getCartBadgeCount(), "1",
                "Cart badge should show '1' after adding one item");
    }

    /**
     * TC8 — Verifies that removing an item from the inventory page hides the cart badge.
     * First adds the item to confirm the badge appears, then removes it and checks badge disappears.
     * This two-step assertion confirms both add and remove work without navigating to the cart.
     */
    @Test(groups = {"inventory", "regression"}, description = "TC8 - Remove Item from Cart (Inventory Page)")
    @TmsLink("TC8")
    @Story("User removes a product directly from the inventory list and badge disappears")
    @Description("Verify item can be removed directly from the main inventory list")
    @Severity(SeverityLevel.NORMAL)
    public void testRemoveItemFromInventory() {
        InventoryPage inventoryPage = loginAsStandardUser();
        String product = CHECKOUT.getText("products", "item1");

        inventoryPage.addProductToCart(product);
        Assert.assertTrue(inventoryPage.isCartBadgeDisplayed(), "Badge should appear after adding item");

        inventoryPage.removeProductFromCart(product);
        Assert.assertFalse(inventoryPage.isCartBadgeDisplayed(),
                "Cart badge should disappear after removing item");
    }

    /**
     * TC9 — Verifies that adding three different items updates the badge count to "3".
     * Uses method chaining on addProductToCart() to add items sequentially and
     * asserts the badge shows the cumulative count after all three additions.
     */
    @Test(groups = {"inventory", "regression"}, description = "TC9 - Add Multiple Items to Cart")
    @TmsLink("TC9")
    @Story("User adds multiple products and cart badge reflects the correct total count")
    @Description("Verify adding 3 different items reflects correctly on the cart badge")
    @Severity(SeverityLevel.CRITICAL)
    public void testAddMultipleItemsToCart() {
        InventoryPage inventoryPage = loginAsStandardUser();

        inventoryPage
                .addProductToCart(CHECKOUT.getText("products", "item1"))
                .addProductToCart(CHECKOUT.getText("products", "item2"))
                .addProductToCart(CHECKOUT.getText("products", "item3"));

        Assert.assertEquals(inventoryPage.getCartBadgeCount(), "3",
                "Cart badge should show '3' after adding three items");
    }
}
