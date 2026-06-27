package com.meghana.appium.pages;

import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CartPage extends BasePage {

    private final By CART_TITLE       = byText("YOUR CART");
    private final By CART_ITEM        = byAccessibilityId("test-Item");
    private final By ITEM_TITLE       = byAccessibilityId("test-Item title");
    private final By ITEM_PRICE       = byAccessibilityId("test-Price");
    private final By CHECKOUT_BUTTON  = byAccessibilityId("test-CHECKOUT");
    private final By CONTINUE_SHOP    = byAccessibilityId("test-CONTINUE SHOPPING");

    /** Wires up the shared driver, waitUtils, and gestureUtils via BasePage. */
    public CartPage(AndroidDriver driver) {
        super(driver);
    }

    /**
     * Waits for the "YOUR CART" title to appear, confirming the cart screen is fully loaded.
     * Uses waitForVisibleDismissingDialogs to tolerate ANR popups during page transitions.
     */
    @Override
    public void waitForPageLoad() {
        waitForVisibleDismissingDialogs(CART_TITLE);
        log.info("CartPage ready");
    }

    /**
     * Extracts the name of each item currently in the cart.
     * Uses a dual-strategy per-item approach because the "test-Item title" element can be
     * either a leaf TextView (direct text) or a ViewGroup wrapper (text in a child TextView)
     * depending on the UiAutomator2 version and layout pass.
     *
     * Strategy 1: Find elements with content-desc "test-Item title" within each item container.
     *   - If the element has non-empty getText(), use it directly.
     *   - If getText() is empty (ViewGroup wrapper), search for the first non-empty child TextView.
     *
     * Strategy 2 (fallback): If strategy 1 yields no name, scan all descendant elements whose
     *   @text is longer than 3 chars, does not start with "$" (price), and is not "REMOVE".
     *   This catches edge cases where the accessibility ID is missing or the layout changes.
     */
    @Step("Get cart item names")
    public List<String> getCartItemNames() {
        List<String> names = new ArrayList<>();
        for (WebElement item : findAll(CART_ITEM)) {
            // Strategy 1: element with content-desc='test-Item title' (leaf TextView on some versions)
            String name = "";
            for (WebElement el : item.findElements(ITEM_TITLE)) {
                name = el.getText().trim();
                if (name.isEmpty()) {
                    // ViewGroup wrapper — get text from first non-empty child TextView
                    name = el.findElements(By.className("android.widget.TextView"))
                            .stream().map(e -> e.getText().trim())
                            .filter(t -> !t.isEmpty()).findFirst().orElse("");
                }
                if (!name.isEmpty()) break;
            }
            // Strategy 2: first text-bearing descendant that isn't qty/price/action
            if (name.isEmpty()) {
                name = item.findElements(By.xpath(
                        ".//*[string-length(@text)>3 and not(starts-with(@text,'$')) and @text!='REMOVE']"))
                        .stream().map(e -> e.getText().trim())
                        .filter(t -> !t.isEmpty()).findFirst().orElse("");
            }
            if (!name.isEmpty()) names.add(name);
        }
        return names;
    }

    /**
     * Returns the number of item container elements in the cart.
     * Uses findAll (no explicit wait) because waitForPageLoad has already confirmed the page
     * is ready before any test calls this method.
     */
    @Step("Get cart item count")
    public int getCartItemCount() {
        return findAll(CART_ITEM).size();
    }

    /**
     * Reads and parses the price of each item currently in the cart.
     * Strips "$" before parsing to double for numeric comparisons.
     */
    @Step("Get cart item prices")
    public List<Double> getCartItemPrices() {
        return findAll(ITEM_PRICE).stream()
                .map(e -> Double.parseDouble(e.getText().replace("$", "").trim()))
                .collect(Collectors.toList());
    }

    /**
     * Clicks the REMOVE button for the named item from the cart.
     * The XPath predicate scopes REMOVE to the specific cart item card that contains the
     * product name, preventing accidental removal of a different item when multiple items exist.
     * Works regardless of whether the title is a leaf TextView or wrapped in a ViewGroup.
     */
    @Step("Remove item from cart: {productName}")
    public CartPage removeItem(String productName) {
        By removeBtn = By.xpath(
                "//*[@content-desc='test-Item'][.//*[@text='" + productName + "']]" +
                "//*[@content-desc='test-REMOVE']");
        click(removeBtn);
        return this;
    }

    /**
     * Returns true if a cart item card containing the given product name is present.
     * Uses findElements (returns empty list, no exception) for safe presence checking.
     */
    public boolean isItemInCart(String productName) {
        By item = By.xpath(
                "//*[@content-desc='test-Item'][.//*[@text='" + productName + "']]");
        return !driver.findElements(item).isEmpty();
    }

    /**
     * Scrolls to and clicks the CHECKOUT button, then waits for CheckoutInfoPage to load.
     * swipeToElement is used because the CHECKOUT button may be below the fold when many
     * items are in the cart, requiring a scroll before the button becomes clickable.
     */
    @Step("Proceed to Checkout")
    public CheckoutInfoPage proceedToCheckout() {
        gestureUtils.swipeToElement(CHECKOUT_BUTTON, 3);
        click(CHECKOUT_BUTTON);
        CheckoutInfoPage page = new CheckoutInfoPage(driver);
        page.waitForPageLoad();
        return page;
    }

    /**
     * Clicks CONTINUE SHOPPING and waits for InventoryPage to load.
     * Used to navigate back to the product list without clearing the cart.
     */
    @Step("Continue Shopping")
    public InventoryPage continueShopping() {
        click(CONTINUE_SHOP);
        InventoryPage page = new InventoryPage(driver);
        page.waitForPageLoad();
        return page;
    }
}
