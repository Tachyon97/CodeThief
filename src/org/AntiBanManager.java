package org;

import org.dreambot.api.input.Mouse;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.input.Camera;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;

import java.awt.*;

/**
 * Enhanced anti-ban manager using account-specific profiles
 */
public class AntiBanManager {
    private final ScriptConfiguration config;
    private final AntiBanProfile profile;
    private long lastAntiBanAction;
    private long lastBreak;
    private boolean antiBanEnabled;

    // Anti-ban complexity tracking (for multiple movement types)
    private int consecutiveSameActions;
    private String lastActionType;

    // Constants
    private static final int MAX_SAME_ACTIONS = 3; // Max times to do same type of action consecutively

    /**
     * Creates a new anti-ban manager with account-specific randomization
     *
     * @param config The script configuration
     */
    public AntiBanManager(ScriptConfiguration config) {
        this.config = config;
        this.lastAntiBanAction = System.currentTimeMillis();
        this.lastBreak = System.currentTimeMillis();
        this.antiBanEnabled = config.isAntiBanEnabled();

        // Get player username
        String username = Players.getLocal().getName();
        if (username == null || username.isEmpty()) {
            username = "default_user";
            System.out.println("[AntiBan] Could not get player name, using default");
        }

        // Create anti-ban profile with selected intensity
        this.profile = new AntiBanProfile(username, config.getAntiBanIntensity());

        // Initialize tracking variables
        this.consecutiveSameActions = 0;
        this.lastActionType = "";

        // Log profile creation
        System.out.println("[AntiBan] Created manager with profile: " + profile.getProfileSummary());
    }

    /**
     * Performs random anti-ban checks based on probability settings
     */
    public void performRandomChecks() {
        // Skip if anti-ban is disabled
        if (!antiBanEnabled) {
            return;
        }

        // Only perform checks at random intervals based on profile
        if (System.currentTimeMillis() - lastAntiBanAction < getRandomCheckInterval()) {
            return;
        }

        // Choose action based on probabilities and avoiding repetition
        String actionToPerform = chooseNextAction();

        // Perform the selected action
        switch (actionToPerform) {
            case "rotate":
                rotateCamera();
                break;
            case "skills":
                checkSkills();
                break;
            case "break":
                takeShortBreak();
                break;
            case "mouse":
                moveMouseRandomly();
                break;
            default:
                // No action
                break;
        }

        // Update tracking variables
        lastAntiBanAction = System.currentTimeMillis();
        if (actionToPerform.equals(lastActionType)) {
            consecutiveSameActions++;
        } else {
            consecutiveSameActions = 0;
            lastActionType = actionToPerform;
        }
    }

    /**
     * Chooses the next anti-ban action based on probabilities
     * and avoiding repetitive patterns
     *
     * @return Action type to perform
     */
    private String chooseNextAction() {
        // If we've done the same action multiple times, force something different
        if (consecutiveSameActions >= MAX_SAME_ACTIONS) {
            // Find a different action
            if (!lastActionType.equals("rotate") && shouldRotateCamera()) return "rotate";
            if (!lastActionType.equals("skills") && shouldCheckSkills()) return "skills";
            if (!lastActionType.equals("break") && shouldTakeBreak()) return "break";
            if (!lastActionType.equals("mouse") && shouldMoveRandomly()) return "mouse";

            // If all checks fail, just pick something different than last
            if (lastActionType.equals("rotate")) return "mouse";
            if (lastActionType.equals("skills")) return "rotate";
            if (lastActionType.equals("break")) return "skills";
            if (lastActionType.equals("mouse")) return "break";
            return "mouse"; // Default
        }

        // Normal probability-based selection
        if (shouldRotateCamera()) return "rotate";
        if (shouldCheckSkills()) return "skills";
        if (shouldTakeBreak()) return "break";
        if (shouldMoveRandomly()) return "mouse";

        // No action selected
        return "none";
    }

    /**
     * Calculates a random interval between anti-ban checks
     *
     * @return Time in milliseconds
     */
    private long getRandomCheckInterval() {
        return profile.getRandomCheckInterval();
    }

    /**
     * Determines if camera should be rotated
     *
     * @return True if action should be performed
     */
    private boolean shouldRotateCamera() {
        return Math.random() < profile.getCameraRotationProbability();
    }

    /**
     * Determines if skills tab should be checked
     *
     * @return True if action should be performed
     */
    private boolean shouldCheckSkills() {
        return Math.random() < profile.getSkillCheckProbability();
    }

    /**
     * Determines if a short break should be taken
     *
     * @return True if action should be performed
     */
    private boolean shouldTakeBreak() {
        // Limit breaks to once every few minutes (based on profile)
        if (System.currentTimeMillis() - lastBreak < profile.getRandomCheckInterval() * 3) {
            return false;
        }

        boolean shouldBreak = Math.random() < profile.getBreakProbability();
        if (shouldBreak) {
            lastBreak = System.currentTimeMillis();
        }
        return shouldBreak;
    }

    /**
     * Determines if mouse should be moved randomly
     *
     * @return True if action should be performed
     */
    private boolean shouldMoveRandomly() {
        return Math.random() < profile.getMouseMovementProbability();
    }

    /**
     * Determines if a misclick should occur
     *
     * @return True if misclick should occur
     */
    public boolean shouldMisclick() {
        if (!antiBanEnabled) {
            return false;
        }
        return Math.random() < profile.getMisclickProbability();
    }

