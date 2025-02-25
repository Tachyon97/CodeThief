package org;

import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.utilities.Timer;

import java.awt.*;

/**
 * Tracks and displays script performance statistics.
 */
public class StatisticsTracker {
    private final Timer runTimer;
    private int startXp;
    private int itemsStolen;
    private int successCount;
    private int failCount;
    private int startLevel;
    private long lastUpdate;
    private ThievingState currentState;
    private String currentTarget;

    // Thieving theme colors - darker purple as requested
    private final Color bgColor = new Color(32, 12, 36, 230); // Darker purple with transparency
    private final Color headerColor = new Color(147, 112, 219); // Medium purple
    private final Color textColor = new Color(255, 255, 255); // White
    private final Color accentColor = new Color(186, 85, 211); // Medium orchid
    private final Color statColor = new Color(216, 191, 216); // Thistle

    // Font settings
    private final Font headerFont = new Font("Arial", Font.BOLD, 12);
    private final Font normalFont = new Font("Arial", Font.PLAIN, 11);

    public StatisticsTracker() {
        this.runTimer = new Timer();
        this.startXp = Skills.getExperience(Skill.THIEVING);
        this.startLevel = Skills.getRealLevel(Skill.THIEVING);
        this.itemsStolen = 0;
        this.successCount = 0;
        this.failCount = 0;
        this.lastUpdate = System.currentTimeMillis();
        this.currentState = ThievingState.INITIALIZE;
        this.currentTarget = "None";
    }

    /**
     * Records a successful thieving attempt
     */
    public void onSuccessfulThieve() {
        successCount++;
        itemsStolen++;
        updateXpGained();
    }

    /**
     * Records a failed thieving attempt
     */
    public void onFailedThieve() {
        failCount++;
        updateXpGained();
    }

    /**
     * Updates internal XP tracking
     */
    private void updateXpGained() {
        // Update method called occasionally to prevent constant polling
        if (System.currentTimeMillis() - lastUpdate < 2000) {
            return;
        }

        lastUpdate = System.currentTimeMillis();
    }

    /**
     * Updates the current script state
     *
     * @param state The new state
     */
    public void setCurrentState(ThievingState state) {
        this.currentState = state;
    }

    /**
     * Updates the current thieving target
     *
     * @param target The new target
     */
    public void setCurrentTarget(String target) {
        this.currentTarget = target;
    }

    /**
     * Gets total XP gained during the session
     *
     * @return XP gained
     */
    public int getXpGained() {
        return Skills.getExperience(Skill.THIEVING) - startXp;
    }

    /**
     * Gets total levels gained during the session
     *
     * @return Levels gained
     */
    public int getLevelsGained() {
        return Skills.getRealLevel(Skill.THIEVING) - startLevel;
    }

    /**
     * Calculates XP per hour based on session duration
     *
     * @return XP per hour
     */
    public int getXpPerHour() {
        double hoursRun = runTimer.elapsed() / 3600000.0;
        if (hoursRun < 0.01) return 0;
        return (int) (getXpGained() / hoursRun);
    }

    /**
     * Calculates actions per hour based on session duration
     *
     * @return Actions per hour
     */
    public int getActionsPerHour() {
        double hoursRun = runTimer.elapsed() / 3600000.0;
        if (hoursRun < 0.01) return 0;
        return (int) ((successCount + failCount) / hoursRun);
    }

    /**
     * Calculates success rate percentage
     *
     * @return Success rate as a percentage
     */
    public double getSuccessRate() {
        if (successCount + failCount == 0) return 0;
        return (double) successCount / (successCount + failCount) * 100.0;
    }

