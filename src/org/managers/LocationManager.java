package org.managers;

import org.core.config.ScriptConfiguration;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.interactive.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages all location-related functionality including walking to thieving locations and banks
 * Simplified to rely more on DreamBot's built-in web walking
 */
public class LocationManager {
    // Core maps of areas
    private final Map<String, Area> thievingAreas;
    private final Map<String, Area> bankAreas;
    private final ScriptConfiguration config;

    // Constants
    private static final int MAX_DISTANCE_TO_TARGET = 15;
    private static final int MAX_WALK_ATTEMPTS = 3;

    // Special locations
    private final Tile TEA_STALL_SAFESPOT = new Tile(3268, 3410, 0);

    // State tracking
    private int walkAttempts = 0;
    private long lastWalkTime = 0;

    /**
     * Creates a new location manager
     *
     * @param config The script configuration
     */
    public LocationManager(ScriptConfiguration config) {
        this.config = config;
        this.thievingAreas = initializeThievingAreas();
        this.bankAreas = initializeBankAreas();
    }

    /**
     * Walks to the current thieving area
     *
     * @return True if successfully walked or already at location
     */
    public boolean walkToThievingArea() {
        // First check if we're already at the target location
        if (isAtThievingArea()) {
            walkAttempts = 0;
            return true;
        }

        String targetName = config.getCurrentTargetName();
        Area area = getCurrentThievingArea();

        if (area == null) {
            System.out.println("[LocationManager] No area defined for target: " + targetName);
            return false;
        }

        // Special case for Tea Stall - walk to specific safespot
        if (targetName.equals("Tea Stall")) {
            return walkToTeaStallSafespot();
        }

        // For other locations, use the area's center or a logical target
        Tile destination = determineOptimalDestination(area);

        // Enable running if we have energy
        enableRunIfPossible();

        // Use DreamBot's built-in walking algorithm
        System.out.println("[LocationManager] Walking to " + targetName + " area");
        boolean result = Walking.walk(destination);

        // Track walk attempt
        trackWalkAttempt(result);
        return result;
    }

    /**
     * Walks to the Tea Stall safespot
     */
    private boolean walkToTeaStallSafespot() {
        Player player = Players.getLocal();
        if (player.distance(TEA_STALL_SAFESPOT) <= 3) {
            walkAttempts = 0;
            return true;
        }

        System.out.println("[LocationManager] Walking to Tea Stall safespot");
        boolean result = Walking.walkExact(TEA_STALL_SAFESPOT);

        // Track walk attempt
        trackWalkAttempt(result);
        return result;
    }

    /**
     * Walks to the nearest bank
     *
     * @return True if successfully walked or already at bank
     */
    public boolean walkToBank() {
        // Use DreamBot's bank location finder
        if (Bank.isOpen() || isAtBank()) {
            walkAttempts = 0;
            return true;
        }

        // Enable running if we have energy
        enableRunIfPossible();

        System.out.println("[LocationManager] Walking to nearest bank");
        boolean result = Walking.walk(Bank.getClosestBankLocation().getCenter());

        // Track walk attempt
        trackWalkAttempt(result);
        return result;
    }

    /**
     * Enables running if we have enough energy
     */
    private void enableRunIfPossible() {
        if (!Walking.isRunEnabled() && Walking.getRunEnergy() > Walking.getRunThreshold()) {
            Walking.toggleRun();
        }
    }

    /**
     * Tracks walk attempts to detect when player might be stuck
     *
     * @param success Whether the walk attempt succeeded
     */
    private void trackWalkAttempt(boolean success) {
        if (success) {
            walkAttempts = 0;
        } else {
            walkAttempts++;
        }
        lastWalkTime = System.currentTimeMillis();
    }

    /**
     * Checks if player is stuck
     *
     * @return True if player appears to be stuck
     */
    public boolean isStuck() {
        return walkAttempts >= MAX_WALK_ATTEMPTS;
    }

    /**
     * Resets the stuck state
     */
    public void resetStuckState() {
        walkAttempts = 0;
    }

    /**
     * Determines the best destination tile within an area
     *
     * @param area The area to find a destination in
     * @return The optimal destination tile
     */
    private Tile determineOptimalDestination(Area area) {
        String targetName = config.getCurrentTargetName();

        // Check if there's a suitable target already visible
        if (!targetName.toLowerCase().contains("stall")) {
            NPC target = findVisibleNPC(targetName, area);
            if (target != null) {
                return target.getTile();
            }
        } else {
            GameObject stall = findVisibleStall(targetName, area);
            if (stall != null) {
                return stall.getTile();
            }
        }

        // If no targets visible, walk to area center
        return area.getCenter();
    }

    /**
     * Finds a visible NPC target
     */
    private NPC findVisibleNPC(String targetName, Area area) {
        return NPCs.closest(npc ->
                npc.getName().equals(targetName) &&
                        area.contains(npc) &&
                        !npc.isInCombat() &&
                        Walking.canWalk(npc));
    }