    /**
     * Rotates the camera randomly according to profile
     */
    private void rotateCamera() {
        // Get profile-based rotation parameters
        int angle = profile.getRandomRotationAngle();
        int tilt = profile.getRandomCameraTilt();

        // Log the action
        System.out.println("[AntiBan] Rotating camera to angle:" + angle + ", tilt:" + tilt);

        // Perform the rotation
        Camera.rotateTo(angle, tilt);
        sleep(profile.getRandomActionDelay());
    }

    /**
     * Opens the skills tab and checks thieving skill according to profile
     */
    private void checkSkills() {
        System.out.println("[AntiBan] Checking skills tab");

        // Open skills tab
        if (Tabs.open(Tab.SKILLS)) {
            sleep(profile.getRandomActionDelay());

            // Determine which skill to check based on profile style
            Skill skillToCheck;
            switch (profile.getSkillCheckStyle()) {
                case 0: // Always check thieving
                    skillToCheck = Skill.THIEVING;
                    break;
                case 1: // Check thieving and combat skills
                    skillToCheck = (Math.random() < 0.7) ? Skill.THIEVING :
                            (Math.random() < 0.5 ? Skill.ATTACK : Skill.STRENGTH);
                    break;
                case 2: // Check random skills
                    Skill[] allSkills = Skill.values();
                    skillToCheck = allSkills[(int)(Math.random() * allSkills.length)];
                    break;
                case 3: // Check thieving and related skills
                    double rand = Math.random();
                    if (rand < 0.6) skillToCheck = Skill.THIEVING;
                    else if (rand < 0.8) skillToCheck = Skill.AGILITY;
                    else skillToCheck = Skill.HITPOINTS;
                    break;
                default:
                    skillToCheck = Skill.THIEVING;
            }

            // Hover over skill
            int skillLevel = Skills.getRealLevel(skillToCheck);
            System.out.println("[AntiBan] Checking " + skillToCheck.name() + " (Level " + skillLevel + ")");

            // Get approximate location for skill in the skills tab
            Point skillLocation = getSkillTabLocation(skillToCheck);
            Mouse.move(skillLocation);

            sleep(profile.getRandomActionDelay());

            // Return to inventory
            Tabs.open(Tab.INVENTORY);
        }
    }

    /**
     * Gets the approximate location of a skill in the skills tab
     *
     * @param skill The skill to find
     * @return Approximate screen position
     */
    private Point getSkillTabLocation(Skill skill) {
        // These are approximate positions - would need to be adjusted for actual interface
        int baseX = 550;
        int baseY = 205;

        // Skills tab layout (approximate):
        // 0,0  1,0  2,0  3,0
        // 0,1  1,1  2,1  3,1
        // ...
        int column, row;

        switch (skill) {
            case ATTACK: column = 0; row = 0; break;
            case STRENGTH: column = 1; row = 0; break;
            case DEFENCE: column = 2; row = 0; break;
            case RANGED: column = 3; row = 0; break;
            case PRAYER: column = 0; row = 1; break;
            case MAGIC: column = 1; row = 1; break;
            case RUNECRAFTING: column = 2; row = 1; break;
            case CONSTRUCTION: column = 3; row = 1; break;
            case HITPOINTS: column = 0; row = 2; break;
            case AGILITY: column = 1; row = 2; break;
            case HERBLORE: column = 2; row = 2; break;
            case THIEVING: column = 3; row = 2; break;
            case CRAFTING: column = 0; row = 3; break;
            case FLETCHING: column = 1; row = 3; break;
            case SLAYER: column = 2; row = 3; break;
            case HUNTER: column = 3; row = 3; break;
            // Add other skills as needed
            default: column = 0; row = 4;
        }

        // Calculate position with small random offset
        int posX = baseX + (column * 63) + (int)(Math.random() * 10) - 5;
        int posY = baseY + (row * 33) + (int)(Math.random() * 10) - 5;

        return new Point(posX, posY);
    }

    /**
     * Takes a short break to simulate AFK behavior according to profile
     */
    private void takeShortBreak() {
        int breakDuration = profile.getRandomBreakDuration();
        System.out.println("[AntiBan] Taking short break (" + breakDuration + "ms)");
        sleep(breakDuration);
    }

    /**
     * Moves the mouse to a random point on screen according to profile
     */
    private void moveMouseRandomly() {
        System.out.println("[AntiBan] Moving mouse randomly");

        // Get a point based on profile's movement style
        Point p = profile.getRandomPointNear(
                300, // Center X
                300, // Center Y
                100  // Radius
        );

        Mouse.move(p);
    }

    /**
     * Generates a random point near the specified center point
     * based on profile settings
     *
     * @param centerX Center X coordinate
     * @param centerY Center Y coordinate
     * @param radius  Maximum distance from center
     * @return Random point within radius of center
     */
    public Point getRandomPointNear(int centerX, int centerY, int radius) {
        return profile.getRandomPointNear(centerX, centerY, radius);
    }

    /**
     * Sleeps for the specified duration
     *
     * @param millis Duration in milliseconds
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the anti-ban enabled state
     *
     * @param enabled Whether anti-ban should be enabled
     */
    public void setAntiBanEnabled(boolean enabled) {
        this.antiBanEnabled = enabled;
    }

    /**
     * Gets the anti-ban profile summary for display
     *
     * @return Profile summary string
     */
    public String getProfileSummary() {
        return profile.getProfileSummary();
    }
}