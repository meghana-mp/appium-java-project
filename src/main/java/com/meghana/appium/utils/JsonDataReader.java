package com.meghana.appium.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Reads JSON test data files from src/test/resources/testdata/ on the classpath.
 * Uses Jackson ObjectMapper; returns JsonNode for flexible key-based access.
 *
 * Usage:
 *   JsonDataReader reader = new JsonDataReader("testdata/users.json");
 *   String username = reader.getText("validUser", "username");
 *   Object[][] table = reader.asDataProviderArray("users", "username", "password");
 */
public class JsonDataReader {

    private static final Logger log = LoggerFactory.getLogger(JsonDataReader.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JsonNode root;

    /**
     * Parses the JSON file at the given classpath path into a JsonNode tree at construction.
     * Loading at construction (not on each access) means the file is read once per test class,
     * not on every data access call — keeping tests fast and reducing I/O overhead.
     * Throws RuntimeException if the file is not found or contains invalid JSON so misconfigured
     * test data fails immediately rather than producing silent null values.
     */
    public JsonDataReader(String classpathResourcePath) {
        try (InputStream is = JsonDataReader.class.getClassLoader()
                .getResourceAsStream(classpathResourcePath)) {
            if (is == null) {
                throw new RuntimeException("Test data file not found on classpath: " + classpathResourcePath);
            }
            root = MAPPER.readTree(is);
            log.info("Test data loaded: {}", classpathResourcePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON: " + classpathResourcePath, e);
        }
    }

    /**
     * Navigates the JSON tree by chained keys (object field names or array indices as strings).
     * Handles mixed object/array paths by checking isArray() at each step.
     * Throws if any key in the chain produces a null node so callers get a precise error message
     * rather than a NullPointerException with no indication of which key was missing.
     * Example: get("validUser", "username") or get("items", "0", "name")
     */
    public JsonNode get(String... keys) {
        JsonNode node = root;
        for (String key : keys) {
            if (node == null) break;
            node = node.isArray() ? node.get(Integer.parseInt(key)) : node.get(key);
        }
        if (node == null) {
            throw new RuntimeException("Key path not found in JSON: " + String.join(" → ", keys));
        }
        return node;
    }

    /**
     * Returns the node value as a String. Delegates to get(keys) then asText().
     * The primary accessor used by tests to fetch usernames, passwords, error messages, etc.
     */
    public String getText(String... keys) {
        return get(keys).asText();
    }

    /**
     * Returns the node value as a double. Used for price or numeric comparisons in test data.
     */
    public double getDouble(String... keys) {
        return get(keys).asDouble();
    }

    /**
     * Returns the node value as an integer. Used for count or index values in test data.
     */
    public int getInt(String... keys) {
        return get(keys).asInt();
    }

    /**
     * Returns the node value as a boolean. Used for flag fields in test data.
     */
    public boolean getBoolean(String... keys) {
        return get(keys).asBoolean();
    }

    /**
     * Converts a JSON array into a TestNG-compatible Object[][] for use with @DataProvider.
     * Each array element becomes one row; fieldNames controls which fields are extracted per row
     * and in what column order. This avoids writing separate model classes for small test data sets.
     *
     * Example:
     *   JSON: { "loginCases": [ { "username": "u1", "password": "p1" }, ... ] }
     *   reader.asDataProviderArray("loginCases", "username", "password")
     *   returns { {"u1", "p1"}, ... }
     */
    public Object[][] asDataProviderArray(String arrayKey, String... fieldNames) {
        JsonNode array = get(arrayKey);
        if (!array.isArray()) {
            throw new RuntimeException("JSON node '" + arrayKey + "' is not an array");
        }
        Object[][] data = new Object[array.size()][fieldNames.length];
        for (int i = 0; i < array.size(); i++) {
            for (int j = 0; j < fieldNames.length; j++) {
                data[i][j] = array.get(i).get(fieldNames[j]).asText();
            }
        }
        return data;
    }

    /**
     * Returns the root JsonNode for callers that need to navigate the tree manually.
     */
    public JsonNode getRoot() {
        return root;
    }
}
