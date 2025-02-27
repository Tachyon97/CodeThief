package org.managers;

import org.core.config.ScriptConfiguration;
import org.dreambot.api.input.Mouse;
import org.dreambot.api.methods.input.Camera;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;

import java.awt.*;

public class AntiBanManager {
    private final ScriptConfiguration config;
    private final AntiBanProfile profile;
    private long lastAntiBanAction;
    private long lastBreak;
    private long lastCameraRotation;
    private long lastMouseMovement;
    private long lastTabCheck;
    private long lastMouseUpdate;
    private boolean antiBanEnabled;

    private int consecutiveSameActions;
    private String lastActionType;

    private static final int MAX_SAME_ACTIONS = 3;
    private static final long MOUSE_UPDATE_INTERVAL = 300000; // 5 minutes

    private WindMouseCustom windMouse;

    public AntiBanManager(ScriptConfiguration config) {
        this.config = config;
        this.lastAntiBanAction = System.currentTimeMillis();
        this.lastBreak = System.currentTimeMillis();
        this.lastCameraRotation = System.currentTimeMillis();
        this.lastMouseMovement = System.currentTimeMillis();
        this.lastTabCheck = System.currentTimeMillis();
        this.lastMouseUpdate = 0;
        this.antiBanEnabled = config.isAntiBanEnabled();

        String username = Players.getLocal().getName();
        if (username == null || username.isEmpty()) {
            username = "default_user";
            System.out.println("[AntiBan] Could not get player name, using default");
        }

        this.profile = new AntiBanProfile(username, config.getAntiBanIntensity());

        this.consecutiveSameActions = 0;
        this.lastActionType = "";

        System.out.println("[AntiBan] Created manager with profile: " + profile.getProfileSummary());
    }

    public void setCustomMouse(WindMouseCustom windMouse) {
        this.windMouse = windMouse;
        if (windMouse != null) {
            windMouse.configureFromProfile(profile);
            lastMouseUpdate = System.currentTimeMillis();
        }
    }

    public void performRandomChecks() {
        if (!antiBanEnabled) {
            return;
        }

        if (System.currentTimeMillis() - lastAntiBanAction < getRandomCheckInterval()) {
            return;
        }

        updateMouseParameters();

        String actionToPerform = chooseNextAction();

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
            case "hover":
                performRandomHover();
                break;
            case "tabs":
                checkRandomTab();
                break;
        }

