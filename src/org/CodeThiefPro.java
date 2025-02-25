package org;

import org.dreambot.api.Client;
import org.dreambot.api.data.GameState;
import org.dreambot.api.input.Mouse;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.SkillTracker;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.listener.ChatListener;
import org.dreambot.api.script.listener.PaintListener;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.items.Item;
import org.dreambot.api.wrappers.widgets.message.Message;
import org.gui.GUI;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ScriptManifest(
        name = "CodeThief Pro",
        author = "Calle",
        version = 1.1,
        description = "Advanced thieving script with auto-progression",
        category = Category.THIEVING
)
public class CodeThiefPro extends AbstractScript implements PaintListener, ChatListener {
    // Core components
    private ThievingState state;
    private ScriptConfiguration config;
    private LocationManager locationManager;
    private InventoryManager inventoryManager;
    private AntiBanManager antiBanManager;
    private StatisticsTracker statsTracker;
    private GUI gui;

    // Tracking variables
    private long lastThievingAttempt;
    private long lastStateChange;
    private long lastTargetSearch;
    private int consecutiveFailures;
    private boolean scriptStarted;
    private Object currentTarget;
    private boolean cantReachMessageReceived;
    private int failedInteractionAttempts;

    // Constants
    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    private static final int COIN_POUCH_THRESHOLD = 12; // Opening threshold for coin pouches
    private static final int TARGET_REFRESH_INTERVAL = 5000; // 5 seconds between target searches
    private static final int MAX_FAILED_INTERACTIONS = 5; // Max failed interaction attempts before finding new target

    @Override
    public void onStart() {
        try {
            // Initialize all components
            config = new ScriptConfiguration();

            // Create a flag to track configuration status
            final boolean[] configComplete = new boolean[1];
            configComplete[0] = false;

            // Wait for the GUI to be created and shown
            SwingUtilities.invokeAndWait(() -> {
                try {
                    gui = new GUI(this, config);

                    // Add window listener to detect when GUI is closed
                    gui.addWindowListener(new java.awt.event.WindowAdapter() {
                        @Override
                        public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                            log("GUI window closed");
                            // Only mark as complete if properly configured
                            configComplete[0] = config.isConfigured();
                        }
                    });

                    gui.setVisible(true);
                } catch (Exception e) {
                    log("Error creating GUI: " + e.getMessage());
                    e.printStackTrace();
                }
            });

            // Wait for configuration to complete with timeout
            int timeout = 0;
            int maxTimeout = 1200; // 10 minutes (500ms * 1200 = 600000ms = 10min)

            log("Waiting for user to configure script...");

            while (!configComplete[0] && timeout < maxTimeout) {
                // Check if config has been marked as configured (via Start button)
                if (config.isConfigured()) {
                    configComplete[0] = true;
                    log("Configuration completed via Start button");
                    break;
                }

                // Check if GUI is still visible (but don't stop if invisible and not configured yet)
                if (gui != null && !gui.isVisible() && timeout > 10) {
                    // Give a small grace period before checking if GUI is invisible
                    if (!config.isConfigured()) {
                        log("GUI closed without configuring. Stopping script.");
                        stop();
                        return;
                    }
                }

                sleep(500);
                timeout++;

                // Log progress every minute
                if (timeout % 120 == 0) {
                    log("Still waiting for configuration... " + (timeout / 120) + " minutes elapsed");
                }
            }

            // Check if timeout occurred
            if (timeout >= maxTimeout) {
                log("Configuration timed out after 10 minutes. Stopping script.");
                stop();
                return;
            }

            // Double-check if config is actually configured
            if (!config.isConfigured()) {
                log("Script not properly configured. Stopping.");
                stop();
                return;
            }

            // Log selected target for debugging
            log("Starting script with target: " + config.getCurrentTargetName());

            // Initialize remaining components with configuration
            locationManager = new LocationManager(config);
            inventoryManager = new InventoryManager(config);
            antiBanManager = new AntiBanManager(config);
            statsTracker = new StatisticsTracker();

            // Initialize skill tracker
            SkillTracker.start(Skill.THIEVING);

            // Log food items for debugging
            log("Configured food items: " + config.getFoodItems().toString());

            // Set initial state
            setState(ThievingState.INITIALIZE);

            // Initialize tracking variables
            lastThievingAttempt = System.currentTimeMillis();
            lastStateChange = System.currentTimeMillis();
            lastTargetSearch = 0;
            consecutiveFailures = 0;
            scriptStarted = true;
            currentTarget = null;
            cantReachMessageReceived = false;
            failedInteractionAttempts = 0;

            log("CodeThief Pro started successfully!");
        } catch (Exception e) {
            log("Error in onStart: " + e.getMessage());
            e.printStackTrace();
            stop();
        }
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
            // Reset unreachable flag at beginning of loop
            cantReachMessageReceived = false;