    /**
     * Calculates time to next level at current XP rate
     *
     * @return Time to next level as formatted string
     */
    public String getTimeToNextLevel() {
        try {
            int currentXp = Skills.getExperience(Skill.THIEVING);
            int currentLevel = Skills.getRealLevel(Skill.THIEVING);

            // Get XP required for next level
            int nextLevelXp = Skills.getExperienceForLevel(currentLevel + 1);
            int xpToNextLevel = nextLevelXp - currentXp;

            // Handle max level case
            if (currentLevel >= 99) {
                return "MAX";
            }

            int xpPerHour = getXpPerHour();
            if (xpPerHour <= 0) return "--:--:--";

            // Calculate time in seconds
            long secondsToNextLevel = (long) (xpToNextLevel * 3600.0 / xpPerHour);
            return formatTime(secondsToNextLevel);
        } catch (Exception e) {
            return "ERROR";
        }
    }

    /**
     * Formats seconds into hours:minutes:seconds
     *
     * @param seconds Total seconds
     * @return Formatted time string
     */
    private String formatTime(long seconds) {
        if (seconds < 0) return "--:--:--";

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    /**
     * Renders statistics on screen using the paint interface
     *
     * @param g Graphics2D object
     */
    public void drawPaint(Graphics2D g) {
        // Store original settings
        Stroke originalStroke = g.getStroke();
        Font originalFont = g.getFont();
        Color originalColor = g.getColor();

        // Enable anti-aliasing for smoother text
        g.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Main background panel - Moved down by 10px and increased height
        int panelX = 5;
        int panelY = 15; // Changed from 5 to 15 for better top margin
        int panelWidth = 200;
        int panelHeight = 210; // Increased to ensure all text is covered

        // Draw panel background with rounded corners
        g.setColor(bgColor);
        g.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 10, 10);

        // Draw border
        g.setColor(accentColor);
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 10, 10);

        // Reset stroke
        g.setStroke(originalStroke);

        // Draw header
        g.setColor(headerColor);
        g.setFont(headerFont);
        g.drawString("CodeThief Pro v1.0", panelX + 10, panelY + 20);

        // Draw horizontal separator
        g.setColor(accentColor);
        g.drawLine(panelX + 10, panelY + 25, panelX + panelWidth - 10, panelY + 25);

        // Set font for stats
        g.setFont(normalFont);

        // Draw statistics
        int textX = panelX + 15;
        int textY = panelY + 42;
        int lineHeight = 16;

        // Runtime and status section
        g.setColor(textColor);
        g.drawString("Runtime: " + runTimer.formatTime(), textX, textY);
        textY += lineHeight;
        g.drawString("State: " + currentState, textX, textY);
        textY += lineHeight;
        g.drawString("Target: " + currentTarget, textX, textY);
        textY += lineHeight;

        // Thin separator
        g.setColor(accentColor);
        g.drawLine(panelX + 10, textY + 2, panelX + panelWidth - 10, textY + 2);
        textY += lineHeight;

        // Level and XP section
        g.setColor(textColor);
        g.drawString("Thieving: " + Skills.getRealLevel(Skill.THIEVING) + " (+" + getLevelsGained() + ")", textX, textY);
        textY += lineHeight;
        g.drawString("XP Gained: " + String.format("%,d", getXpGained()), textX, textY);
        textY += lineHeight;
        g.drawString("XP/hr: " + String.format("%,d", getXpPerHour()), textX, textY);
        textY += lineHeight;
        g.drawString("TTL: " + getTimeToNextLevel(), textX, textY);
        textY += lineHeight;

        // Thin separator
        g.setColor(accentColor);
        g.drawLine(panelX + 10, textY + 2, panelX + panelWidth - 10, textY + 2);
        textY += lineHeight;

        // Performance section
        g.setColor(textColor);
        g.drawString("Success Rate: " + String.format("%.1f", getSuccessRate()) + "%", textX, textY);
        textY += lineHeight;
        g.drawString("Items Stolen: " + String.format("%,d", itemsStolen), textX, textY);

        // Restore original settings
        g.setFont(originalFont);
        g.setColor(originalColor);
        g.setStroke(originalStroke);
    }
}