package org;

import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.SkillTracker;
import org.dreambot.api.methods.skills.Skills;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Enhanced statistics tracker for CodeThief Pro
 * Tracks thieving statistics and provides a visually appealing paint
 * Updated with darker theme and improved toggle button functionality
 */
public class StatisticsTracker {

    // Runtime tracking
    private long startTime;
    private long lastPaintUpdate;

    // Thieving stats
    private int successfulThieves;
    private int failedThieves;
    private int totalAttempts;
    private String currentTarget;
    private ThievingState currentState;

    // Experience tracking
    private int startXp;
    private int startLevel;
    private int xpGained;
    private int levelsGained;

    // Profit tracking
    private int itemsStolen;
    private int estimatedProfit;

    // Inventory stats
    private int foodEaten;
    private int itemsDropped;
    private int itemsBanked;

    // Stats by target
    private final Map<String, TargetStats> targetStats;

    // Format helpers
    private final DecimalFormat formatPercent = new DecimalFormat("#0.0%");
    private final NumberFormat formatNumber = NumberFormat.getNumberInstance(Locale.US);

    // Paint settings
    private boolean expandedPaint = true;

    // Paint colors - exact color scheme as requested
    private final Color backgroundBaseColor = new Color(20, 12, 28, 230); // Darker background with opacity
    private final Color backgroundGradientColor = new Color(30, 18, 40, 230); // Medium background with opacity
    private final Color lightBackground = new Color(40, 25, 55, 230); // Lighter background for hover
    private final Color headerColor = new Color(180, 140, 255); // Light purple for headers
    private final Color textColor = new Color(230, 230, 230); // Off-white text
    private final Color accentColor = new Color(180, 120, 220); // Purple accent (no yellow)
    private final Color progressBarBg = new Color(20, 12, 28, 150); // Darker progress bar background
    private final Color progressBarFg = new Color(140, 100, 200); // Purple progress bar foreground

    // Toggle button
    private Rectangle toggleButton;

    /**
     * Creates a new statistics tracker
     */
    public StatisticsTracker() {
        this.startTime = System.currentTimeMillis();
        this.lastPaintUpdate = 0;
        this.successfulThieves = 0;
        this.failedThieves = 0;
        this.totalAttempts = 0;
        this.itemsStolen = 0;
        this.estimatedProfit = 0;
        this.startXp = Skills.getExperience(Skill.THIEVING);
        this.startLevel = Skills.getRealLevel(Skill.THIEVING);
        this.targetStats = new HashMap<>();
        this.currentTarget = "None";
        this.currentState = ThievingState.INITIALIZE;

        // Initialize tracker for XP/hr calculations
        SkillTracker.start(Skill.THIEVING);
    }

    /**
     * Called when a thieving attempt succeeds
     */
    public void onSuccessfulThieve() {
        successfulThieves++;
        totalAttempts++;
        itemsStolen++;

        // Update target-specific stats
        TargetStats stats = getTargetStatsFor(currentTarget);
        stats.successfulAttempts++;
        stats.totalAttempts++;

        // Update XP gained from tracker
        xpGained = Skills.getExperience(Skill.THIEVING) - startXp;
        int currentLevel = Skills.getRealLevel(Skill.THIEVING);
        levelsGained = currentLevel - startLevel;

        // Estimate profit based on target
        estimatedProfit += estimateProfitPerThieve(currentTarget);
    }

    /**
     * Called when a thieving attempt fails
     */
    public void onFailedThieve() {
        failedThieves++;
        totalAttempts++;

        // Update target-specific stats
        TargetStats stats = getTargetStatsFor(currentTarget);
        stats.failedAttempts++;
        stats.totalAttempts++;
    }

    /**
     * Updates inventory statistics
     *
     * @param foodEaten    Count of food eaten
     * @param itemsDropped Count of items dropped
     * @param itemsBanked  Count of items banked
     */
    public void updateInventoryStats(int foodEaten, int itemsDropped, int itemsBanked) {
        this.foodEaten = foodEaten;
        this.itemsDropped = itemsDropped;
        this.itemsBanked = itemsBanked;
    }

