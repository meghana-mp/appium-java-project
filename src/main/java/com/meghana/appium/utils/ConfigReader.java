package com.meghana.appium.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {

    private static final Logger log = LoggerFactory.getLogger(ConfigReader.class);
    private static ConfigReader instance;
    private final Properties properties;

    /**
     * Loads config.properties from the classpath at construction time.
     * Private to enforce singleton access — only getInstance() may create an instance.
     * Throws RuntimeException immediately if the file is missing or unreadable so tests
     * fail fast with a clear message rather than producing cryptic NullPointerExceptions later.
     */
    private ConfigReader() {
        properties = new Properties();
        try (InputStream is = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (is == null) {
                throw new RuntimeException("config.properties not found on classpath");
            }
            properties.load(is);
            log.info("config.properties loaded successfully");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    /**
     * Returns the singleton instance, creating it on first call using double-checked locking.
     * Thread-safe without synchronising on every read — only the initial creation is guarded,
     * so concurrent tests don't contend on this method after the first access.
     */
    public static ConfigReader getInstance() {
        if (instance == null) {
            synchronized (ConfigReader.class) {
                if (instance == null) {
                    instance = new ConfigReader();
                }
            }
        }
        return instance;
    }

    /**
     * Returns the trimmed value for the given key.
     * Throws if the key is absent — missing required config is always a test setup error,
     * not a recoverable state, so failing loudly prevents hard-to-diagnose downstream errors.
     */
    public String get(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Property not found in config.properties: " + key);
        }
        return value.trim();
    }

    /**
     * Returns the value for the given key, or defaultValue if the key is absent.
     * Used for optional capabilities that have sensible defaults (e.g. timeout values, flags).
     */
    public String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue).trim();
    }

    /**
     * Parses the property value as a boolean ("true"/"false").
     * Used for Appium capability flags like noReset, ensureWebviewsHavePages, nativeWebScreenshot.
     */
    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    /**
     * Parses the property value as an integer.
     * Used for numeric capabilities like newCommandTimeout and implicitWaitSeconds.
     */
    public int getInt(String key) {
        return Integer.parseInt(get(key));
    }
}
