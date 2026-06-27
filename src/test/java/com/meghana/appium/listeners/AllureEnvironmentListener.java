package com.meghana.appium.listeners;

import com.meghana.appium.utils.ConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * TestNG ISuiteListener that runs once before the suite starts.
 * Writes environment.properties to allure-results so the Allure report shows
 * device/platform/app info in the Environment panel. Also copies categories.json
 * from the classpath to allure-results so failures are automatically categorised.
 */
public class AllureEnvironmentListener implements ISuiteListener {

    private static final Logger log = LoggerFactory.getLogger(AllureEnvironmentListener.class);

    /**
     * Fires once before the suite starts. Writes both allure helper files before any
     * test result JSON files are created, so Allure picks them up in the same directory scan.
     */
    @Override
    public void onStart(ISuite suite) {
        Path resultsDir = resolveAllureResultsDir();
        writeEnvironmentProperties(resultsDir);
        copyCategoriesJson(resultsDir);
    }

    @Override
    public void onFinish(ISuite suite) {
        // nothing needed post-suite
    }

    /**
     * Reads the allure.results.directory system property (set by Surefire via pom.xml).
     * Falls back to target/allure-results when running outside Maven (e.g. IDE).
     */
    private Path resolveAllureResultsDir() {
        String dir = System.getProperty("allure.results.directory", "target/allure-results");
        return Paths.get(dir);
    }

    /**
     * Writes environment.properties containing device, platform, and app details.
     * Values are read from config.properties via ConfigReader so there is a single source
     * of truth — changing the device or server URL in config auto-updates the report.
     */
    private void writeEnvironmentProperties(Path resultsDir) {
        try {
            Files.createDirectories(resultsDir);
            ConfigReader config = ConfigReader.getInstance();

            Properties env = new Properties();
            env.setProperty("Platform",       config.get("platformName"));
            env.setProperty("Automation",     config.get("automationName"));
            env.setProperty("Device",         config.get("deviceName"));
            env.setProperty("App.Package",    config.get("appPackage"));
            env.setProperty("App.Version",    "2.7.1");
            env.setProperty("Appium.Server",  config.get("appiumServerUrl"));
            env.setProperty("No.Reset",       config.get("noReset"));

            Path envFile = resultsDir.resolve("environment.properties");
            try (OutputStream out = Files.newOutputStream(envFile)) {
                env.store(out, null);
            }
            log.info("Allure environment.properties written: {}", envFile.toAbsolutePath());
        } catch (Exception e) {
            log.warn("Could not write Allure environment.properties: {}", e.getMessage());
        }
    }

    /**
     * Copies categories.json from the test classpath to allure-results.
     * Allure reads this file to classify failures into labelled groups (product defects,
     * infrastructure issues, etc.) visible in the Categories tab of the report.
     */
    private void copyCategoriesJson(Path resultsDir) {
        try {
            Files.createDirectories(resultsDir);
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("categories.json")) {
                if (is == null) {
                    log.warn("categories.json not found on classpath — skipping");
                    return;
                }
                Path dest = resultsDir.resolve("categories.json");
                Files.copy(is, dest, StandardCopyOption.REPLACE_EXISTING);
                log.info("Allure categories.json copied: {}", dest.toAbsolutePath());
            }
        } catch (IOException e) {
            log.warn("Could not copy Allure categories.json: {}", e.getMessage());
        }
    }
}