    /**
     * Sets the current thieving target
     *
     * @param targetName The target name
     */
    public void setCurrentTarget(String targetName) {
        this.currentTarget = targetName;

        // Initialize target stats if needed
        getTargetStatsFor(targetName);
    }

    /**
     * Sets the current script state
     *
     * @param state The current state
     */
    public void setCurrentState(ThievingState state) {
        this.currentState = state;
    }

    /**
     * Gets or creates target statistics for a specific target
     *
     * @param targetName The target name
     * @return Target statistics
     */
    private TargetStats getTargetStatsFor(String targetName) {
        return targetStats.computeIfAbsent(targetName, k -> new TargetStats());
    }

    /**
     * Draws the paint on screen
     *
     * @param g2d Graphics2D object
     */
    public void drawPaint(Graphics2D g2d) {
        try {
            // Only update certain stats every 500ms to reduce CPU usage
            boolean updateStats = (System.currentTimeMillis() - lastPaintUpdate) > 500;
            if (updateStats) {
                // Update XP gained from tracker
                xpGained = Skills.getExperience(Skill.THIEVING) - startXp;
                int currentLevel = Skills.getRealLevel(Skill.THIEVING);
                levelsGained = currentLevel - startLevel;

                lastPaintUpdate = System.currentTimeMillis();
            }

            // Enable antialiasing for smoother graphics
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Set up dimensions and position - wider to accommodate text
            int baseX = 5;
            int baseY = 5;
            int width = expandedPaint ? 240 : 210; // Narrower to fit screen better
            int height = expandedPaint ? 355 : 190; // Taller for expanded view to fit all text

            // Calculate toggle button position (top right corner)
            toggleButton = new Rectangle(baseX + width - 25, baseY + 5, 20, 20);

            // Draw main panel background - rounded rectangle with gradient
            drawPanelBackground(g2d, baseX, baseY, width, height);

            // Draw header with script name and version
            drawHeader(g2d, baseX, baseY, width);

            // Draw basic stats panel
            int yOffset = baseY + 35;
            drawBasicStats(g2d, baseX, yOffset, width);

            // Draw toggle button with theme-matching visibility
            g2d.setColor(lightBackground);
            g2d.fillRect(toggleButton.x, toggleButton.y, toggleButton.width, toggleButton.height);
            g2d.setColor(textColor); // White text for contrast
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString(expandedPaint ? "-" : "+", toggleButton.x + 7, toggleButton.y + 15);

            // If expanded, draw additional stats
            if (expandedPaint) {
                drawExpandedStats(g2d, baseX, baseY + 190, width);
            }

        } catch (Exception e) {
            // If paint errors, draw simple fallback
            g2d.setColor(Color.RED);
            g2d.drawString("Paint Error: " + e.getMessage(), 10, 70);
        }
    }

    /**
     * Draws the panel background with gradient
     */
    private void drawPanelBackground(Graphics2D g2d, int x, int y, int width, int height) {
        // Create gradient paint
        GradientPaint gradient = new GradientPaint(
                x, y, backgroundBaseColor,
                x + width, y + height, backgroundGradientColor
        );
        g2d.setPaint(gradient);

        // Draw rounded rectangle
        RoundRectangle2D roundedRect = new RoundRectangle2D.Float(x, y, width, height, 10, 10);
        g2d.fill(roundedRect);

        // Draw subtle border
        g2d.setColor(new Color(130, 70, 180, 80));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(roundedRect);
    }

    /**
     * Draws the header section
     */
    private void drawHeader(Graphics2D g2d, int x, int y, int width) {
        // Draw header text
        g2d.setFont(new Font("Arial", Font.BOLD, 14)); // Smaller font
        g2d.setColor(headerColor);
        g2d.drawString("CodeThief Pro v1.2", x + 10, y + 20);

        // Draw underline
        g2d.setColor(accentColor);
        g2d.fillRect(x + 10, y + 24, width - 20, 2);
    }

