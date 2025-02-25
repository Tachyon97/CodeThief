package org;

import java.util.Arrays;
import java.util.List;

/**
 * Manages all configuration options for the thieving script.
 */
public class ScriptConfiguration {
    // Main settings
    private String currentTargetName;
    private boolean autoProgressionEnabled;
    private boolean freestyleMode;
    private boolean bankingEnabled;
    private int healthThreshold;
    private boolean configured;
    private boolean preferStalls;

    // Anti-ban settings
    private boolean antiBanEnabled;
    private int antiBanIntensity;
    private double cameraRotationProbability;
    private double skillCheckProbability;
    private double breakProbability;
    private double randomMovementProbability;
    private double misclickProbability;

    // Food items to use when healing
    private List<String> foodItems;

    /**
     * Creates a new configuration with default values
     */
    public ScriptConfiguration() {
        // Default values
        this.currentTargetName = "Man";
        this.autoProgressionEnabled = true;
        this.freestyleMode = false;
        this.bankingEnabled = true;
        this.healthThreshold = 50;
        this.configured = false;
        this.preferStalls = false;

        // Default anti-ban settings
        this.antiBanEnabled = true;
        this.antiBanIntensity = 50;
        this.cameraRotationProbability = 0.15;
        this.skillCheckProbability = 0.05;
        this.breakProbability = 0.02;
        this.randomMovementProbability = 0.10;
        this.misclickProbability = 0.02;

        // Initialize food item list
        this.foodItems = Arrays.asList("Cake", "Bread", "Fish", "Shark", "Lobster", "Salmon", "Tuna", "Monkfish",
                "Food", "Potion", "Brew", "Wine");
    }

    // Getters and setters remain the same
    public String getCurrentTargetName() {
        return currentTargetName;
    }

    public void setCurrentTargetName(String currentTargetName) {
        this.currentTargetName = currentTargetName;
    }

    public boolean isAutoProgressionEnabled() {
        return autoProgressionEnabled;
    }

    public void setAutoProgressionEnabled(boolean autoProgressionEnabled) {
        this.autoProgressionEnabled = autoProgressionEnabled;
    }

    public boolean isFreestyleMode() {
        return freestyleMode;
    }

    public void setFreestyleMode(boolean freestyleMode) {
        this.freestyleMode = freestyleMode;
    }

    public boolean isBankingEnabled() {
        return bankingEnabled;
    }

    public void setBankingEnabled(boolean bankingEnabled) {
        this.bankingEnabled = bankingEnabled;
    }

    public int getHealthThreshold() {
        return healthThreshold;
    }

    public void setHealthThreshold(int healthThreshold) {
        this.healthThreshold = healthThreshold;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public boolean isAntiBanEnabled() {
        return antiBanEnabled;
    }

    public void setAntiBanEnabled(boolean antiBanEnabled) {
        this.antiBanEnabled = antiBanEnabled;
    }

    public int getAntiBanIntensity() {
        return antiBanIntensity;
    }

    public void setAntiBanIntensity(int antiBanIntensity) {
        this.antiBanIntensity = antiBanIntensity;
    }

    public boolean preferStalls() {
        return preferStalls;
    }

    public void setPreferStalls(boolean preferStalls) {
        this.preferStalls = preferStalls;
    }

    /**
     * Gets the list of food items configured for healing
     *
     * @return List of food item names
     */
    public List<String> getFoodItems() {
        return foodItems;
    }

    /**
     * Sets the list of food items for healing
     *
     * @param foodItems List of food item names
     */
    public void setFoodItems(List<String> foodItems) {
        this.foodItems = foodItems;
    }

    // Other getters/setters continue...

    /**
     * Gets all available thieving method names.
     * Limited to working methods only - only Tea and Cake stalls included.
     *
     * @return String array of thieving method names
     */
    public String[] getAllThievingMethods() {
        return new String[]{
                "Man", "Woman", "Farmer", "Warrior", "Guard", "Master Farmer",
                "Knight of Ardougne", "Paladin", "Hero",
                "Cake Stall", "Tea Stall" // Only include working stalls
        };
    }

    /**
     * Determines the optimal thieving target based on current thieving level
     * Updated to only use Tea and Cake stalls
     *
     * @param thievingLevel The current thieving level
     * @return The name of the optimal target
     */
    public String determineOptimalTarget(int thievingLevel) {
        if (thievingLevel >= 90) return "Hero";
        else if (thievingLevel >= 80) return "Paladin";
        else if (thievingLevel >= 70) return "Paladin";
        else if (thievingLevel >= 55) return "Knight of Ardougne";
        else if (thievingLevel >= 40) return "Master Farmer";
        else if (thievingLevel >= 25) return "Warrior";
        else if (thievingLevel >= 20) {
            if (preferStalls) {
                return "Tea Stall"; // Only use working stalls
            } else {
                return "Warrior";
            }
        }
        else if (thievingLevel >= 5) {
            if (preferStalls) {
                return "Cake Stall"; // Only use working stalls
            } else {
                return "Man";
            }
        }
        else return "Man";
    }
}