            // Anti-ban random checks
            antiBanManager.performRandomChecks();

            // Execute current state
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
        } catch (Exception e) {
            log("Error in main loop: " + e.getMessage());
            e.printStackTrace();
            setState(ThievingState.ERROR);
            return 1000;
        }
    }

    @Override
    public void onMessage(Message message) {
        String msg = message.getMessage().toLowerCase();

        // Check for unreachable target messages
        if (msg.contains("can't reach that") || msg.contains("too far away")) {
            cantReachMessageReceived = true;
            failedInteractionAttempts++;
            log("Received unreachable message: " + message.getMessage());

            // If we've had multiple unreachable messages, force target refresh
            if (failedInteractionAttempts >= MAX_FAILED_INTERACTIONS) {
                log("Too many failed interaction attempts. Forcing target refresh.");
                currentTarget = null;
                failedInteractionAttempts = 0;
            }
        }

        // Check for successful pickpocketing messages
        if (msg.contains("you pick") || msg.contains("you steal")) {
            statsTracker.onSuccessfulThieve();
            consecutiveFailures = 0;
            failedInteractionAttempts = 0;
        }

        // Check for failed pickpocketing messages
        if (msg.contains("you fail") || msg.contains("you're stunned")) {
            statsTracker.onFailedThieve();
            consecutiveFailures++;
        }

        // Check for lack of inventory space
        if (msg.contains("inventory is too full")) {
            setState(ThievingState.HANDLING_INVENTORY);
        }
    }

    /**
     * Handles the initialize state
     *
     * @return Sleep time in ms
     */
    private int handleInitialize() {
        // Get current thieving level
        int thievingLevel = Skills.getRealLevel(Skill.THIEVING);

        // If auto-progression is enabled, update target based on level
        if (config.isAutoProgressionEnabled()) {
            String optimalTarget = config.determineOptimalTarget(thievingLevel);
            config.setCurrentTargetName(optimalTarget);
            log("Auto-progression set target to: " + optimalTarget);
        }

        // Update statistics tracker with current target
        statsTracker.setCurrentTarget(config.getCurrentTargetName());

        // Check if we need food and have none
        if (inventoryManager.needToHeal() && !inventoryManager.hasFood() && config.isBankingEnabled()) {
            log("Need to heal but no food available. Going to bank first.");
            setState(ThievingState.WALKING_TO_BANK);
            return 300;
        }

        // Find current target before deciding to walk
        currentTarget = findThievingTarget();

        // If target is already visible/accessible, go straight to thieving
        if (currentTarget != null) {
            log("Target already in view. Starting thieving without walking.");
            setState(ThievingState.THIEVING);
            return 300;
        }

        // No target visible, need to walk to location
        setState(ThievingState.WALKING_TO_LOCATION);
        return 300;
    }

    /**
     * Handles walking to thieving location
     *
     * @return Sleep time in ms
     */
    private int handleWalkingToLocation() {
        // Try to find a target before walking (might already be in range)
        currentTarget = findThievingTarget();
        if (currentTarget != null) {
            log("Found target while preparing to walk. Skipping walk.");
            setState(ThievingState.THIEVING);
            return 300;
        }

        // Check if health is low before traveling
        if (inventoryManager.needToHeal()) {
            setState(ThievingState.HANDLING_HEALTH);
            return 300;
        }

        // Check if already at thieving area
        if (locationManager.isAtThievingArea()) {
            log("Arrived at thieving location");
            setState(ThievingState.THIEVING);
            return 300;
        }

        if (locationManager.walkToThievingArea()) {
            log("Walking to thieving location: " + config.getCurrentTargetName());
            return 1000;
        } else {
            log("Failed to walk to thieving location");
            setState(ThievingState.ERROR);
            return 1000;
        }
    }

    /**
     * Handles thieving actions
     *
     * @return Sleep time in ms
     */
    private int handleThieving() {
        // Check if inventory is full
        if (inventoryManager.isInventoryFull()) {
            setState(ThievingState.HANDLING_INVENTORY);
            return 300;
        }

        // Check if health is low
        if (inventoryManager.needToHeal()) {
            setState(ThievingState.HANDLING_HEALTH);
            return 300;
        }

        // Check for coin pouches first
        if (handleCoinPouches()) {
            return 600; // Wait a moment after opening pouches
        }

        // If we don't have a current target or need to refresh it
        if (currentTarget == null || shouldRefreshTarget()) {
            currentTarget = findThievingTarget();
        }

        if (currentTarget != null) {
            // Handle different target types
            if (currentTarget instanceof NPC) {
                return handleNPCThieving((NPC) currentTarget);
            } else if (currentTarget instanceof GameObject) {
                return handleObjectThieving((GameObject) currentTarget);
            }
        }

        // No target found but we're in the right area, check if we're actually there
        if (!locationManager.isThievingTargetAvailable()) {
            log("No thieving target found. Walking to location.");
            setState(ThievingState.WALKING_TO_LOCATION);
            return 600;
        }

        // We're in the right area but no target found, wait for respawn
        log("Waiting for target to appear");
        return 1000;
    }

    /**
     * Handles coin pouches in inventory
     *
     * @return True if pouches were opened
     */
    private boolean handleCoinPouches() {
        int pouchCount = Inventory.count("Coin pouch");
        if (pouchCount >= COIN_POUCH_THRESHOLD) {
            log("Opening " + pouchCount + " coin pouches");
            Item pouch = Inventory.get("Coin pouch");
            if (pouch != null) {
                // Try to open all pouches at once if possible
                if (pouch.hasAction("Open-all")) {
                    return pouch.interact("Open-all");
                } else {
                    return pouch.interact("Open");
                }
            }
        }
        return false;
    }

    /**
     * Determines if we should find a new target
     *
     * @return True if target should be refreshed
     */
    private boolean shouldRefreshTarget() {
        // Check if we received a "can't reach that" message
        if (cantReachMessageReceived) {
            return true;
        }

        // Periodically refresh target to find better ones
        long timeSinceLastSearch = System.currentTimeMillis() - lastTargetSearch;
        if (timeSinceLastSearch > TARGET_REFRESH_INTERVAL) {
            return true;
        }

        // Check if current target is still valid
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
     * Finds a thieving target based on current configuration
     *
     * @return NPC or GameObject to thieve from
     */
    private Object findThievingTarget() {
        lastTargetSearch = System.currentTimeMillis();
        String targetName = config.getCurrentTargetName();

        System.out.println("[CodeThiefPro] Searching for target: " + targetName);

        // Specialized handling for Tea and Cake stalls
        if (targetName.equals("Tea Stall") || targetName.equals("Cake Stall")) {
            GameObject stall = GameObjects.closest(obj ->
                    obj.getName().equals(targetName) &&
                            obj.hasAction("Steal-from"));

            if (stall != null) {
                System.out.println("[CodeThiefPro] Found " + targetName + " at " + stall.getTile() + ", distance: " + stall.distance());
                // For Tea Stall, ensure we're at the right location (the safespot)
                if (targetName.equals("Tea Stall")) {
                    Player player = Players.getLocal();
                    Tile teaStallSafespot = new Tile(3268, 3410, 0);

                    // If we're not at the safespot, we need to get there first
                    if (player.distance(teaStallSafespot) > 3) {
                        System.out.println("[CodeThiefPro] Not at Tea Stall safespot yet. Need to walk there first.");
                        return null; // This will trigger walkToLocation
                    }
                }
                return stall;
            } else {
                System.out.println("[CodeThiefPro] Could not find " + targetName);
            }
            return null;
        }
        // Other non-stall thieving methods
        else if (!targetName.toLowerCase().contains("stall")) {
            // Handle NPC pickpocketing or other methods
            return findNPC(targetName);
        }

        return null;
    }

    /**
     * Finds an NPC to pickpocket with reachability check
     *
     * @param targetName The NPC name
     * @return The closest valid and reachable NPC
     */
    private NPC findNPC(String targetName) {
        // First try to find a reachable NPC
        List<NPC> reachableNPCs = new ArrayList<>();

        // Get all matching NPCs
        List<NPC> allNPCs = NPCs.all(npc ->
                npc.getName().equals(targetName) &&
                        !npc.isInCombat() &&
                        npc.hasAction("Pickpocket") &&
                        npc.getInteractingCharacter() == null &&
                        npc.exists());

        // Filter for reachable ones and sort by distance
        for (NPC npc : allNPCs) {
            if (npc.canReach()) {
                reachableNPCs.add(npc);
            }
        }

        // Sort by distance
        reachableNPCs.sort((npc1, npc2) ->
                (int) (npc1.distance() - npc2.distance()));

        // Get closest reachable NPC
        if (!reachableNPCs.isEmpty()) {
            NPC closestReachable = reachableNPCs.get(0);
            log("Found reachable " + targetName + " at distance " + String.format("%.1f", closestReachable.distance()));
            return closestReachable;
        }

        // Log that we found NPCs but none are reachable
        if (!allNPCs.isEmpty()) {
            log("Found " + allNPCs.size() + " " + targetName + "(s) but none are reachable. Try repositioning.");
        }

        return null;
    }

    /**
     * Finds a stall to steal from with reachability check
     *
     * @param stallName The stall name
     * @return The closest valid and reachable stall
     */
    private GameObject findStallObject(String stallName) {
        List<GameObject> reachableStalls = new ArrayList<>();

        // Get all matching stalls
        List<GameObject> allStalls = GameObjects.all(obj ->
                obj.getName().equals(stallName) &&
                        obj.hasAction("Steal-from") &&
                        obj.exists());

        // Filter for reachable ones and sort by distance
        for (GameObject obj : allStalls) {
            if (obj.canReach()) {
                reachableStalls.add(obj);
            }
        }

        // Sort by distance
        reachableStalls.sort((obj1, obj2) ->
                (int) (obj1.distance() - obj2.distance()));

        // Get closest reachable stall
        if (!reachableStalls.isEmpty()) {
            return reachableStalls.get(0);
        }

        // Log that we found stalls but none are reachable
        if (!allStalls.isEmpty()) {
            log("Found " + allStalls.size() + " " + stallName + "(s) but none are reachable. Try repositioning.");
        }

        return null;
    }

    /**
     * Finds a chest to open with reachability check
     *
     * @return The closest valid and reachable chest
     */
    private GameObject findChest() {
        List<GameObject> reachableChests = new ArrayList<>();

        // Get all matching chests
        List<GameObject> allChests = GameObjects.all(obj ->
                obj.getName().toLowerCase().contains("chest") &&
                        obj.hasAction("Open") &&
                        obj.exists());

        // Filter for reachable ones and sort by distance
        for (GameObject obj : allChests) {
            if (obj.canReach()) {
                reachableChests.add(obj);
            }
        }

        // Sort by distance
        reachableChests.sort((obj1, obj2) ->
                (int) (obj1.distance() - obj2.distance()));

        // Get closest reachable chest
        if (!reachableChests.isEmpty()) {
            return reachableChests.get(0);
        }

        return null;
    }

    /**
     * Finds a wall safe to crack with reachability check
     *
     * @return The closest valid and reachable wall safe
     */
    private GameObject findWallSafe() {
        List<GameObject> reachableSafes = new ArrayList<>();

        // Get all matching safes
        List<GameObject> allSafes = GameObjects.all(obj ->
                obj.getName().toLowerCase().contains("wall safe") &&
                        obj.hasAction("Crack") &&
                        obj.exists());

        // Filter for reachable ones and sort by distance
        for (GameObject obj : allSafes) {
            if (obj.canReach()) {
                reachableSafes.add(obj);
            }
        }

        // Sort by distance
        reachableSafes.sort((obj1, obj2) ->
                (int) (obj1.distance() - obj2.distance()));

        // Get closest reachable safe
        if (!reachableSafes.isEmpty()) {
            return reachableSafes.get(0);
        }

        return null;
    }

    /**
     * Handles NPC pickpocketing
     *
     * @param npc The NPC to pickpocket
     * @return Sleep time in ms
     */
    private int handleNPCThieving(NPC npc) {
        // Check if enough time has passed since last attempt
        if (System.currentTimeMillis() - lastThievingAttempt < 1200) {
            return 100;
        }

        // Check if NPC is valid and reachable
        if ((npc == null || !npc.exists() || npc.isInCombat() || !npc.canReach() && npc.distance() < 5)) {
            if (npc != null) {
                if (!npc.canReach()) {
                    log("Target " + npc.getName() + " is not reachable. Finding new target.");
                } else if (npc.isInCombat()) {
                    log("Target " + npc.getName() + " is in combat. Finding new target.");
                }
            }
            currentTarget = null; // Reset target so we find a new one
            return 600;
        }

        // Occasionally add a misclick for more human-like behavior
        if (antiBanManager.shouldMisclick()) {
            log("Intentional misclick");
            int currentX = npc.getX();
            int currentY = npc.getY();
            Point randomPoint = antiBanManager.getRandomPointNear(currentX, currentY, 5);
            Mouse.move(randomPoint);
            Mouse.click(false); // Left click
            sleep(300, 600);
            lastThievingAttempt = System.currentTimeMillis();
            return 1000;
        }

        // Attempt to pickpocket
        if (npc.distance() < 8) {
            if (npc.interact("Pickpocket")) {
                log("Pickpocketing " + npc.getName());
                lastThievingAttempt = System.currentTimeMillis();

                // Wait for pickpocketing animation
                sleep(600, 1200);

                // Check for failure or success - now handled in onMessage

                return 300;
            } else {
                failedInteractionAttempts++;
                log("Failed to interact with " + npc.getName() + " (Attempt " + failedInteractionAttempts + ")");
                if (failedInteractionAttempts >= MAX_FAILED_INTERACTIONS) {
                    log("Too many failed interaction attempts. Finding new target.");
                    //    currentTarget = null;
                    //    failedInteractionAttempts = 0;
                }
            }
        }


        return 600;
    }

    /**
     * Handles object thieving (stalls, chests, etc.)
     *
     * @param gameObject The object to steal from
     * @return Sleep time in ms
     */
    private int handleObjectThieving(GameObject gameObject) {
        // Check if enough time has passed since last attempt
        if (System.currentTimeMillis() - lastThievingAttempt < 1200) {
            return 100;
        }

        // Check if object is valid and reachable
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

        // Get the appropriate action based on object type
        String action = "Steal-from";
        if (gameObject.getName().toLowerCase().contains("chest")) {
            action = "Open";
        } else if (gameObject.getName().toLowerCase().contains("safe")) {
            action = "Crack";
        }

        // Explicitly handle Tea Stall and Cake Stall
        boolean isStall = gameObject.getName().equals("Tea Stall") || gameObject.getName().equals("Cake Stall");

        log("Attempting to " + action + " " + gameObject.getName() + " (distance: " + gameObject.distance() + ")");

        // Move closer to stall if needed (for stalls specifically)
        if (isStall && gameObject.distance() > 2) {
            Walking.walk(gameObject.getTile());
            sleep(600, 1000);
            log("Moving closer to " + gameObject.getName());
            return 600;
        }

        // Attempt to interact with the object
        if (gameObject.interact(action)) {
            log("Successfully initiated " + action + " on " + gameObject.getName());
            lastThievingAttempt = System.currentTimeMillis();

            // Wait longer for stalls
            if (isStall) {
                sleep(1200, 1800); // Longer wait for stall animation
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

            // For stalls specifically, try a different approach if failing
            if (isStall) {
                log("Repositioning for stall interaction");
                Tile stallTile = gameObject.getTile();

                // Try to find a better position around the stall
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        if (x == 0 && y == 0) continue; // Skip the stall's exact position

                        Tile nearbyTile = new Tile(stallTile.getX() + x, stallTile.getY() + y, stallTile.getZ());
                        if (nearbyTile.canReach()) {
                            Walking.walk(nearbyTile);
                            sleep(800, 1200);
                            break;
                        }
                    }
                }
            }
        }

        return 600;
    }

    /**
     * Handles inventory management
     *
     * @return Sleep time in ms
     */
    private int handleInventory() {
        if (config.isFreestyleMode() || !config.isBankingEnabled()) {
            // Handle inventory without banking
            if (inventoryManager.handleFullInventory()) {
                setState(ThievingState.THIEVING);
                return 300;
            }
        } else {
            // Need to bank
            setState(ThievingState.WALKING_TO_BANK);
            return 300;
        }

        return 600;
    }

    /**
     * Handles walking to bank
     *
     * @return Sleep time in ms
     */
    private int handleWalkingToBank() {
        if (locationManager.isAtBank()) {
            setState(ThievingState.BANKING);
            return 300;
        }

        if (locationManager.walkToBank()) {
            return 1000;
        } else {
            log("Failed to walk to bank");
            setState(ThievingState.ERROR);
            return 1000;
        }
    }

    /**
     * Handles banking operations
     *
     * @return Sleep time in ms
     */
    private int handleBanking() {
        // First check if we're at a bank
        if (!locationManager.isAtBank()) {
            log("Not at bank yet, moving to WALKING_TO_BANK state");
            setState(ThievingState.WALKING_TO_BANK);
            return 300;
        }

        if (inventoryManager.handleBanking()) {
            // After banking, check if a target is nearby before walking
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
     * Handles player health
     *
     * @return Sleep time in ms
     */
    private int handleHealth() {
        if (inventoryManager.hasFood()) {
            if (inventoryManager.eatFood()) {
                log("Eating food to heal");
                sleep(600, 1200);

                // If health is now ok, determine where to go next
                if (!inventoryManager.needToHeal()) {
                    // Check if we have a valid target to continue thieving
                    currentTarget = findThievingTarget();

                    if (currentTarget != null) {
                        // Target is visible, go directly to thieving
                        log("Target in view after eating. Continuing thieving.");
                        setState(ThievingState.THIEVING);
                    } else if (locationManager.isAtThievingArea()) {
                        // We're in the right area but no visible target
                        log("At thieving area after eating. Waiting for target.");
                        setState(ThievingState.THIEVING);
                    } else {
                        // Need to walk to thieving area
                        log("No target visible after eating. Walking to location.");
                        setState(ThievingState.WALKING_TO_LOCATION);
                    }
                }
                return 300;
            }
        } else {
            // No food, check if banking is enabled
            if (config.isBankingEnabled()) {
                setState(ThievingState.WALKING_TO_BANK);
            } else if (config.isFreestyleMode()) {
                // In freestyle mode with no food, logout
                log("No food in freestyle mode. Logging out.");
                stop();
            } else {
                // No banking, no freestyle. Just continue thieving but warn
                log("WARNING: Low health but no food. Continuing to thieve.");
                setState(ThievingState.THIEVING);
            }
        }

        return 600;
    }

    /**
     * Handles error state
     *
     * @return Sleep time in ms
     */
    private int handleError() {
        // Check how long we've been in error state
        if (System.currentTimeMillis() - lastStateChange > 30000) {
            // After 30 seconds in error, try to recover by reinitializing
            log("Attempting to recover from error state");
            setState(ThievingState.INITIALIZE);
            return 1000;
        }

        return 5000; // Wait in error state
    }

    /**
     * Checks if the player is stunned
     *
     * @return True if stunned
     */
    private boolean isPlayerStunned() {
        // Simple check for stunned animation
        Player local = Players.getLocal();
        int animationId = local.getAnimation();

        // Common stun animation IDs (may need to be updated)
        return animationId == 422 || animationId == 836;
    }

    /**
     * Checks if the player is in combat
     *
     * @return True if in combat
     */
    private boolean isInCombat() {
        return Players.getLocal().isInCombat();
    }

    /**
     * Updates the current script state
     *
     * @param newState The new state
     */
    private void setState(ThievingState newState) {
        if (state != newState) {
            log("State changed: " + state + " -> " + newState);
            state = newState;
            statsTracker.setCurrentState(newState);
            lastStateChange = System.currentTimeMillis();
        }
    }

    /**
     * Sleeps for a random time between min and max milliseconds
     *
     * @param min Minimum sleep time
     * @param max Maximum sleep time
     */
    private void sleep(int min, int max) {
        try {
            Thread.sleep(min + (int) (Math.random() * (max - min)));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPaint(Graphics graphics) {
        // Add debugging for paint issues
        try {
            if (statsTracker != null) {
                statsTracker.drawPaint((Graphics2D) graphics);
            } else {
                // Draw a simple indicator if statsTracker isn't initialized
                graphics.setColor(Color.RED);
                graphics.drawString("CodeThief Pro - Stats tracker not initialized", 10, 50);
            }
        } catch (Exception e) {
            // If any error occurs in painting, catch it and show an error message
            graphics.setColor(Color.RED);
            graphics.drawString("Paint Error: " + e.getMessage(), 10, 70);
        }
    }

    /**
     * Opens the GUI when clicked on the paint
     */
    public void openGUIFromPaint() {
        if (gui == null || !gui.isVisible()) {
            SwingUtilities.invokeLater(() -> {
                if (gui == null) {
                    gui = new GUI(this, config);
                }
                gui.setVisible(true);
            });
        }
    }

    /**
     * Called when the script stops
     */
    @Override
    public void onExit() {
        // Clean up resources
        log("Script stopped. Final stats:");
        log("XP Gained: " + statsTracker.getXpGained());
        log("Success Rate: " + String.format("%.1f%%", statsTracker.getSuccessRate()));

        // Close GUI if open
        if (gui != null && gui.isVisible()) {
            gui.dispose();
        }
    }
}