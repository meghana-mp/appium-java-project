package com.meghana.appium.utils;

import com.meghana.appium.driver.DriverManager;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;

/**
 * All touch gestures use the W3C Actions API (PointerInput + Sequence).
 * Compatible with Appium 2.0 / UiAutomator2. The deprecated TouchAction API
 * was removed in Appium Java Client 8+.
 */
public class GestureUtils {

    private static final Logger log = LoggerFactory.getLogger(GestureUtils.class);

    public enum Direction { UP, DOWN, LEFT, RIGHT }

    private final AndroidDriver driver;
    private final WebDriverWaitUtils waitUtils;

    /**
     * Stores the driver and creates a waitUtils instance used by swipeToElement
     * to check element visibility between scroll attempts.
     */
    public GestureUtils(AndroidDriver driver) {
        this.driver = driver;
        this.waitUtils = new WebDriverWaitUtils(driver);
    }

    // ─── Tap ────────────────────────────────────────────────────────────────

    /**
     * Performs a single tap at absolute screen coordinates using the W3C PointerInput API.
     * Creates a TOUCH pointer, moves to position, presses down, then lifts — simulating
     * a real finger tap without using the deprecated TouchAction class.
     */
    public void tap(int x, int y) {
        log.debug("Tap at ({}, {})", x, y);
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence seq = new Sequence(finger, 0)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(seq));
    }

    /**
     * Taps the center of the given element by computing its midpoint from the element's bounds.
     * Delegates to tap(int, int) so all tap logic stays in one place.
     */
    public void tap(WebElement element) {
        Rectangle rect = element.getRect();
        tap(rect.x + rect.width / 2, rect.y + rect.height / 2);
    }

    // ─── Double Tap ──────────────────────────────────────────────────────────

    /**
     * Performs two rapid taps at the same coordinates with a 100ms gap between them.
     * The short pause ensures the OS recognises the two separate down/up events as a double-tap
     * rather than a long press.
     */
    public void doubleTap(int x, int y) {
        log.debug("Double tap at ({}, {})", x, y);
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence seq = new Sequence(finger, 0)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger.createPointerMove(Duration.ofMillis(100), PointerInput.Origin.viewport(), x, y))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(seq));
    }

    /**
     * Double-taps the center of the given element. Delegates to doubleTap(int, int).
     */
    public void doubleTap(WebElement element) {
        Rectangle rect = element.getRect();
        doubleTap(rect.x + rect.width / 2, rect.y + rect.height / 2);
    }

    // ─── Long Press ──────────────────────────────────────────────────────────

    /**
     * Holds a finger down at coordinates for the specified duration before releasing.
     * The holdDuration moves the pointer with zero displacement — keeping position constant —
     * which UiAutomator2 recognises as a long press.
     */
    public void longPress(int x, int y, Duration holdDuration) {
        log.debug("Long press at ({}, {}) for {}ms", x, y, holdDuration.toMillis());
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence seq = new Sequence(finger, 0)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger.createPointerMove(holdDuration, PointerInput.Origin.viewport(), x, y))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(seq));
    }

    /**
     * Long-presses the center of an element for the specified duration.
     */
    public void longPress(WebElement element, Duration holdDuration) {
        Rectangle rect = element.getRect();
        longPress(rect.x + rect.width / 2, rect.y + rect.height / 2, holdDuration);
    }

    /**
     * Long-presses an element using the default 1500ms hold duration — long enough to trigger
     * context menus and drag handles on most Android apps.
     */
    public void longPress(WebElement element) {
        longPress(element, Duration.ofMillis(1500));
    }

    // ─── Swipe ──────────────────────────────────────────────────────────────

    /**
     * Performs a linear finger swipe from (startX, startY) to (endX, endY) over the given duration.
     * Duration controls swipe speed — slower swipes are more reliable for scroll detection
     * on list views; faster swipes work better for dismissing drawers.
     */
    public void swipe(int startX, int startY, int endX, int endY, Duration duration) {
        log.debug("Swipe ({},{}) → ({},{}) in {}ms", startX, startY, endX, endY, duration.toMillis());
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence seq = new Sequence(finger, 0)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger.createPointerMove(duration, PointerInput.Origin.viewport(), endX, endY))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(seq));
    }

    /**
     * Swipes with the default 600ms duration — balanced between speed and reliability.
     */
    public void swipe(int startX, int startY, int endX, int endY) {
        swipe(startX, startY, endX, endY, Duration.ofMillis(600));
    }

    // ─── Scroll (screen-relative fraction) ──────────────────────────────────

    /**
     * Scrolls the viewport in the given direction by a fraction of the screen size.
     * Coordinates are calculated from the current screen dimensions so the gesture works
     * on any resolution without hardcoded pixel values.
     * scrollFraction (0.0–1.0) controls how far to scroll per gesture — 0.5 = half screen.
     * E.g. Direction.DOWN with 0.5 scrolls half the screen upward (content moves up = scrolling down).
     */
    public void scroll(Direction direction, double scrollFraction) {
        Dimension size = driver.manage().window().getSize();
        int centerX = size.width / 2;
        int centerY = size.height / 2;
        int deltaX = (int) (size.width * scrollFraction);
        int deltaY = (int) (size.height * scrollFraction);

        int startX, startY, endX, endY;
        switch (direction) {
            case DOWN -> {
                startX = centerX; startY = centerY + deltaY / 2;
                endX   = centerX; endY   = centerY - deltaY / 2;
            }
            case UP -> {
                startX = centerX; startY = centerY - deltaY / 2;
                endX   = centerX; endY   = centerY + deltaY / 2;
            }
            case LEFT -> {
                startX = centerX + deltaX / 2; startY = centerY;
                endX   = centerX - deltaX / 2; endY   = centerY;
            }
            case RIGHT -> {
                startX = centerX - deltaX / 2; startY = centerY;
                endX   = centerX + deltaX / 2; endY   = centerY;
            }
            default -> throw new IllegalArgumentException("Unknown direction: " + direction);
        }
        log.debug("Scroll {} (fraction={})", direction, scrollFraction);
        swipe(startX, startY, endX, endY, Duration.ofMillis(600));
    }

    /** Scrolls down by half the screen height. */
    public void scrollDown() {
        scroll(Direction.DOWN, 0.5);
    }

    /** Scrolls up by half the screen height. */
    public void scrollUp() {
        scroll(Direction.UP, 0.5);
    }

    // ─── Swipe to Element ────────────────────────────────────────────────────

    /**
     * Scrolls DOWN until the element identified by locator becomes visible, up to maxSwipes attempts.
     * After each failed visibility check, dismisses any system dialog then scrolls down 40% of screen.
     * Used when buttons (CHECKOUT, FINISH) are below the fold on smaller emulator screens.
     * Throws RuntimeException if the element is not found after all swipes.
     */
    public WebElement swipeToElement(By locator, int maxSwipes) {
        for (int i = 0; i < maxSwipes; i++) {
            try {
                WebElement el = waitUtils.waitForVisible(locator, 2);
                log.debug("Element found after {} swipe(s): {}", i, locator);
                return el;
            } catch (Exception e) {
                DriverManager.dismissSystemDialogs();
                log.debug("Element not visible — swiping down (attempt {}/{})", i + 1, maxSwipes);
                scroll(Direction.DOWN, 0.4);
            }
        }
        throw new RuntimeException("Element not found after " + maxSwipes + " swipes: " + locator);
    }

    /**
     * Same as swipeToElement(By, int) with a default maximum of 5 swipes.
     */
    public WebElement swipeToElement(By locator) {
        return swipeToElement(locator, 5);
    }

    // ─── Swipe Element Into View (from top) ─────────────────────────────────

    /**
     * Scrolls UP until the element becomes visible, up to maxSwipes attempts.
     * Used to scroll back toward the top of a list after it has been scrolled down during a test.
     */
    public WebElement swipeUpToElement(By locator, int maxSwipes) {
        for (int i = 0; i < maxSwipes; i++) {
            try {
                return waitUtils.waitForVisible(locator, 2);
            } catch (Exception e) {
                scroll(Direction.UP, 0.4);
            }
        }
        throw new RuntimeException("Element not found after " + maxSwipes + " upward swipes: " + locator);
    }

    // ─── Pinch / Zoom ────────────────────────────────────────────────────────

    /**
     * Two-finger pinch or zoom gesture centered on (centerX, centerY).
     * Uses two independent PointerInput sequences (finger1, finger2) performed simultaneously.
     * zoomIn=true: fingers spread outward (zoom in); zoomIn=false: fingers pinch inward (zoom out).
     * distance controls the initial separation between the two fingers in pixels.
     */
    public void pinchOrZoom(int centerX, int centerY, int distance, boolean zoomIn) {
        int startDelta = zoomIn ? distance / 4 : distance / 2;
        int endDelta   = zoomIn ? distance / 2 : distance / 4;

        PointerInput finger1 = new PointerInput(PointerInput.Kind.TOUCH, "finger1");
        PointerInput finger2 = new PointerInput(PointerInput.Kind.TOUCH, "finger2");

        Sequence seq1 = new Sequence(finger1, 0)
                .addAction(finger1.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), centerX - startDelta, centerY))
                .addAction(finger1.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger1.createPointerMove(Duration.ofMillis(600), PointerInput.Origin.viewport(), centerX - endDelta, centerY))
                .addAction(finger1.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        Sequence seq2 = new Sequence(finger2, 0)
                .addAction(finger2.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), centerX + startDelta, centerY))
                .addAction(finger2.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger2.createPointerMove(Duration.ofMillis(600), PointerInput.Origin.viewport(), centerX + endDelta, centerY))
                .addAction(finger2.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(java.util.List.of(seq1, seq2));
    }

    /** Zooms in at the given center coordinates with a default 200px finger separation. */
    public void zoomIn(int centerX, int centerY) {
        pinchOrZoom(centerX, centerY, 200, true);
    }

    /** Pinches out (zooms out) at the given center coordinates with a default 200px finger separation. */
    public void pinchOut(int centerX, int centerY) {
        pinchOrZoom(centerX, centerY, 200, false);
    }
}
