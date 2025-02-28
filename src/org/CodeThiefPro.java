package org;

import org.core.config.ScriptConfiguration;
import org.core.config.ThievingState;
import org.dreambot.api.Client;
import org.dreambot.api.data.GameState;
import org.dreambot.api.input.Mouse;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.SkillTracker;
import org.dreambot.api.methods.skills.Skills;
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

    // Configuration and managers
    private ThievingState state;
    private ScriptConfiguration config;
    private LocationManager locationManager;
    private InventoryManager inventoryManager;
    private AntiBanManager antiBanManager;
    private StatisticsTracker statsTracker;
    private GUI gui;
    private WindMouseCustom customMouse;

    // Tracking variables
    private long lastThievingAttempt;
    private long lastStateChange;
    private long lastTargetSearch;
    private long lastStatsUpdate;
    private int consecutiveFailures;
    private int failedInteractionAttempts;
    private boolean scriptStarted;
    private boolean cantReachMessageReceived;
    private Object currentTarget;

    // Constants
    private static final int COIN_POUCH_THRESHOLD = 12;
    private static final int TARGET_REFRESH_INTERVAL = 5000;
    private static final int MAX_FAILED_INTERACTIONS = 5;
    private static final int STATS_UPDATE_INTERVAL = 2000;

    @Override
    public void onStart() {
        try {
            // Initialize custom mouse movement
            customMouse = new WindMouseCustom();
            Mouse.setMouseAlgorithm(customMouse);
            log("Custom mouse movement algorithm initialized");

            // Initialize configuration
            config = new ScriptConfiguration();

            // Set up GUI
            setupGUI();

            // Continue initialization when config is ready
            if (config.isConfigured()) {
                initializeScript();
            }
        } catch (Exception e) {
            log("Error in onStart: " + e.getMessage());
            stop();
        }
    }

    /**
     * Sets up the GUI for script configuration
     */
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
        int maxTimeout = 1200; // 10 minutes (1200 * 500ms)

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

            sleep(500);
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

    /**
     * Creates default settings when GUI fails
     */
    private void createDefaultSettings() {
        config.setCurrentTargetName("Man");
        config.setAutoProgressionEnabled(true);
        config.setBankingEnabled(true);
        config.setConfigured(true);
        log("Using default settings due to GUI error");
    }

    /**
     * Initializes the script with core components
     */
    private void initializeScript() {
        log("Starting script with target: " + config.getCurrentTargetName());

        // Initialize managers
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

        // Add mouse listener for paint interaction
        Canvas canvas = Client.getCanvas();
        canvas.addMouseListener(this);

        // Start tracking XP
        SkillTracker.start(Skill.THIEVING);

        // Initialize state variables
        setState(ThievingState.INITIALIZE);
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

            // Perform anti-ban actions
            antiBanManager.performRandomChecks();

            // Update statistics periodically
            if (System.currentTimeMillis() - lastStatsUpdate > STATS_UPDATE_INTERVAL) {
                updateStatistics();
                lastStatsUpdate = System.currentTimeMillis();
            }

            // State machine for script behavior
            return handleState();
        } catch (Exception e) {
            log("Error in main loop: " + e.getMessage());
            setState(ThievingState.ERROR);
            return 1000;
        }
    }

    /**
     * Handles the current state of the script
     */
    private int handleState() {
        switch (state) {
            case INITIALIZE:
                return handleInitialize();
            case WALKING_TO_LOCATION:
                return handleWalkingToLocation();
            case THIEVING:
                return handleThieving();
            case HANDLING_INVENTORY:
                return handleInventory();
            case WALKING_TO_BANK:
                return handleWalkingToBank();
            case BANKING:
                return handleBanking();
            case HANDLING_HEALTH:
                return handleHealth();
            case ERROR:
                return handleError();
            default:
                setState(ThievingState.INITIALIZE);
                return 600;
        }
    }

    @Override
    public void onMessage(Message message) {
        String msg = message.getMessage().toLowerCase();

        // Handle "can't reach" messages
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

        // Handle successful pickpocket/steal
        if (msg.contains("you pick") || msg.contains("you steal")) {
            statsTracker.onSuccessfulThieve();
            consecutiveFailures = 0;
            failedInteractionAttempts = 0;
        }

        // Handle failed pickpocket/stun
        if (msg.contains("you fail") || msg.contains("you're stunned")) {
            statsTracker.onFailedThieve();
            consecutiveFailures++;
        }

        // Handle full inventory
        if (msg.contains("inventory is too full")) {
            setState(ThievingState.HANDLING_INVENTORY);
        }
    }

    /**
     * Initializes the script state
     */
    private int handleInitialize() {
        int thievingLevel = Skills.getRealLevel(Skill.THIEVING);

        // Auto-progression if enabled
        if (config.isAutoProgressionEnabled()) {
            String optimalTarget = config.determineOptimalTarget(thievingLevel);
            config.setCurrentTargetName(optimalTarget);
            log("Auto-progression set target to: " + optimalTarget);
        }

        statsTracker.setCurrentTarget(config.getCurrentTargetName());

        // Check if we need to heal and get food
        if (inventoryManager.needToHeal() && !inventoryManager.hasFood() && config.isBankingEnabled()) {
            log("Need to heal but no food available. Going to bank first.");
            setState(ThievingState.WALKING_TO_BANK);
            return 300;
        }

        // Check if we're already at target
        if (locationManager.isAtThievingArea()) {
            currentTarget = findThievingTarget();
            if (currentTarget != null) {
                log("Target already in view. Starting thieving without walking.");
                setState(ThievingState.THIEVING);
                return 300;
            }
        }

        // Find target or walk to location
        currentTarget = findThievingTarget();
        if (currentTarget != null) {
            log("Target found. Starting thieving.");
            setState(ThievingState.THIEVING);
            return 300;
        }

        setState(ThievingState.WALKING_TO_LOCATION);
        return 300;
    }

    /**
     * Handles walking to thieving location
     */
    private int handleWalkingToLocation() {
        // Check if target is already visible
        currentTarget = findThievingTarget();
        if (currentTarget != null) {
            log("Found target while preparing to walk. Skipping walk.");
            setState(ThievingState.THIEVING);
            return 300;
        }

        // Handle health if needed
        if (inventoryManager.needToHeal()) {
            setState(ThievingState.HANDLING_HEALTH);
            return 300;
        }

        // Check if already at location
        if (locationManager.isAtThievingArea()) {
            log("Arrived at thieving location");
            setState(ThievingState.THIEVING);
            return 300;
        }

        // Walk to location
        if (locationManager.walkToThievingArea()) {
            log("Walking to thieving location: " + config.getCurrentTargetName());
            return 1000;
        } else {
            // Check if we're stuck
            if (locationManager.isStuck()) {
                log("Walking appears to be stuck. Trying to recover.");
                locationManager.resetStuckState();
                setState(ThievingState.ERROR);
            }
            return 1000;
        }
    }

    /**
     * Handles thieving actions
     */
    private int handleThieving() {
        // Check inventory status
        if (inventoryManager.isInventoryFull()) {
            setState(ThievingState.HANDLING_INVENTORY);
            return 300;
        }

        // Check health status
        if (inventoryManager.needToHeal()) {
            setState(ThievingState.HANDLING_HEALTH);
            return 300;
        }

        // Handle coin pouches
        if (inventoryManager.handleCoinPouches(COIN_POUCH_THRESHOLD)) {
            return 600;
        }

        // Check if we need to refresh target
        if (currentTarget == null || shouldRefreshTarget()) {
            currentTarget = findThievingTarget();
        }

        // Interact with target
        if (currentTarget != null) {
            if (currentTarget instanceof NPC) {
                return handleNPCThieving((NPC) currentTarget);
            } else if (currentTarget instanceof GameObject) {
                return handleObjectThieving((GameObject) currentTarget);
            }
        }

        // No target found, check if we're in the right area
        if (!locationManager.isThievingTargetAvailable()) {
            log("No thieving target found. Walking to location.");
            setState(ThievingState.WALKING_TO_LOCATION);
            return 600;
        }

        log("Waiting for target to appear");
        return 1000;
    }

    /**
     * Determines if the target should be refreshed
     */
    private boolean shouldRefreshTarget() {
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

    /**
     * Finds a thieving target
     */
    private Object findThievingTarget() {
        lastTargetSearch = System.currentTimeMillis();
        String targetName = config.getCurrentTargetName();

        log("Searching for target: " + targetName);

        // Handle stalls
        if (targetName.equals("Tea Stall") || targetName.equals("Cake Stall")) {
            return findStallTarget(targetName);
        } else {
            return findNPCTarget(targetName);
        }
    }

    /**
     * Finds a stall target
     */
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

    /**
     * Finds an NPC target
     */
    private NPC findNPCTarget(String targetName) {
        NPC closestNPC = NPCs.closest(npc ->
                npc != null &&
                        npc.exists() &&
                        npc.getName().equals(targetName) &&
                        npc.hasAction("Pickpocket"));

        if (closestNPC != null) {
            double distance = closestNPC.distance();
            log("Found " + targetName + " at distance " + String.format("%.1f", distance));

            // If we can directly reach it, just return it
            if (closestNPC.canReach()) {
                log("Target is directly reachable");
                return closestNPC;
            } else {
                // Try to walk closer
                log("Target not immediately reachable, attempting to walk closer");
                if (org.dreambot.api.methods.walking.impl.Walking.walk(closestNPC.getTile())) {
                    log("Walking to NPC at " + closestNPC.getTile());
                    sleep(800, 1200);

                    // After walking, check if we can reach it
                    if (closestNPC.canReach()) {
                        log("Target is now reachable after walking");
                    }
                }

                // Return the closest NPC regardless
                return closestNPC;
            }
        }

        log("No " + targetName + " found nearby");
        return null;
    }

    /**
     * Handles pickpocketing an NPC
     */
    private int handleNPCThieving(NPC npc) {
        if (System.currentTimeMillis() - lastThievingAttempt < 1200) {
            return 100;
        }

        if ((npc == null || !npc.exists() || npc.isInCombat() || !npc.canReach())) {
            if (npc != null) {
                if (!npc.canReach()) {
                    log("Target " + npc.getName() + " is not reachable. Finding new target.");
                } else if (npc.isInCombat()) {
                    log("Target " + npc.getName() + " is in combat. Finding new target.");
                }
            }
            currentTarget = null;
            return 600;
        }

        // Purposely misclick sometimes for anti-ban
        if (antiBanManager.shouldMisclick()) {
            log("Intentional misclick");
            int currentX = npc.getX();
            int currentY = npc.getY();
            Point randomPoint = antiBanManager.getRandomPointNear(currentX, currentY, 5);
            Mouse.move(randomPoint);
            Mouse.click(false);
            sleep(300, 600);
            lastThievingAttempt = System.currentTimeMillis();
            return 1000;
        }

        // Attempt to pickpocket
        if (npc.interact("Pickpocket")) {
            log("Pickpocketing " + npc.getName());
            lastThievingAttempt = System.currentTimeMillis();

            sleep(600, 1200);
            return 300;
        } else {
            failedInteractionAttempts++;
            log("Failed to interact with " + npc.getName() + " (Attempt " + failedInteractionAttempts + ")");

            if (failedInteractionAttempts >= MAX_FAILED_INTERACTIONS) {
                log("Too many failed interaction attempts. Finding new target.");
                currentTarget = null;
                failedInteractionAttempts = 0;
            }
            return 600;
        }
    }

    /**
     * Handles stealing from stalls or objects
     */
    private int handleObjectThieving(GameObject gameObject) {
        if (System.currentTimeMillis() - lastThievingAttempt < 1200) {
            return 100;
        }

        if (gameObject == null || !gameObject.exists()) {
            log("Target object no longer exists. Finding new target.");
            currentTarget = null;
            return 600;
        }

        if (!gameObject.canReach()) {
            log("Target " + gameObject.getName() + " is not reachable. Finding new target.");
            currentTarget = null;
            return 600;
        }

        String action = "Steal-from";
        boolean isStall = gameObject.getName().equals("Tea Stall") || gameObject.getName().equals("Cake Stall");

        log("Attempting to " + action + " " + gameObject.getName() + " (distance: " + gameObject.distance() + ")");

        // Make sure we're close enough to stall
        if (isStall && gameObject.distance() > 2) {
            org.dreambot.api.methods.walking.impl.Walking.walk(gameObject.getTile());
            sleep(600, 1000);
            log("Moving closer to " + gameObject.getName());
            return 600;
        }

        // Interact with object
        if (gameObject.interact(action)) {
            log("Successfully initiated " + action + " on " + gameObject.getName());
            lastThievingAttempt = System.currentTimeMillis();

            if (isStall) {
                sleep(1200, 1800);
            } else {
                sleep(600, 1200);
            }

            return 300;
        } else {
            failedInteractionAttempts++;
            log("Failed to interact with " + gameObject.getName() + " (Attempt " + failedInteractionAttempts + ")");

            if (failedInteractionAttempts >= MAX_FAILED_INTERACTIONS) {
                log("Too many failed interaction attempts. Finding new target.");
                currentTarget = null;
                failedInteractionAttempts = 0;
            }

            return 600;
        }
    }

    /**
     * Handles full inventory
     */
    private int handleInventory() {
        if (config.isFreestyleMode() || !config.isBankingEnabled()) {
            if (inventoryManager.handleFullInventory()) {
                setState(ThievingState.THIEVING);
                return 300;
            }
        } else {
            setState(ThievingState.WALKING_TO_BANK);
            return 300;
        }

        return 600;
    }

    /**
     * Handles walking to bank
     */
    private int handleWalkingToBank() {
        try {
            if (locationManager.isAtBank()) {
                setState(ThievingState.BANKING);
                return 300;
            }

            log("Walking to nearest bank");
            if (locationManager.walkToBank()) {
                return 1000;
            } else {
                if (locationManager.isStuck()) {
                    log("Multiple bank walk failures. Moving to ERROR state.");
                    locationManager.resetStuckState();
                    setState(ThievingState.ERROR);
                }
                return 1000;
            }
        } catch (Exception e) {
            log("Error in handleWalkingToBank: " + e.getMessage());
            setState(ThievingState.ERROR);
            return 1000;
        }
    }

    /**
     * Handles banking operations
     */
    private int handleBanking() {
        if (!locationManager.isAtBank()) {
            log("Not at bank yet, moving to WALKING_TO_BANK state");
            setState(ThievingState.WALKING_TO_BANK);
            return 300;
        }

        if (inventoryManager.handleBanking()) {
            currentTarget = findThievingTarget();

            if (currentTarget != null) {
                log("Found target near bank. No need to walk to thieving area.");
                setState(ThievingState.THIEVING);
            } else {
                setState(ThievingState.WALKING_TO_LOCATION);
            }
            return 300;
        } else {
            log("Banking failed, retrying...");
        }

        return 600;
    }

    /**
     * Handles health management
     */
    private int handleHealth() {
        if (inventoryManager.hasFood()) {
            if (inventoryManager.eatFood()) {
                log("Eating food to heal");
                sleep(600, 1200);

                if (!inventoryManager.needToHeal()) {
                    currentTarget = findThievingTarget();

                    if (currentTarget != null) {
                        log("Target in view after eating. Continuing thieving.");
                        setState(ThievingState.THIEVING);
                    } else if (locationManager.isAtThievingArea()) {
                        log("At thieving area after eating. Waiting for target.");
                        setState(ThievingState.THIEVING);
                    } else {
                        log("No target visible after eating. Walking to location.");
                        setState(ThievingState.WALKING_TO_LOCATION);
                    }
                }
                return 300;
            }
        } else {
            if (config.isBankingEnabled()) {
                setState(ThievingState.WALKING_TO_BANK);
            } else if (config.isFreestyleMode()) {
                log("No food in freestyle mode. Logging out.");
                stop();
            } else {
                log("WARNING: Low health but no food. Continuing to thieve.");
                setState(ThievingState.THIEVING);
            }
        }

        return 600;
    }

    /**
     * Handles error recovery
     */
    private int handleError() {
        if (System.currentTimeMillis() - lastStateChange > 30000) {
            log("Attempting to recover from error state");
            setState(ThievingState.INITIALIZE);
            return 1000;
        }

        return 5000;
    }

    /**
     * Updates statistics tracking
     */
    private void updateStatistics() {
        if (statsTracker != null && inventoryManager != null) {
            statsTracker.updateInventoryStats(
                    inventoryManager.getFoodEaten(),
                    inventoryManager.getItemsDropped(),
                    inventoryManager.getItemsBanked()
            );
        }
    }

    /**
     * Changes script state
     */
    private void setState(ThievingState newState) {
        if (state != newState) {
            log("State changed: " + state + " -> " + newState);
            state = newState;
            if (statsTracker != null) {
                statsTracker.setCurrentState(newState);
            }
            lastStateChange = System.currentTimeMillis();
        }
    }

    /**
     * Sleeps for a random time between min and max
     */
    private void sleep(int min, int max) {
        try {
            Thread.sleep(min + (int) (Math.random() * (max - min)));
        } catch (InterruptedException e) {
            log("Sleep interrupted: " + e.getMessage());
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

        // Restore default mouse algorithm
        Mouse.setMouseAlgorithm(Mouse.getDefaultMouseAlgorithm());
    }

    // Mouse listener implementation for paint interaction
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
        // Not used
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // Not used
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // Not used
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // Not used
    }
}