        lastAntiBanAction = System.currentTimeMillis();
        if (actionToPerform.equals(lastActionType)) {
            consecutiveSameActions++;
        } else {
            consecutiveSameActions = 0;
            lastActionType = actionToPerform;
        }
    }

    private void updateMouseParameters() {
        if (windMouse != null && System.currentTimeMillis() - lastMouseUpdate > MOUSE_UPDATE_INTERVAL) {
            windMouse.configureFromProfile(profile);
            lastMouseUpdate = System.currentTimeMillis();
        }
    }

    private String chooseNextAction() {
        if (consecutiveSameActions >= MAX_SAME_ACTIONS) {
            if (!lastActionType.equals("rotate") && shouldRotateCamera()) return "rotate";
            if (!lastActionType.equals("skills") && shouldCheckSkills()) return "skills";
            if (!lastActionType.equals("break") && shouldTakeBreak()) return "break";
            if (!lastActionType.equals("mouse") && shouldMoveRandomly()) return "mouse";
            if (!lastActionType.equals("hover") && shouldPerformHover()) return "hover";
            if (!lastActionType.equals("tabs") && shouldCheckTabs()) return "tabs";

            if (lastActionType.equals("rotate")) return "mouse";
            if (lastActionType.equals("skills")) return "rotate";
            if (lastActionType.equals("break")) return "skills";
            if (lastActionType.equals("mouse")) return "hover";
            if (lastActionType.equals("hover")) return "tabs";
            if (lastActionType.equals("tabs")) return "break";
            return "mouse";
        }

        long timeSinceCameraRotation = System.currentTimeMillis() - lastCameraRotation;
        long timeSinceMouseMovement = System.currentTimeMillis() - lastMouseMovement;
        long timeSinceTabCheck = System.currentTimeMillis() - lastTabCheck;

        // Prioritize camera rotation if it's been a while
        if (timeSinceCameraRotation > 180000 && Math.random() < 0.7) return "rotate";

        // Prioritize mouse movement if it's been a while
        if (timeSinceMouseMovement > 90000 && Math.random() < 0.7) return "mouse";

        // Prioritize tab checking if it's been a while
        if (timeSinceTabCheck > 240000 && Math.random() < 0.7) return "tabs";

        // Otherwise use normal probabilities
        if (shouldRotateCamera()) return "rotate";
        if (shouldCheckSkills()) return "skills";
        if (shouldTakeBreak()) return "break";
        if (shouldMoveRandomly()) return "mouse";
        if (shouldPerformHover()) return "hover";
        if (shouldCheckTabs()) return "tabs";

        return "none";
    }

    private long getRandomCheckInterval() {
        return profile.getRandomCheckInterval();
    }

    private boolean shouldRotateCamera() {
        return Math.random() < profile.getCameraRotationProbability();
    }

    private boolean shouldCheckSkills() {
        return Math.random() < profile.getSkillCheckProbability();
    }

    private boolean shouldTakeBreak() {
        if (System.currentTimeMillis() - lastBreak < profile.getRandomCheckInterval() * 3) {
            return false;
        }

        boolean shouldBreak = Math.random() < profile.getBreakProbability();
        if (shouldBreak) {
            lastBreak = System.currentTimeMillis();
        }
        return shouldBreak;
    }

    private boolean shouldMoveRandomly() {
        return Math.random() < profile.getMouseMovementProbability();
    }

    private boolean shouldPerformHover() {
        return Math.random() < (profile.getMouseMovementProbability() * 0.5);
    }

    private boolean shouldCheckTabs() {
        return Math.random() < (profile.getSkillCheckProbability() * 0.7);
    }

    public boolean shouldMisclick() {
        if (!antiBanEnabled) {
            return false;
        }
        return Math.random() < profile.getMisclickProbability();
    }

    private void rotateCamera() {
        int angle = profile.getRandomRotationAngle();
        int tilt = profile.getRandomCameraTilt();

        System.out.println("[AntiBan] Rotating camera to angle:" + angle + ", tilt:" + tilt);

        Camera.rotateTo(angle, tilt);
        sleep(profile.getRandomActionDelay());
        lastCameraRotation = System.currentTimeMillis();
    }

    private void checkSkills() {
        System.out.println("[AntiBan] Checking skills tab");

        if (Tabs.open(Tab.SKILLS)) {
            sleep(profile.getRandomActionDelay());

            Skill skillToCheck;
            switch (profile.getSkillCheckStyle()) {
                case 0:
                    skillToCheck = Skill.THIEVING;
                    break;
                case 1:
                    skillToCheck = (Math.random() < 0.7) ? Skill.THIEVING :
                            (Math.random() < 0.5 ? Skill.ATTACK : Skill.STRENGTH);
                    break;
                case 2:
                    Skill[] allSkills = Skill.values();
                    skillToCheck = allSkills[(int) (Math.random() * allSkills.length)];
                    break;
                case 3:
                    double rand = Math.random();
                    if (rand < 0.6) skillToCheck = Skill.THIEVING;
                    else if (rand < 0.8) skillToCheck = Skill.AGILITY;
                    else skillToCheck = Skill.HITPOINTS;
                    break;
                default:
                    skillToCheck = Skill.THIEVING;
            }

            int skillLevel = Skills.getRealLevel(skillToCheck);
            System.out.println("[AntiBan] Checking " + skillToCheck.name() + " (Level " + skillLevel + ")");

            Point skillLocation = getSkillTabLocation(skillToCheck);
            Mouse.move(skillLocation);

            // Add hesitation for more human-like behavior
            sleep(profile.getHesitationTime());

            sleep(profile.getRandomActionDelay());

            Tabs.open(Tab.INVENTORY);
        }
    }

    private void checkRandomTab() {
        Tab[] commonTabs = {Tab.INVENTORY, Tab.EQUIPMENT, Tab.PRAYER, Tab.COMBAT};
        Tab tabToCheck = commonTabs[(int) (Math.random() * commonTabs.length)];

        System.out.println("[AntiBan] Checking " + tabToCheck.name() + " tab");

        if (Tabs.open(tabToCheck)) {
            sleep(profile.getRandomActionDelay());

            // Simulate looking at the tab content
            sleep(200, 700);

            // Return to inventory
            Tabs.open(Tab.INVENTORY);
        }

        lastTabCheck = System.currentTimeMillis();
    }

    private void performRandomHover() {
        System.out.println("[AntiBan] Performing random hover");

        // Get random inventory item to hover
        int slot = (int) (Math.random() * 28);
        int slotX = 580 + (slot % 4) * 42;
        int slotY = 225 + (slot / 4) * 36;

        // Move to item
        Point hoverPoint = new Point(slotX, slotY);
        Mouse.move(hoverPoint);

        // Hesitate
        sleep(profile.getHesitationTime());

        // Move back to center-ish
        moveMouseRandomly();
    }

    private Point getSkillTabLocation(Skill skill) {
        int baseX = 550;
        int baseY = 205;

        int column, row;

        switch (skill) {
            case ATTACK:
                column = 0;
                row = 0;
                break;
            case STRENGTH:
                column = 1;
                row = 0;
                break;
            case DEFENCE:
                column = 2;
                row = 0;
                break;
            case RANGED:
                column = 3;
                row = 0;
                break;
            case PRAYER:
                column = 0;
                row = 1;
                break;
            case MAGIC:
                column = 1;
                row = 1;
                break;
            case RUNECRAFTING:
                column = 2;
                row = 1;
                break;
            case CONSTRUCTION:
                column = 3;
                row = 1;
                break;
            case HITPOINTS:
                column = 0;
                row = 2;
                break;
            case AGILITY:
                column = 1;
                row = 2;
                break;
            case HERBLORE:
                column = 2;
                row = 2;
                break;
            case THIEVING:
                column = 3;
                row = 2;
                break;
            case CRAFTING:
                column = 0;
                row = 3;
                break;
            case FLETCHING:
                column = 1;
                row = 3;
                break;
            case SLAYER:
                column = 2;
                row = 3;
                break;
            case HUNTER:
                column = 3;
                row = 3;
                break;
            default:
                column = 0;
                row = 4;
        }

        int posX = baseX + (column * 63) + (int) (Math.random() * 10) - 5;
        int posY = baseY + (row * 33) + (int) (Math.random() * 10) - 5;

        return new Point(posX, posY);
    }

    private void takeShortBreak() {
        int breakDuration = profile.getRandomBreakDuration();
        System.out.println("[AntiBan] Taking short break (" + breakDuration + "ms)");
        sleep(breakDuration);
    }

    private void moveMouseRandomly() {
        System.out.println("[AntiBan] Moving mouse randomly");

        Point p = profile.getRandomPointNear(
                300,
                300,
                100
        );

        Mouse.move(p);
        lastMouseMovement = System.currentTimeMillis();
    }

    public Point getRandomPointNear(int centerX, int centerY, int radius) {
        return profile.getRandomPointNear(centerX, centerY, radius);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sleep(int min, int max) {
        try {
            Thread.sleep(min + (int) (Math.random() * (max - min)));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setAntiBanEnabled(boolean enabled) {
        this.antiBanEnabled = enabled;
    }

    public String getProfileSummary() {
        return profile.getProfileSummary();
    }

    public AntiBanProfile getProfile() {
        return profile;
    }
}