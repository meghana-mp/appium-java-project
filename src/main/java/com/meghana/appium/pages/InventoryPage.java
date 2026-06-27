package com.meghana.appium.pages;

import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

public class InventoryPage extends BasePage {

    private final By PRODUCTS_HEADER = byAccessibilityId("test-PRODUCTS");
    private final By PRODUCT_ITEM    = byAccessibilityId("test-Item");
    private final By ITEM_TITLE      = byAccessibilityId("test-Item title");
    private final By ITEM_PRICE      = byAccessibilityId("test-Price");
    private final By CART_ICON       = byAccessibilityId("test-Cart");
    /**
     * XPath descends from the Cart icon into its badge overlay TextView.
     * Scoped to test-Cart's child hierarchy to avoid matching unrelated TextViews.
     */
    // Uses // (descendant axis) instead of strict parent→child /android.view.ViewGroup/android.widget.TextView
    // because the badge overlay nesting depth varies across Android API levels and screen densities.
    private final By CART_BADGE      = By.xpath(
            "//*[@content-desc='test-Cart']//android.widget.TextView");
    private final By SORT_BUTTON     = byAccessibilityId("test-Modal Selector Button");
    private final By MENU_BUTTON     = byAccessibilityId("test-Menu");

    private final By SORT_AZ         = byText("Name (A to Z)");
    private final By SORT_ZA         = byText("Name (Z to A)");
    private final By SORT_PRICE_LOW  = byText("Price (low to high)");
    private final By SORT_PRICE_HIGH = byText("Price (high to low)");

    /** Wires up the shared driver, waitUtils, and gestureUtils via BasePage. */
    public InventoryPage(AndroidDriver driver) {
        super(driver);
    }

    /**
     * Waits for the PRODUCTS header to appear, confirming the inventory screen is loaded.
     * Uses waitForVisibleDismissingDialogs to tolerate ANR popups during app startup.
     */
    @Override
    public void waitForPageLoad() {
        waitForVisibleDismissingDialogs(PRODUCTS_HEADER);
        log.info("InventoryPage ready");
    }

    /**
     * Collects the displayed text from all visible product title elements.
     * Uses a stream because the count of visible items may vary depending on scroll position.
     */
    @Step("Get all product names")
    public List<String> getProductNames() {
        return findAll(ITEM_TITLE).stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }

    /**
     * Reads and parses the price text of all visible product items.
     * Strips "$" before parsing to double so price comparisons and sort verifications work numerically.
     */
    @Step("Get all product prices")
    public List<Double> getProductPrices() {
        return findAll(ITEM_PRICE).stream()
                .map(e -> Double.parseDouble(e.getText().replace("$", "").trim()))
                .collect(Collectors.toList());
    }

    /**
     * Returns the total number of product cards on the inventory screen.
     * Waits for at least 1 item to ensure the list has finished rendering before counting.
     */
    @Step("Get product count")
    public int getProductCount() {
        return waitUtils.waitForElementCount(PRODUCT_ITEM, 1).size();
    }

    /**
     * Opens the sort dropdown and selects the option matching the given key.
     * After clicking the option, waits for the product list to contain at least 1 item
     * to ensure the list has re-rendered with the new sort order before the caller reads names/prices.
     * Without the waitForElementCount call, subsequent getProductNames() may return stale data.
     */
    @Step("Sort by: {option}")
    public InventoryPage sortBy(String option) {
        click(SORT_BUTTON);
        By locator = switch (option.toLowerCase()) {
            case "az"         -> SORT_AZ;
            case "za"         -> SORT_ZA;
            case "price_low"  -> SORT_PRICE_LOW;
            case "price_high" -> SORT_PRICE_HIGH;
            default -> throw new IllegalArgumentException("Unknown sort option: " + option);
        };
        click(locator);
        waitUtils.waitForElementCount(PRODUCT_ITEM, 1);
        return this;
    }

    /**
     * Scrolls to and taps the ADD TO CART button for the named product.
     * The XPath predicate scopes the button search within the product card that contains
     * the matching product name, so multiple products with ADD TO CART buttons don't collide.
     * swipeToElement scrolls the product into view first in case it is below the fold.
     */
    @Step("Add product to cart: {productName}")
    public InventoryPage addProductToCart(String productName) {
        By addBtn = By.xpath(
                "//*[@content-desc='test-Item'][.//*[@text='" + productName + "']]" +
                "//*[@content-desc='test-ADD TO CART']");
        gestureUtils.swipeToElement(addBtn, 5);
        click(addBtn);
        return this;
    }

    /**
     * Clicks the REMOVE button for the named product from the inventory screen.
     * Uses the same scoped XPath pattern as addProductToCart to target only the correct item card.
     */
    @Step("Remove product from cart: {productName}")
    public InventoryPage removeProductFromCart(String productName) {
        By removeBtn = By.xpath(
                "//*[@content-desc='test-Item'][.//*[@text='" + productName + "']]" +
                "//*[@content-desc='test-REMOVE']");
        click(removeBtn);
        return this;
    }

    /**
     * Returns the text shown on the cart badge (e.g. "1", "3").
     * The badge is a child TextView of the Cart icon's ViewGroup overlay.
     */
    @Step("Get cart badge count")
    public String getCartBadgeCount() {
        return getText(CART_BADGE);
    }

    /**
     * Returns true if the cart badge is currently visible.
     * The badge only appears when at least one item has been added to the cart.
     */
    public boolean isCartBadgeDisplayed() {
        return isDisplayed(CART_BADGE);
    }

    /**
     * Taps the cart icon and waits for the CartPage to load.
     * Navigates from the inventory to the cart to review selected items.
     */
    @Step("Navigate to Cart")
    public CartPage goToCart() {
        click(CART_ICON);
        CartPage page = new CartPage(driver);
        page.waitForPageLoad();
        return page;
    }

    /**
     * Taps the hamburger menu icon and waits for the SidebarPage to load.
     * Opens the navigation drawer containing logout, all items, and reset options.
     */
    @Step("Open sidebar menu")
    public SidebarPage openMenu() {
        click(MENU_BUTTON);
        SidebarPage page = new SidebarPage(driver);
        page.waitForPageLoad();
        return page;
    }
}
