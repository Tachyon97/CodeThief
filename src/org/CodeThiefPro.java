package org;

import org.core.config.ScriptConfiguration;
import org.dreambot.api.Client;
import org.dreambot.api.data.GameState;
import org.dreambot.api.input.Mouse;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.SkillTracker;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.listener.ChatListener;
import org.dreambot.api.script.listener.PaintListener;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.widgets.message.Message;
import org.gui.GUI;
import org.managers.*;
import org.managers.antiban.AntiBanManager;
import org.managers.antiban.WindMouseCustom;
import org.states.ErrorState;
import org.states.HandlingInventoryState;
import org.states.InitializeState;
import org.states.ScriptState;
import org.util.ScriptUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

@ScriptManifest(
        name = "CodeThief Pro",
        author = "Calle",
        version = 1.0,
        description = "Advanced thieving script with auto-progression",
        category = Category.THIEVING
)
public class CodeThiefPro extends AbstractScript implements PaintListener, ChatListener, MouseListener {
    private ScriptState currentState;
    private ScriptConfiguration config;
    private LocationManager locationManager;
    private InventoryManager inventoryManager;
    private AntiBanManager antiBanManager;
    private StatisticsTracker statsTracker;
    private GUI gui;
    private WindMouseCustom customMouse;

    private long lastThievingAttempt;
    private long lastStateChange;
    private long lastTargetSearch;
    private long lastStatsUpdate;
    private int consecutiveFailures;
    private int failedInteractionAttempts;
    private boolean scriptStarted;
    private boolean cantReachMessageReceived;
    private Object currentTarget;

    public static final int COIN_POUCH_THRESHOLD = 12;
    public static final int TARGET_REFRESH_INTERVAL = 5000;
    public static final int MAX_FAILED_INTERACTIONS = 5;
    private static final int STATS_UPDATE_INTERVAL = 2000;

    @Override
    public void onStart() {
        try {
            customMouse = new WindMouseCustom();
            Mouse.setMouseAlgorithm(customMouse);
            log("Custom mouse movement algorithm initialized");

            config = new ScriptConfiguration();

            setupGUI();

            if (config.isConfigured()) {
                initializeScript();
            }
        } catch (Exception e) {
            log("Error in onStart: " + e.getMessage());
            stop();
        }
    }

    private void setupGUI() {
        final boolean[] configComplete = new boolean[1];
        configComplete[0] = false;

        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    log("Creating GUI...");
                    gui = new GUI(this, config);

                    gui.addWindowListener(new java.awt.event.WindowAdapter() {
                        @Override
                        public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                            log("GUI window closed");
                            configComplete[0] = config.isConfigured();
                        }
                    });

