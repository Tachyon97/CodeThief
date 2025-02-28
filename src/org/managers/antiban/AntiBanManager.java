package org.managers.antiban;

import org.core.config.ScriptConfiguration;
import org.dreambot.api.methods.interactive.Players;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final long MOUSE_UPDATE_INTERVAL = 300000;

    private WindMouseCustom windMouse;
    private final List<AntiBanAction> antiBanActions;
    private final Map<String, Long> lastActionTimes = new HashMap<>();

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

        AntiBanActionFactory actionFactory = new AntiBanActionFactory(profile);
        this.antiBanActions = actionFactory.createAllActions();

        for (AntiBanAction action : antiBanActions) {
            lastActionTimes.put(action.getActionType(), 0L);
        }

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
        AntiBanAction selectedAction = findActionByType(actionToPerform);

        if (selectedAction != null) {
            selectedAction.execute();
            lastActionTimes.put(actionToPerform, System.currentTimeMillis());

            if (actionToPerform.equals("rotate")) {
                lastCameraRotation = System.currentTimeMillis();
            } else if (actionToPerform.equals("mouse")) {
                lastMouseMovement = System.currentTimeMillis();
            } else if (actionToPerform.equals("tabs")) {
                lastTabCheck = System.currentTimeMillis();
            } else if (actionToPerform.equals("break")) {
                lastBreak = System.currentTimeMillis();
            }
        }

        lastAntiBanAction = System.currentTimeMillis();
        if (actionToPerform.equals(lastActionType)) {
            consecutiveSameActions++;
        } else {
            consecutiveSameActions = 0;
            lastActionType = actionToPerform;
        }
    }

    private AntiBanAction findActionByType(String actionType) {
        for (AntiBanAction action : antiBanActions) {
            if (action.getActionType().equals(actionType)) {
                return action;
            }
        }
        return null;
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

        if (timeSinceCameraRotation > 180000 && Math.random() < 0.7) return "rotate";
        if (timeSinceMouseMovement > 90000 && Math.random() < 0.7) return "mouse";
        if (timeSinceTabCheck > 240000 && Math.random() < 0.7) return "tabs";

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

    public Point getRandomPointNear(int centerX, int centerY, int radius) {
        return profile.getRandomPointNear(centerX, centerY, radius);
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