package com.meghana.appium.utils;

import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class CommonUtils {

    private static final Logger log = LoggerFactory.getLogger(CommonUtils.class);
    private static final String SCREENSHOT_DIR = "screenshots";
    private static final Random RANDOM = new Random();

    /** Prevents instantiation — all methods are static utilities with no shared state. */
    private CommonUtils() {}

    /**
     * Captures a PNG screenshot, attaches it to the running Allure report, and saves it to disk.
     * Called from BaseTest @AfterMethod on test failure so each failed test has a visual record.
     * The PNG bytes are captured once and reused for both Allure attachment and disk write to
     * avoid taking two separate screenshots. Filenames are sanitised and timestamped to prevent
     * collisions across multiple runs.
     */
    public static Path takeScreenshot(AndroidDriver driver, String name) {
        byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);

        Allure.addAttachment(name, "image/png", new ByteArrayInputStream(png), "png");

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = name.replaceAll("[^a-zA-Z0-9_\\-]", "_") + "_" + timestamp + ".png";
        Path dir = Paths.get(SCREENSHOT_DIR);

        try {
            Files.createDirectories(dir);
            Path filePath = dir.resolve(fileName);
            Files.write(filePath, png);
            log.info("Screenshot saved: {}", filePath.toAbsolutePath());
            return filePath;
        } catch (IOException e) {
            log.error("Failed to save screenshot '{}': {}", fileName, e.getMessage());
            return null;
        }
    }

    /**
     * Blocks the current thread for the given number of seconds.
     * Use sparingly — only when an element-based wait is not feasible (e.g. waiting for
     * a background job that produces no UI change). Prefer WebDriverWaitUtils everywhere else.
     * Restores the interrupted flag if the sleep is interrupted so callers can detect it.
     */
    public static void waitForSeconds(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Sleep interrupted");
        }
    }

    /**
     * Generates a random alphanumeric string of the given length.
     * Used to produce unique input values in tests that require non-empty text fields
     * without relying on fixed strings that could collide across parallel test runs.
     */
    public static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Generates a random numeric-only string of the given length.
     * Used for postal code or phone number fields that accept digits only.
     */
    public static String generateRandomNumericString(int length) {
        String digits = "0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(digits.charAt(RANDOM.nextInt(digits.length())));
        }
        return sb.toString();
    }

    /**
     * Strips the leading "$" and parses the remainder as a double.
     * Centralised here so all price parsing uses the same logic, making it easy to change
     * if the app's currency format changes (e.g. adds commas or changes symbol).
     */
    public static double parsePriceString(String priceText) {
        if (priceText == null || priceText.isBlank()) {
            throw new IllegalArgumentException("Price text is null or blank");
        }
        return Double.parseDouble(priceText.replace("$", "").trim());
    }

    /**
     * Rounds a double to 2 decimal places using integer arithmetic to avoid floating-point drift.
     * Used when comparing calculated totals against displayed values, since price labels show
     * at most 2 decimal places and direct double comparison would fail on rounding differences.
     */
    public static double roundToTwoDecimalPlaces(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * Returns true if the list is in case-insensitive ascending order.
     * Used by testProductSortingAlphabetical to verify the "Name (A to Z)" sort option.
     * Compares adjacent pairs — the first out-of-order pair returns false immediately.
     */
    public static boolean isSortedAscending(java.util.List<String> list) {
        for (int i = 0; i < list.size() - 1; i++) {
            if (list.get(i).compareToIgnoreCase(list.get(i + 1)) > 0) return false;
        }
        return true;
    }

    /**
     * Returns true if the list is in case-insensitive descending order.
     * Used by testProductSortingAlphabetical to verify the "Name (Z to A)" sort option.
     */
    public static boolean isSortedDescending(java.util.List<String> list) {
        for (int i = 0; i < list.size() - 1; i++) {
            if (list.get(i).compareToIgnoreCase(list.get(i + 1)) < 0) return false;
        }
        return true;
    }

    /**
     * Returns true if the double list is in ascending numeric order.
     * Used by testProductSortingPriceLowToHigh to verify the "Price (low to high)" sort option.
     */
    public static boolean isSortedAscendingDouble(java.util.List<Double> list) {
        for (int i = 0; i < list.size() - 1; i++) {
            if (list.get(i) > list.get(i + 1)) return false;
        }
        return true;
    }
}