    /**
     * Finds a visible stall
     */
    private GameObject findVisibleStall(String targetName, Area area) {
        return GameObjects.closest(obj ->
                obj.getName().equals(targetName) &&
                        area.contains(obj));
    }

    /**
     * Checks if player is at the thieving area
     *
     * @return True if at thieving area
     */
    public boolean isAtThievingArea() {
        String targetName = config.getCurrentTargetName();
        Area area = getCurrentThievingArea();

        if (area == null) {
            return false;
        }

        // Special case for Tea Stall
        if (targetName.equals("Tea Stall")) {
            Player localPlayer = Players.getLocal();
            return localPlayer.distance(TEA_STALL_SAFESPOT) < 3;
        }

        // Check if player is in area
        return area.contains(Players.getLocal());
    }

    /**
     * Checks if a thieving target is available nearby
     *
     * @return True if target is available
     */
    public boolean isThievingTargetAvailable() {
        String targetName = config.getCurrentTargetName();
        Player localPlayer = Players.getLocal();

        if (!isAtThievingArea()) {
            return false;
        }

        // Check for stalls
        if (targetName.equals("Tea Stall") || targetName.equals("Cake Stall")) {
            return GameObjects.closest(obj ->
                    obj.getName().equals(targetName) &&
                            obj.hasAction("Steal-from") &&
                            obj.distance() < MAX_DISTANCE_TO_TARGET) != null;
        }

        // Check for NPCs
        return NPCs.closest(npc ->
                npc.getName().equals(targetName) &&
                        npc.hasAction("Pickpocket") &&
                        !npc.isInCombat() &&
                        npc.distance() < MAX_DISTANCE_TO_TARGET) != null;
    }

    /**
     * Checks if player is at a bank
     *
     * @return True if at bank
     */
    public boolean isAtBank() {
        // Check if bank is open
        if (Bank.isOpen()) {
            return true;
        }

        // Check if player is near a bank
        if (Bank.getClosestBankLocation() != null) {
            return Players.getLocal().distance(Bank.getClosestBankLocation().getCenter()) < 10;
        }

        return false;
    }

    /**
     * Gets the current thieving area
     *
     * @return The current thieving area
     */
    public Area getCurrentThievingArea() {
        String targetName = config.getCurrentTargetName();
        return thievingAreas.getOrDefault(targetName, null);
    }

    /**
     * Initializes all thieving areas
     *
     * @return Map of thieving areas
     */
    private Map<String, Area> initializeThievingAreas() {
        Map<String, Area> areas = new HashMap<>();

        // Lumbridge
        areas.put("Man", new Area(new Tile(3222, 3218, 0), new Tile(3236, 3233, 0)));
        areas.put("Woman", new Area(new Tile(3222, 3218, 0), new Tile(3236, 3233, 0)));

        // Draynor
        areas.put("Farmer", new Area(new Tile(3079, 3246, 0), new Tile(3088, 3258, 0)));
        areas.put("Master Farmer", new Area(new Tile(3073, 3246, 0), new Tile(3082, 3257, 0)));

        // Al Kharid and Varrock
        areas.put("Warrior", new Area(new Tile(3278, 3161, 0), new Tile(3287, 3174, 0)));
        areas.put("Guard", new Area(new Tile(3219, 3380, 0), new Tile(3229, 3394, 0)));

        // Ardougne
        areas.put("Knight of Ardougne", new Area(new Tile(2649, 3280, 0), new Tile(2659, 3287, 0)));
        areas.put("Paladin", new Area(new Tile(2572, 3302, 1), new Tile(2585, 3312, 1)));
        areas.put("Hero", new Area(new Tile(2625, 3286, 0), new Tile(2642, 3297, 0)));

        // Stalls
        areas.put("Tea Stall", new Area(new Tile(3266, 3408, 0), new Tile(3270, 3412, 0)));
        areas.put("Cake Stall", new Area(new Tile(2643, 3295, 0), new Tile(2647, 3299, 0)));

        return areas;
    }

    /**
     * Initializes all bank areas
     *
     * @return Map of bank areas
     */
    private Map<String, Area> initializeBankAreas() {
        Map<String, Area> banks = new HashMap<>();

        // Common bank locations
        banks.put("Lumbridge", new Area(new Tile(3206, 3209, 2), new Tile(3210, 3225, 2)));
        banks.put("Varrock East", new Area(new Tile(3250, 3416, 0), new Tile(3257, 3423, 0)));
        banks.put("Draynor", new Area(new Tile(3092, 3240, 0), new Tile(3097, 3246, 0)));
        banks.put("Al Kharid", new Area(new Tile(3267, 3161, 0), new Tile(3273, 3174, 0)));
        banks.put("Ardougne North", new Area(new Tile(2612, 3330, 0), new Tile(2622, 3336, 0)));

        return banks;
    }
}