                    gui.setVisible(true);
                    log("GUI created and shown successfully");
                } catch (Exception e) {
                    log("Error creating GUI: " + e.getMessage());
                    createDefaultSettings();
                }
            });
        } catch (Exception e) {
            log("Error waiting for GUI: " + e.getMessage());
            createDefaultSettings();
        }

        int timeout = 0;
        int maxTimeout = 1200;

        log("Waiting for user to configure script...");

        while (!configComplete[0] && timeout < maxTimeout) {
            if (config.isConfigured()) {
                configComplete[0] = true;
                log("Configuration completed via Start button");
                break;
            }

            if (gui != null && !gui.isVisible() && timeout > 10) {
                if (!config.isConfigured()) {
                    log("GUI closed without configuring. Stopping script.");
                    stop();
                    return;
                }
            }

            ScriptUtils.sleep(500);
            timeout++;

            if (timeout % 120 == 0) {
                log("Still waiting for configuration... " + (timeout / 120) + " minutes elapsed");
            }
        }

        if (timeout >= maxTimeout) {
            log("Configuration timed out after 10 minutes. Stopping script.");
            stop();
            return;
        }

        if (!config.isConfigured()) {
            log("Script not properly configured. Stopping.");
            stop();
        }
    }

    private void createDefaultSettings() {
        config.setCurrentTargetName("Man");
        config.setAutoProgressionEnabled(true);
        config.setBankingEnabled(true);
        config.setConfigured(true);
        log("Using default settings due to GUI error");
    }

    private void initializeScript() {
        log("Starting script with target: " + config.getCurrentTargetName());

        locationManager = new LocationManager(config);
        log("Loaded LocationManager");

        inventoryManager = new InventoryManager(config);
        inventoryManager.setLocationManager(locationManager);
        log("Loaded InventoryManager");

        antiBanManager = new AntiBanManager(config);
        log("Loaded AntiBanManager");
        antiBanManager.setCustomMouse(customMouse);
        log("Loaded Anti-ban custom mouse");

        statsTracker = new StatisticsTracker();

        Canvas canvas = Client.getCanvas();
        canvas.addMouseListener(this);

        SkillTracker.start(Skill.THIEVING);

        setState(new InitializeState());
        lastThievingAttempt = System.currentTimeMillis();
        lastStateChange = System.currentTimeMillis();
        lastTargetSearch = 0;
        lastStatsUpdate = 0;
        consecutiveFailures = 0;
        scriptStarted = true;
        currentTarget = null;
        cantReachMessageReceived = false;
        failedInteractionAttempts = 0;

        log("CodeThief Pro started successfully!");
    }

    @Override
    public int onLoop() {
        if (Client.getGameState() != GameState.LOGGED_IN) {
            return 600;
        }

        if (!scriptStarted) {
            return 1000;
        }

        try {
            cantReachMessageReceived = false;

            antiBanManager.performRandomChecks();

            if (System.currentTimeMillis() - lastStatsUpdate > STATS_UPDATE_INTERVAL) {
                updateStatistics();
                lastStatsUpdate = System.currentTimeMillis();
            }

            return currentState.execute(this);
        } catch (Exception e) {
            log("Error in main loop: " + e.getMessage());
            setState(new ErrorState());
            return 1000;
        }
    }

    @Override
    public void onMessage(Message message) {
        String msg = message.getMessage().toLowerCase();

        if (msg.contains("can't reach that") || msg.contains("too far away")) {
            cantReachMessageReceived = true;
            failedInteractionAttempts++;
            log("Received unreachable message: " + message.getMessage());

            if (failedInteractionAttempts >= MAX_FAILED_INTERACTIONS) {
                log("Too many failed interaction attempts. Forcing target refresh.");
                currentTarget = null;
                failedInteractionAttempts = 0;
            }
        }

        if (msg.contains("you pick") || msg.contains("you steal")) {
            statsTracker.onSuccessfulThieve();
            consecutiveFailures = 0;
            failedInteractionAttempts = 0;
        }

        if (msg.contains("you fail") || msg.contains("you're stunned")) {
            statsTracker.onFailedThieve();
            consecutiveFailures++;
        }

        if (msg.contains("inventory is too full")) {
            setState(new HandlingInventoryState());
        }
    }

    public Object findThievingTarget() {
        lastTargetSearch = System.currentTimeMillis();
        String targetName = config.getCurrentTargetName();

        log("Searching for target: " + targetName);

        if (targetName.equals("Tea Stall") || targetName.equals("Cake Stall")) {
            return findStallTarget(targetName);
        } else {
            return findNPCTarget(targetName);
        }
    }

    private GameObject findStallTarget(String targetName) {
        GameObject stall = GameObjects.closest(obj ->
                obj.getName().equals(targetName) &&
                        obj.hasAction("Steal-from"));

        if (stall != null) {
            log("Found " + targetName + " at " + stall.getTile() + ", distance: " + stall.distance());

            if (targetName.equals("Tea Stall")) {
                Player player = Players.getLocal();
                Tile teaStallSafespot = new Tile(3268, 3410, 0);

                if (player.distance(teaStallSafespot) > 3) {
                    log("Not at Tea Stall safespot yet. Need to walk there first.");
                    return null;
                }
            }
            return stall;
        } else {
            log("Could not find " + targetName);
        }
        return null;
    }

    private NPC findNPCTarget(String targetName) {
        NPC closestNPC = NPCs.closest(npc ->
                npc != null &&
                        npc.exists() &&
                        npc.getName().equals(targetName) &&
                        npc.hasAction("Pickpocket"));

        if (closestNPC != null) {
            double distance = closestNPC.distance();
            log("Found " + targetName + " at distance " + String.format("%.1f", distance));

            if (closestNPC.canReach()) {
                log("Target is directly reachable");
                return closestNPC;
            } else {
                log("Target not immediately reachable, attempting to walk closer");
                if (org.dreambot.api.methods.walking.impl.Walking.walk(closestNPC.getTile())) {
                    log("Walking to NPC at " + closestNPC.getTile());
                    ScriptUtils.sleep(800, 1200);

                    if (closestNPC.canReach()) {
                        log("Target is now reachable after walking");
                    }
                }

                return closestNPC;
            }
        }

        log("No " + targetName + " found nearby");
        return null;
    }

    public boolean shouldRefreshTarget() {
        if (cantReachMessageReceived) {
            return true;
        }

        long timeSinceLastSearch = System.currentTimeMillis() - lastTargetSearch;
        if (timeSinceLastSearch > TARGET_REFRESH_INTERVAL) {
            return true;
        }

        if (currentTarget instanceof NPC) {
            NPC npc = (NPC) currentTarget;
            return !npc.exists() || npc.isInCombat() || npc.getInteractingCharacter() != null;
        } else if (currentTarget instanceof GameObject) {
            GameObject obj = (GameObject) currentTarget;
            return !obj.exists();
        }
        return true;
    }

    private void updateStatistics() {
        if (statsTracker != null && inventoryManager != null) {
            statsTracker.updateInventoryStats(
                    inventoryManager.getFoodEaten(),
                    inventoryManager.getItemsDropped(),
                    inventoryManager.getItemsBanked()
            );
        }
    }

    public void setState(ScriptState newState) {
        if (this.currentState == null || !this.currentState.getClass().equals(newState.getClass())) {
            String oldState = this.currentState != null ? this.currentState.getClass().getSimpleName() : "null";
            String newStateName = newState.getClass().getSimpleName();

            log("State changed: " + oldState + " -> " + newStateName);
            this.currentState = newState;

            if (statsTracker != null) {
                statsTracker.setCurrentState(newStateName);
            }

            lastStateChange = System.currentTimeMillis();
        }
    }

    @Override
    public void onPaint(Graphics graphics) {
        try {
            if (statsTracker != null) {
                statsTracker.drawPaint((Graphics2D) graphics);
            } else {
                graphics.setColor(Color.RED);
                graphics.drawString("CodeThief Pro - Stats tracker not initialized", 10, 50);
            }
        } catch (Exception e) {
            graphics.setColor(Color.RED);
            graphics.drawString("Paint Error: " + e.getMessage(), 10, 70);
        }
    }

    @Override
    public void onExit() {
        log("Script stopped. Final stats:");
        if (statsTracker != null) {
            log("XP Gained: " + statsTracker.getXpGained());
            log("Success Rate: " + String.format("%.1f%%", statsTracker.getSuccessRate() * 100));
        }

        Canvas canvas = Client.getCanvas();
        if (canvas != null) {
            canvas.removeMouseListener(this);
        }

        if (gui != null && gui.isVisible()) {
            gui.dispose();
        }

        Mouse.setMouseAlgorithm(Mouse.getDefaultMouseAlgorithm());
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        try {
            if (statsTracker != null && statsTracker.isOverToggleButton(e.getPoint())) {
                statsTracker.toggleExpandedPaint();
            }
        } catch (Exception ex) {
            log("Error in mouseClicked: " + ex.getMessage());
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    // Getter methods for state access
    public ScriptConfiguration getConfig() {
        return config;
    }

    public LocationManager getLocationManager() {
        return locationManager;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public AntiBanManager getAntiBanManager() {
        return antiBanManager;
    }

    public StatisticsTracker getStatsTracker() {
        return statsTracker;
    }

    public Object getCurrentTarget() {
        return currentTarget;
    }

    public void setCurrentTarget(Object target) {
        this.currentTarget = target;
    }

    public long getLastThievingAttempt() {
        return lastThievingAttempt;
    }

    public void setLastThievingAttempt(long time) {
        this.lastThievingAttempt = time;
    }

    public long getLastStateChange() {
        return lastStateChange;
    }

    public int getFailedInteractionAttempts() {
        return failedInteractionAttempts;
    }

    public void incrementFailedInteractionAttempts() {
        failedInteractionAttempts++;
    }

    public void resetFailedInteractionAttempts() {
        failedInteractionAttempts = 0;
    }
}