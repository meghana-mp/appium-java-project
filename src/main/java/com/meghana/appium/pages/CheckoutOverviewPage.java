package com.meghana.appium.pages;

import com.meghana.appium.utils.CommonUtils;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

public class CheckoutOverviewPage extends BasePage {

    private final By OVERVIEW_TITLE  = byText("CHECKOUT: OVERVIEW");
    private final By ITEM_TITLE      = byAccessibilityId("test-Item title");
    private final By ITEM_PRICE      = byAccessibilityId("test-Price");
    /**
     * XPath text-contains locators for the price summary labels.
     * These labels render as plain TextViews without accessibility IDs, so XPath text matching
     * is the only reliable locator strategy for subtotal, tax, and total.
     */
    private final By SUBTOTAL_LABEL  = By.xpath("//*[contains(@text,'Item total:')]");
    private final By TAX_LABEL       = By.xpath("//*[contains(@text,'Tax:')]");
    private final By TOTAL_LABEL     = By.xpath("//*[contains(@text,'Total:')]");
    private final By FINISH_BUTTON   = byAccessibilityId("test-FINISH");
    private final By CANCEL_BUTTON   = byAccessibilityId("test-CANCEL");

    /** Wires up the shared driver, waitUtils, and gestureUtils via BasePage. */
    public CheckoutOverviewPage(AndroidDriver driver) {
        super(driver);
    }

    /**
     * Waits for the "CHECKOUT: OVERVIEW" header to appear, confirming the overview screen is loaded.
     * Uses waitForVisibleDismissingDialogs to tolerate ANR popups during page transitions.
     */
    @Override
    public void waitForPageLoad() {
        waitForVisibleDismissingDialogs(OVERVIEW_TITLE);
        log.info("CheckoutOverviewPage ready");
    }

    /**
     * Returns the names of all items shown on the order summary screen.
     * Used to verify that items added to the cart match what appears in the order review.
     */
    @Step("Get order item names")
    public List<String> getItemNames() {
        return findAll(ITEM_TITLE).stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }

    /**
     * Reads and parses the item subtotal from the "Item total:" label.
     * Strips the label prefix before delegating to CommonUtils.parsePriceString to extract
     * the numeric value — centralised parsing avoids duplicating "$" strip logic.
     */
    @Step("Get item subtotal")
    public double getItemTotal() {
        String raw = getText(SUBTOTAL_LABEL);
        return CommonUtils.parsePriceString(raw.replace("Item total:", "").trim());
    }

    /**
     * Reads and parses the tax amount from the "Tax:" label.
     * Strips the label prefix then delegates to CommonUtils.parsePriceString.
     */
    @Step("Get tax amount")
    public double getTax() {
        String raw = getText(TAX_LABEL);
        return CommonUtils.parsePriceString(raw.replace("Tax:", "").trim());
    }

    /**
     * Reads and parses the grand total from the "Total:" label.
     * Note: TOTAL_LABEL uses contains(@text,'Total:') which also matches "Item total:" — this
     * works because XPath returns the first matching element, which is the Total row, not the
     * Item total row, given the DOM order on this screen.
     */
    @Step("Get grand total")
    public double getTotal() {
        String raw = getText(TOTAL_LABEL);
        return CommonUtils.parsePriceString(raw.replace("Total:", "").trim());
    }

    /**
     * Verifies that the displayed total equals item subtotal + tax, both rounded to 2 decimal places.
     * Rounding is applied to both sides before comparison to avoid floating-point precision errors
     * that would cause equality checks to fail on values like 0.1 + 0.2 ≠ 0.3 in IEEE 754.
     */
    @Step("Verify total = subtotal + tax")
    public boolean isTotalCorrect() {
        double expected = CommonUtils.roundToTwoDecimalPlaces(getItemTotal() + getTax());
        double actual   = CommonUtils.roundToTwoDecimalPlaces(getTotal());
        log.info("Expected total: {}, Actual total: {}", expected, actual);
        return Double.compare(expected, actual) == 0;
    }

    /**
     * Scrolls to and clicks the FINISH button, then waits for CheckoutCompletePage to load.
     * swipeToElement is used because FINISH may be below the fold on screens with many order items.
     */
    @Step("Tap Finish")
    public CheckoutCompletePage tapFinish() {
        gestureUtils.swipeToElement(FINISH_BUTTON, 3);
        click(FINISH_BUTTON);
        CheckoutCompletePage page = new CheckoutCompletePage(driver);
        page.waitForPageLoad();
        return page;
    }

    /**
     * Clicks CANCEL and waits for the InventoryPage to load.
     * Allows the user to abandon checkout and return to the product list.
     */
    @Step("Cancel — return to inventory")
    public InventoryPage cancel() {
        click(CANCEL_BUTTON);
        InventoryPage page = new InventoryPage(driver);
        page.waitForPageLoad();
        return page;
    }
}