    /**
     * Draws the basic statistics section
     */
    private void drawBasicStats(Graphics2D g2d, int x, int y, int width) {
        // Setup font - smaller font for better fit
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        g2d.setColor(textColor);

        // Runtime
        String runtime = formatTime(System.currentTimeMillis() - startTime);
        g2d.drawString("Runtime: " + runtime, x + 10, y + 15);

        // Current state
        g2d.drawString("State: " + formatState(currentState), x + 10, y + 32);

        // Current target
        g2d.drawString("Target: " + currentTarget, x + 10, y + 49);

        // Thieving count (make more compact)
        String countText = successfulThieves + " (+" + (xpGained / Math.max(1, getXpPerThieve(currentTarget))) + ")";
        g2d.drawString("Thieving: " + countText, x + 10, y + 66);

        // XP gained and XP/hr
        g2d.drawString("XP Gained: " + formatNumber.format(xpGained), x + 10, y + 83);

        int xpHr = SkillTracker.getGainedExperiencePerHour(Skill.THIEVING);
        g2d.drawString("XP/hr: " + formatNumber.format(xpHr), x + 10, y + 100);

        // TTL
        int xpToLevel = Skills.getExperienceToLevel(Skill.THIEVING);
        String ttl = xpHr > 0 ? formatTime((long) (xpToLevel * 3600000.0 / xpHr)) : "--:--:--";
        g2d.drawString("TTL: " + ttl, x + 10, y + 117);

        // Success rate & items stolen
        double successRate = totalAttempts > 0 ? (double) successfulThieves / totalAttempts : 0;
        g2d.drawString("Success Rate: " + formatPercent.format(successRate), x + 10, y + 134);
        g2d.drawString("Items Stolen: " + itemsStolen, x + 10, y + 151);
    }

    /**
     * Draws the expanded statistics section when toggled
     */
    private void drawExpandedStats(Graphics2D g2d, int x, int y, int width) {
        // Setup section header
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(headerColor);
        g2d.drawString("Advanced Statistics", x + 10, y + 15);

        // Underline
        g2d.setColor(accentColor);
        g2d.fillRect(x + 10, y + 19, width - 20, 1);

        // Setup regular font
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(textColor);

        // Inventory stats
        g2d.drawString("Items Banked: " + itemsBanked, x + 10, y + 40);
        g2d.drawString("Items Dropped: " + itemsDropped, x + 10, y + 55);
        g2d.drawString("Food Eaten: " + foodEaten, x + 10, y + 70);

        // Profit estimation - Use shorter labels and format for space
        g2d.drawString("Est. Profit: " + formatNumber.format(estimatedProfit) + " gp", x + 10, y + 90);
        g2d.drawString("Profit/hr: " + formatNumber.format(calculateProfitPerHour()) + " gp", x + 10, y + 105);

        // Progress to next level bar
        int currentXp = Skills.getExperience(Skill.THIEVING);
        int currentLevel = Skills.getRealLevel(Skill.THIEVING);
        int xpForCurrentLevel = Skills.getExperienceForLevel(currentLevel);
        int xpForNextLevel = Skills.getExperienceForLevel(currentLevel + 1);
        int xpToNextLevel = xpForNextLevel - currentXp;
        double progressPercent = (double) (currentXp - xpForCurrentLevel) / Math.max(1, (xpForNextLevel - xpForCurrentLevel));

        // Draw level info with shorter format
        g2d.drawString("Level " + currentLevel + " → " + (currentLevel + 1) + " (" + formatPercent.format(progressPercent) + ")",
                x + 10, y + 125);
        g2d.drawString("XP to level: " + formatNumber.format(xpToNextLevel), x + 10, y + 140);

        // Draw progress bar
        int barX = x + 10;
        int barY = y + 150; // Adjust position
        int barWidth = width - 20;
        int barHeight = 12; // Smaller height

        // Bar background
        g2d.setColor(progressBarBg);
        g2d.fillRect(barX, barY, barWidth, barHeight);

        // Bar foreground
        g2d.setColor(progressBarFg);
        g2d.fillRect(barX, barY, (int) (barWidth * progressPercent), barHeight);

        // Bar border
        g2d.setColor(Color.GRAY);
        g2d.drawRect(barX, barY, barWidth, barHeight);
    }

