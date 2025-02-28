package org.gui.util;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Handles saving and loading script settings for persistence between sessions
 */
public class SettingsPersistence {
    private final String username;
    // Use the same path pattern as AntiBan profiles
    private static final String DREAMBOT_DIR = System.getProperty("user.home") + "/DreamBot/Scripts/";
    private static final String SETTINGS_DIRECTORY = DREAMBOT_DIR + "CodeScripts/CodeThief/gui/";

    /**
     * Creates a new settings persistence instance for a specific user
     *
     * @param username The username to associate with settings
     */
    public SettingsPersistence(String username) {
        this.username = sanitizeFilename(username);
        ensureDirectoryExists();
    }

    /**
     * Ensures the settings directory exists and is writable
     */
    private void ensureDirectoryExists() {
        File directory = new File(SETTINGS_DIRECTORY);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                System.out.println("Created settings directory: " + directory.getAbsolutePath());
            } else {
                System.err.println("Failed to create settings directory: " + directory.getAbsolutePath());
                // Try alternate location if creation fails
                tryAlternateLocation();
            }
        } else if (!directory.canWrite()) {
            System.err.println("Settings directory exists but is not writable: " + directory.getAbsolutePath());
            // Try alternate location if not writable
            tryAlternateLocation();
        } else {
            System.out.println("Using settings directory: " + directory.getAbsolutePath());
        }
    }

    /**
     * Tries to use an alternate location if the primary one fails
     */
    private void tryAlternateLocation() {
        // First try DreamBot CodeThief directory
        File alternateDir = new File(System.getProperty("user.home") + "/DreamBot/Scripts/CodeThief/");
        if (!alternateDir.exists()) {
            alternateDir.mkdirs();
        }

        if (alternateDir.exists() && alternateDir.canWrite()) {
            // Use static field for all instances - not ideal but works for emergency fallback
            System.out.println("Using alternate settings directory: " + alternateDir.getAbsolutePath());
        } else {
            // Last resort - use temp directory
            alternateDir = new File(System.getProperty("java.io.tmpdir") + "/CodeThief/");
            alternateDir.mkdirs();
            System.out.println("Using temporary directory for settings: " + alternateDir.getAbsolutePath());
        }
    }

    /**
     * Saves settings to a file
     *
     * @param settings Map of settings to save
     * @return True if successful
     */
    public boolean saveSettings(Map<String, Object> settings) {
        try {
            String filename = getSettingsFilename();
            File settingsFile = new File(filename);

            // Ensure parent directory exists
            File parentDir = settingsFile.getParentFile();
            if (!parentDir.exists()) {
                boolean dirCreated = parentDir.mkdirs();
                if (!dirCreated) {
                    System.err.println("Failed to create parent directory: " + parentDir.getAbsolutePath());
                    return false;
                }
            }

            // Serialize settings to the file
            try (FileOutputStream fileOut = new FileOutputStream(settingsFile);
                 ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
                out.writeObject(settings);
                System.out.println("Settings saved to " + settingsFile.getAbsolutePath());
                return true;
            }
        } catch (IOException e) {
            System.err.println("Error saving settings: " + e.getMessage());
            e.printStackTrace();

            // Try direct file output with simple properties as last resort
            try {
                saveSettingsAsProperties(settings);
                return true;
            } catch (Exception ex) {
                System.err.println("Failed to save as properties too: " + ex.getMessage());
                return false;
            }
        }
    }

    /**
     * Fallback method to save settings as simple properties file
     */
    private void saveSettingsAsProperties(Map<String, Object> settings) throws IOException {
        Properties props = new Properties();
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            if (entry.getValue() != null) {
                props.setProperty(entry.getKey(), entry.getValue().toString());
            }
        }

        File propsFile = new File(getSettingsFilename() + ".properties");
        try (FileOutputStream out = new FileOutputStream(propsFile)) {
            props.store(out, "CodeThief settings for " + username);
            System.out.println("Saved settings as properties to " + propsFile.getAbsolutePath());
        }
    }

    /**
     * Loads settings from a file
     *
     * @return Map of settings, or empty map if file not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> loadSettings() {
        File settingsFile = new File(getSettingsFilename());

        if (!settingsFile.exists()) {
            System.out.println("No saved settings found for " + username + " at " + settingsFile.getAbsolutePath());
            return new HashMap<>();
        }

        if (!settingsFile.canRead()) {
            System.err.println("Settings file exists but cannot be read: " + settingsFile.getAbsolutePath());
            return new HashMap<>();
        }

        try (FileInputStream fileIn = new FileInputStream(settingsFile);
             ObjectInputStream in = new ObjectInputStream(fileIn)) {
            Object obj = in.readObject();
            if (obj instanceof Map) {
                System.out.println("Settings loaded from " + settingsFile.getAbsolutePath());
                return (Map<String, Object>) obj;
            } else {
                System.out.println("Loaded object is not a Map: " + obj.getClass().getName());
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading settings: " + e.getMessage());

            // Try loading from properties as fallback
            try {
                Map<String, Object> propSettings = loadSettingsFromProperties();
                if (!propSettings.isEmpty()) {
                    return propSettings;
                }
            } catch (Exception ex) {
                System.err.println("Failed to load from properties file too: " + ex.getMessage());
            }
        }

        return new HashMap<>();
    }

    /**
     * Fallback method to load settings from properties file
     */
    private Map<String, Object> loadSettingsFromProperties() throws IOException {
        File propsFile = new File(getSettingsFilename() + ".properties");
        Map<String, Object> result = new HashMap<>();

        if (propsFile.exists() && propsFile.canRead()) {
            Properties props = new Properties();
            try (FileInputStream in = new FileInputStream(propsFile)) {
                props.load(in);

                for (String key : props.stringPropertyNames()) {
                    String value = props.getProperty(key);

                    // Try to convert to appropriate type
                    if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                        result.put(key, Boolean.valueOf(value));
                    } else {
                        try {
                            result.put(key, Integer.valueOf(value));
                        } catch (NumberFormatException e) {
                            result.put(key, value);
                        }
                    }
                }

                System.out.println("Loaded " + result.size() + " settings from properties file");
            }
        }

        return result;
    }

    /**
     * Gets the full filename for the settings file
     *
     * @return The settings filename
     */
    private String getSettingsFilename() {
        return SETTINGS_DIRECTORY + username + "_settings.dat";
    }

    /**
     * Sanitizes a username for file storage
     *
     * @param username The username to sanitize
     * @return A safe filename
     */
    private String sanitizeFilename(String username) {
        return username.replaceAll("[^a-zA-Z0-9]", "_");
    }
}