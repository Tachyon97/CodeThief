package org.gui.util;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles saving and loading script settings for persistence between sessions
 */
public class SettingsPersistence {
    private final String username;
    private static final String SETTINGS_DIRECTORY = "codethief_settings/";

    /**
     * Creates a new settings persistence instance for a specific user
     *
     * @param username The username to associate with settings
     */
    public SettingsPersistence(String username) {
        this.username = sanitizeFilename(username);

        // Create settings directory if it doesn't exist
        File directory = new File(SETTINGS_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdirs();
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

            // Create the settings file if it doesn't exist
            File file = new File(filename);
            if (!file.exists()) {
                file.createNewFile();
            }

            // Serialize the settings map to the file
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
                out.writeObject(settings);
            }

            System.out.println("Settings saved to " + filename);
            return true;
        } catch (IOException e) {
            System.err.println("Error saving settings: " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads settings from a file
     *
     * @return Map of settings, or empty map if file not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> loadSettings() {
        try {
            String filename = getSettingsFilename();
            File file = new File(filename);

            if (!file.exists()) {
                System.out.println("No saved settings found for " + username);
                return new HashMap<>();
            }

            // Deserialize the settings map from the file
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                Object obj = in.readObject();
                if (obj instanceof Map) {
                    System.out.println("Settings loaded from " + filename);
                    return (Map<String, Object>) obj;
                }
            }

            return new HashMap<>();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading settings: " + e.getMessage());
            return new HashMap<>();
        }
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