    /**
     * Formats a time in milliseconds to HH:MM:SS
     */
    private String formatTime(long time) {
        long seconds = time / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
    }

    /**
     * Formats a state enum to a readable string
     */
    private String formatState(ThievingState state) {
        if (state == null) return "UNKNOWN";

        String stateName = state.name();
        return stateName.replace("_", " ");
    }

    /**
     * Gets current success rate
     */
    public double getSuccessRate() {
        if (totalAttempts == 0) return 0;
        return (double) successfulThieves / totalAttempts;
    }

    /**
     * Gets XP gained since starting
     */
    public int getXpGained() {
        return xpGained;
    }

    /**
     * Gets current target's success rate
     */
    public double getTargetSuccessRate() {
        TargetStats stats = targetStats.get(currentTarget);
        if (stats == null || stats.totalAttempts == 0) return 0;
        return (double) stats.successfulAttempts / stats.totalAttempts;
    }

    /**
     * Checks if the mouse is over the toggle button
     */
    public boolean isOverToggleButton(Point mousePos) {
        return toggleButton != null && toggleButton.contains(mousePos);
    }

    /**
     * Toggles the expanded paint view
     */
    public void toggleExpandedPaint() {
        this.expandedPaint = !this.expandedPaint;
    }

    /**
     * Sets whether the paint is in expanded view
     *
     * @param expanded Whether to show the expanded view
     */
    public void setExpandedPaint(boolean expanded) {
        this.expandedPaint = expanded;
    }

    /**
     * Gets the toggle button rectangle for mouse interaction
     *
     * @return Rectangle representing the toggle button
     */
    public Rectangle getToggleButton() {
        return toggleButton;
    }

    /**
     * Estimates profit per thieve based on target
     */
    private int estimateProfitPerThieve(String targetName) {
        // Return approximate average gp per successful pickpocket
        switch (targetName) {
            case "Man":
            case "Woman":
                return 3;
            case "Farmer":
                return 9;
            case "Warrior":
                return 18;
            case "Guard":
                return 30;
            case "Knight of Ardougne":
                return 50;
            case "Paladin":
                return 80;
            case "Hero":
                return 120;
            case "Master Farmer":
                return 45; // Average seed value
            case "Cake Stall":
                return 15;
            case "Tea Stall":
                return 12;
            default:
                return 10; // Default value
        }
    }

    /**
     * Gets XP per thieve based on target
     */
    private int getXpPerThieve(String targetName) {
        switch (targetName) {
            case "Man":
            case "Woman":
                return 8;
            case "Farmer":
                return 14;
            case "Warrior":
                return 26;
            case "Guard":
                return 42;
            case "Knight of Ardougne":
                return 84;
            case "Paladin":
                return 152;
            case "Hero":
                return 275;
            case "Master Farmer":
                return 43;
            case "Cake Stall":
                return 16;
            case "Tea Stall":
                return 16;
            default:
                return 8; // Default value
        }
    }

    /**
     * Calculates profit per hour
     */
    private long calculateProfitPerHour() {
        if (startTime == 0) return 0;

        long runTime = System.currentTimeMillis() - startTime;
        if (runTime < 1000) return 0; // Avoid division by zero

        return (long) (estimatedProfit * 3600000.0 / runTime);
    }

    /**
     * Helper class to track stats for each target type
     */
    private static class TargetStats {
        int successfulAttempts;
        int failedAttempts;
        int totalAttempts;

        TargetStats() {
            successfulAttempts = 0;
            failedAttempts = 0;
            totalAttempts = 0;
        }
    }
}