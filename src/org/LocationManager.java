package org;

import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.equipment.Equipment;
import org.dreambot.api.methods.container.impl.equipment.EquipmentSlot;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.items.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages thieving locations and walking paths for the script.
 * Updated with optimized Tea Stall functionality
 */
public class LocationManager {
    private final Map<String, Area> thievingAreas;
    private final Map<String, Area> bankAreas;
    private final ScriptConfiguration config;

    // Special safespot coordinates for Tea Stall
    private final Tile TEA_STALL_SAFESPOT = new Tile(3268, 3410, 0);

    // Teleport items
    private final String[] teleportItems = {
            "Amulet of glory",
            "Games necklace",
            "Ring of dueling",
            "Skills necklace",
            "Ring of wealth",
            "Combat bracelet"
    };

    // Location tracking
    private long lastWalkAttempt;
    private int failedWalkAttempts;
    private Tile lastWalkableTile;

    /**
     * Creates a new location manager
     *
     * @param config The script configuration
     */
    public LocationManager(ScriptConfiguration config) {
        this.config = config;
        this.thievingAreas = initializeThievingAreas();
        this.bankAreas = initializeBankAreas();
        this.lastWalkAttempt = 0;
        this.failedWalkAttempts = 0;
        this.lastWalkableTile = null;
    }

    /**
     * Gets the thieving area for the current target
     *
     * @return Area object for the current target
     */
    public Area getCurrentThievingArea() {
        String targetName = config.getCurrentTargetName();
        return thievingAreas.getOrDefault(targetName, null);
    }

    /**
     * Gets the nearest bank area to the current thieving location
     *
     * @return The nearest bank area
     */
    public Area getNearestBankArea() {
        Area currentArea = getCurrentThievingArea();
        if (currentArea == null) {
            return bankAreas.get("Default");
        }

        // Find closest bank to current area
        Area closestBank = null;
        double closestDistance = Double.MAX_VALUE;

        for (Area bankArea : bankAreas.values()) {
            double distance = currentArea.getCenter().distance(bankArea.getCenter());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestBank = bankArea;
            }
        }

        return closestBank != null ? closestBank : bankAreas.get("Default");
    }

    /**
     * Walks to the current thieving area
     *
     * @return True if already at the area or successfully walking to it
     */
    public boolean walkToThievingArea() {
        String targetName = config.getCurrentTargetName();
        Area area = getCurrentThievingArea();

        if (area == null) {
            System.out.println("[LocationManager] No thieving area defined for " + targetName);
            return false;
        }

        Player localPlayer = Players.getLocal();

        // Special case for Tea Stall - walk to the specific safespot
        if (targetName.equals("Tea Stall")) {
            if (localPlayer.distance(TEA_STALL_SAFESPOT) > 3) {
                System.out.println("[LocationManager] Walking to Tea Stall safespot: " + TEA_STALL_SAFESPOT);
                return Walking.walk(TEA_STALL_SAFESPOT);
            }
            // We're already at the tea stall safespot
            return true;
        }

        // Normal handling for other targets
        if (!area.contains(localPlayer)) {
            // Check for teleport if far away
            if (shouldUseTeleport(area)) {
                if (useTeleport(area)) {
                    System.out.println("[LocationManager] Used teleport to get closer to destination");
                    return true;
                }
            }

            // Calculate where in the area to walk based on target type
            Tile destination = determineOptimalDestination(area);

            // Track this walk attempt
            lastWalkAttempt = System.currentTimeMillis();

            // Check if we're stuck
            if (isPlayerStuck()) {
                // Try a different tile if we're stuck
                destination = findAlternativeWalkableTile(area);
                System.out.println("[LocationManager] Possibly stuck, trying alternative tile: " + destination);
            }

            if (destination != null) {
                boolean walkResult = Walking.walk(destination);

                if (!walkResult) {
                    failedWalkAttempts++;
                    System.out.println("[LocationManager] Walk failed (Attempt " + failedWalkAttempts + ")");
                } else {
                    failedWalkAttempts = 0;
                    lastWalkableTile = destination;
                }

                return walkResult;
            } else {
                System.out.println("[LocationManager] Failed to find valid destination in thieving area");
                return false;
            }
        }
        return true;
    }

    /**
     * Determines if the player appears to be stuck
     *
     * @return True if player seems stuck
     */
    private boolean isPlayerStuck() {
        // If we've had multiple failed walk attempts
        if (failedWalkAttempts >= 3) {
            return true;
        }

        // If we've been trying to walk for a long time
        long walkTime = System.currentTimeMillis() - lastWalkAttempt;
        if (walkTime > 10000 && failedWalkAttempts > 0) { // 10 seconds
            return true;
        }

        return false;
    }

    /**
     * Finds an alternative tile to walk to when stuck
     *
     * @param area The target area
     * @return An alternative tile to try
     */
    private Tile findAlternativeWalkableTile(Area area) {
        // Get current player position
        Player localPlayer = Players.getLocal();
        Tile playerTile = localPlayer.getTile();

        // Try different tiles in the area
        List<Tile> areaTiles = List.of(area.getTiles());

        // Sort tiles by distance to player
        areaTiles.sort((t1, t2) ->
                (int) (t1.distance(playerTile) - t2.distance(playerTile)));

        // Try up to 5 different tiles
        for (int i = 0; i < Math.min(5, areaTiles.size()); i++) {
            Tile tile = areaTiles.get(i);

            // Skip the last tile we tried
            if (lastWalkableTile != null && tile.equals(lastWalkableTile)) {
                continue;
            }

            if (tile.canReach()) {
                return tile;
            }
        }

        // If we still can't find a reachable tile, try a random one
        return area.getRandomTile();
    }

    /**
     * Determines the optimal destination within a thieving area
     *
     * @param area The area to search within
     * @return The optimal tile to walk to
     */
    private Tile determineOptimalDestination(Area area) {
        String targetName = config.getCurrentTargetName();
        Player localPlayer = Players.getLocal();

        // Special case for Tea Stall
        if (targetName.equals("Tea Stall")) {
            return TEA_STALL_SAFESPOT;
        }

        // For Cake Stall, find the actual stall
        if (targetName.equals("Cake Stall")) {
            // Look for any game object containing "cake" and "stall" in the name
            List<GameObject> cakeStalls = GameObjects.all(obj ->
                    obj.getName().toLowerCase().contains("cake") &&
                            obj.getName().toLowerCase().contains("stall") &&
                            area.contains(obj));

            if (!cakeStalls.isEmpty()) {
                // Sort by distance
                cakeStalls.sort((s1, s2) -> (int) (s1.distance() - s2.distance()));

                // Get the closest stall
                GameObject closestStall = cakeStalls.get(0);

                // Find a tile near the stall
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        Tile nearbyTile = new Tile(
                                closestStall.getTile().getX() + x,
                                closestStall.getTile().getY() + y,
                                closestStall.getTile().getZ());

                        if (nearbyTile.canReach()) {
                            return nearbyTile;
                        }
                    }
                }

                return closestStall.getTile();
            }
        }

        // For NPCs, try to find a spot near an actual NPC
        if (!targetName.toLowerCase().contains("stall") &&
                !targetName.equals("Chest") &&
                !targetName.equals("Wall Safe") &&
                !targetName.equals("Pyramid Plunder")) {

            List<NPC> reachableNPCs = new ArrayList<>();

            // Get all matching NPCs in area
            List<NPC> allNPCs = NPCs.all(npc ->
                    npc.getName().equals(targetName) &&
                            area.contains(npc) &&
                            !npc.isInCombat());

            // Filter for reachable ones
            for (NPC npc : allNPCs) {
                if (npc.canReach()) {
                    reachableNPCs.add(npc);
                }
            }

            // Sort by distance
            reachableNPCs.sort((npc1, npc2) ->
                    (int) (npc1.distance(localPlayer) - npc2.distance(localPlayer)));

            // Find a tile near the closest reachable NPC
            if (!reachableNPCs.isEmpty()) {
                NPC target = reachableNPCs.get(0);

                // Get a tile near the NPC but not exactly on it
                Tile npcTile = target.getTile();

                // Try several nearby tiles to find a reachable one
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        if (x == 0 && y == 0) continue; // Skip the exact NPC tile

                        Tile nearbyTile = new Tile(npcTile.getX() + x, npcTile.getY() + y, npcTile.getZ());
                        if (nearbyTile.canReach()) {
                            return nearbyTile;
                        }
                    }
                }

                // If no nearby tile is reachable, use the NPC's tile
                return npcTile;
            }
        }

        // Default: try to find any reachable tile in the area
        List<Tile> reachableTiles = new ArrayList<>();
        for (Tile tile : area.getTiles()) {
            if (tile.canReach()) {
                reachableTiles.add(tile);
            }
        }

        if (!reachableTiles.isEmpty()) {
            // Sort by distance
            reachableTiles.sort((t1, t2) ->
                    (int) (t1.distance(localPlayer) - t2.distance(localPlayer)));

            return reachableTiles.get(0);
        }

        // Last resort: just return the center of the area
        return area.getCenter();
    }

    /**
     * Walks to the nearest bank
     *
     * @return True if already at the bank or successfully walking to it
     */
    public boolean walkToBank() {
        Area bank = getNearestBankArea();
        if (bank == null) {
            System.out.println("[LocationManager] Could not find a suitable bank area");
            return false;
        }

        Player localPlayer = Players.getLocal();
        if (!bank.contains(localPlayer)) {
            // Try to find a reachable tile in the bank area
            List<Tile> reachableTiles = new ArrayList<>();
            for (Tile tile : bank.getTiles()) {
                if (tile.canReach()) {
                    reachableTiles.add(tile);
                }
            }

            if (!reachableTiles.isEmpty()) {
                // Sort by distance
                reachableTiles.sort((t1, t2) ->
                        (int) (t1.distance(localPlayer) - t2.distance(localPlayer)));

                return Walking.walk(reachableTiles.get(0));
            }

            // If no reachable tiles found, use default walk
            return Walking.walk(bank.getRandomTile());
        }
        return true;
    }

    /**
     * Checks if the player is at the current thieving area
     *
     * @return True if at the thieving area
     */
    public boolean isAtThievingArea() {
        String targetName = config.getCurrentTargetName();
        Area area = getCurrentThievingArea();

        if (area == null) {
            return false;
        }

        // Special case for Tea Stall - check if at the safespot
        if (targetName.equals("Tea Stall")) {
            Player localPlayer = Players.getLocal();
            return localPlayer.distance(TEA_STALL_SAFESPOT) < 3;
        }

        // Standard check for other targets
        Player localPlayer = Players.getLocal();
        return area.contains(localPlayer);
    }

    /**
     * Checks if a thieving target is available at the current location
     * More specific than just checking if we're in the area
     *
     * @return True if a valid target exists in the vicinity
     */
    public boolean isThievingTargetAvailable() {
        String targetName = config.getCurrentTargetName();
        Player localPlayer = Players.getLocal();

        // If we're not even in the thieving area, definitely no target available
        if (!isAtThievingArea()) {
            return false;
        }

        // Special case for Tea Stall - more flexible detection
        if (targetName.equals("Tea Stall")) {
            // Look for any tea stall within a reasonable radius of the safespot
            List<GameObject> teaStalls = GameObjects.all(obj ->
                    obj.getName().toLowerCase().contains("tea") &&
                            obj.getName().toLowerCase().contains("stall") &&
                            obj.distance(TEA_STALL_SAFESPOT) < 5);

            if (!teaStalls.isEmpty()) {
                System.out.println("[LocationManager] Found Tea Stall at safespot");

                // Print debug info about the tea stall
                GameObject teaStall = teaStalls.get(0);
                String[] actions = teaStall.getActions();
                String actionsList = actions != null ? String.join(", ", actions) : "null";
                System.out.println("[LocationManager] Tea Stall details: " + teaStall.getName() +
                        " | ID: " + teaStall.getID() +
                        " | Distance: " + teaStall.distance() +
                        " | Actions: " + actionsList);

                return true;
            }

            System.out.println("[LocationManager] At Tea Stall location but no stall found nearby");
            return false;
        }

        // Check for cake stall specifically
        if (targetName.equals("Cake Stall")) {
            GameObject target = GameObjects.closest(obj ->
                    obj.getName().toLowerCase().contains("cake") &&
                            obj.getName().toLowerCase().contains("stall") &&
                            obj.distance(localPlayer) < 7);

            if (target != null) {
                System.out.println("[LocationManager] Found Cake Stall at distance: " + target.distance());
                return true;
            } else {
                System.out.println("[LocationManager] At Cake Stall location but no stall found nearby");
                return false;
            }
        }

        // Check for NPC targets
        else if (!targetName.equals("Chest") && !targetName.equals("Wall Safe") && !targetName.equals("Pyramid Plunder")) {
            NPC target = NPCs.closest(npc ->
                    npc.getName().equals(targetName) &&
                            npc.hasAction("Pickpocket") &&
                            npc.distance(localPlayer) < 15 &&
                            !npc.isInCombat() &&
                            npc.canReach());

            return target != null;
        }
        // Special cases
        else if (targetName.equals("Chest")) {
            GameObject target = GameObjects.closest(obj ->
                    obj.getName().toLowerCase().contains("chest") &&
                            obj.hasAction("Open") &&
                            obj.distance(localPlayer) < 15 &&
                            obj.canReach());

            return target != null;
        } else if (targetName.equals("Wall Safe")) {
            GameObject target = GameObjects.closest(obj ->
                    obj.getName().toLowerCase().contains("wall safe") &&
                            obj.hasAction("Crack") &&
                            obj.distance(localPlayer) < 15 &&
                            obj.canReach());

            return target != null;
        } else if (targetName.equals("Pyramid Plunder")) {
            return isAtThievingArea();
        }

        return false;
    }

    /**
     * Checks if the player is at a bank
     *
     * @return True if at a bank
     */
    public boolean isAtBank() {
        Player localPlayer = Players.getLocal();
        for (Area bankArea : bankAreas.values()) {
            if (bankArea.contains(localPlayer)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Initializes all thieving areas
     * Updated with precise Tea Stall location
     *
     * @return Map of target names to areas
     */
    private Map<String, Area> initializeThievingAreas() {
        Map<String, Area> areas = new HashMap<>();

        // Men/Women locations
        areas.put("Man", new Area(
                new Tile(3222, 3218, 0),
                new Tile(3236, 3233, 0)
        )); // Lumbridge

        areas.put("Woman", new Area(
                new Tile(3222, 3218, 0),
                new Tile(3236, 3233, 0)
        )); // Same as Man

        // Farmers
        areas.put("Farmer", new Area(
                new Tile(3079, 3246, 0),
                new Tile(3088, 3258, 0)
        )); // Draynor

        // Master Farmers
        areas.put("Master Farmer", new Area(
                new Tile(3073, 3246, 0),
                new Tile(3082, 3257, 0)
        )); // Draynor

        // Warriors
        areas.put("Warrior", new Area(
                new Tile(3278, 3161, 0),
                new Tile(3287, 3174, 0)
        )); // Al Kharid

        // Guards
        areas.put("Guard", new Area(
                new Tile(3219, 3380, 0),
                new Tile(3229, 3394, 0)
        )); // Varrock

        // Knights of Ardougne
        areas.put("Knight of Ardougne", new Area(
                new Tile(2649, 3280, 0),
                new Tile(2659, 3287, 0)
        )); // East Ardougne market

        // Paladins
        areas.put("Paladin", new Area(
                new Tile(2572, 3302, 0),
                new Tile(2585, 3312, 1)
        )); // Ardougne castle

        // Heroes
        areas.put("Hero", new Area(
                new Tile(2625, 3286, 0),
                new Tile(2642, 3297, 0)
        )); // Ardougne

        // Tea Stall - more precise area centered on the safespot
        areas.put("Tea Stall", new Area(
                new Tile(3266, 3408, 0),
                new Tile(3270, 3412, 0)
        )); // Varrock - focused on the exact tea stall location

        // Cake Stall - more precise area
        areas.put("Cake Stall", new Area(
                new Tile(2643, 3295, 0),
                new Tile(2647, 3299, 0)
        )); // Ardougne market - more precise area

        return areas;
    }

    /**
     * Initializes all bank areas
     *
     * @return Map of bank names to areas
     */
    private Map<String, Area> initializeBankAreas() {
        Map<String, Area> banks = new HashMap<>();

        // Add common bank locations
        banks.put("Lumbridge", new Area(
                new Tile(3206, 3209, 2),
                new Tile(3210, 3225, 2)
        ));

        banks.put("Varrock East", new Area(
                new Tile(3250, 3416, 0),
                new Tile(3257, 3423, 0)
        ));

        banks.put("Varrock West", new Area(
                new Tile(3180, 3433, 0),
                new Tile(3190, 3448, 0)
        ));

        banks.put("Draynor", new Area(
                new Tile(3092, 3240, 0),
                new Tile(3097, 3246, 0)
        ));

        banks.put("Al Kharid", new Area(
                new Tile(3267, 3161, 0),
                new Tile(3273, 3174, 0)
        ));

        banks.put("Falador East", new Area(
                new Tile(3009, 3353, 0),
                new Tile(3018, 3359, 0)
        ));

        banks.put("Falador West", new Area(
                new Tile(2943, 3368, 0),
                new Tile(2948, 3373, 0)
        ));

        banks.put("Ardougne North", new Area(
                new Tile(2612, 3330, 0),
                new Tile(2622, 3336, 0)
        ));

        banks.put("Ardougne South", new Area(
                new Tile(2649, 3280, 0),
                new Tile(2657, 3287, 0)
        ));

        // Default bank (Lumbridge)
        banks.put("Default", banks.get("Lumbridge"));

        return banks;
    }

    // Rest of the methods remain unchanged - shouldUseTeleport, hasTeleportItem, useTeleport, etc.
    // Including them here for completeness

    /**
     * Checks if we should use a teleport to reach a destination
     */
    public boolean shouldUseTeleport(Area destination) {
        if (destination == null) {
            return false;
        }

        Player localPlayer = Players.getLocal();
        double distance = localPlayer.distance(destination.getCenter());

        // Only use teleports for long distances
        if (distance > 50) {
            // Check if we have teleport items
            for (String teleportItem : teleportItems) {
                if (hasTeleportItem(teleportItem)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if player has a specific teleport item
     */
    private boolean hasTeleportItem(String itemName) {
        // Check inventory
        if (Inventory.contains(item ->
                item != null && item.getName().contains(itemName))) {
            return true;
        }

        // Check equipment (glory, etc.)
        if (Equipment.contains(item ->
                item != null && item.getName().contains(itemName))) {
            return true;
        }

        return false;
    }

    /**
     * Uses an appropriate teleport item to get closer to destination
     */
    public boolean useTeleport(Area destination) {
        if (destination == null) {
            return false;
        }

        // Determine destination region (very simplified)
        String region = determineRegion(destination);
        System.out.println("[LocationManager] Trying to teleport to region: " + region);

        // Try glory first (most useful)
        if (region.equals("Draynor") || region.equals("Al Kharid") ||
                region.equals("Edgeville") || region.equals("Karamja")) {

            // Try equipped glory
            Item glory = Equipment.getItemInSlot(EquipmentSlot.AMULET.getSlot());
            if (glory != null && glory.getName().contains("Amulet of glory")) {
                System.out.println("[LocationManager] Using equipped glory");
                if (glory.interact("Rub")) {
                    sleep(1000, 2000);

                    // Select destination based on region
                    int option = 0;
                    if (region.equals("Draynor")) option = 1;
                    else if (region.equals("Al Kharid")) option = 2;
                    else if (region.equals("Edgeville")) option = 3;
                    else if (region.equals("Karamja")) option = 4;

                    // Click the option if valid using Dialogues
                    if (option > 0 && Dialogues.canContinue()) {
                        Dialogues.continueDialogue();
                        sleep(600, 1000);

                        if (Dialogues.chooseOption(option)) {
                            sleep(2000, 3000); // Wait for teleport
                            return true;
                        }
                    }
                }
            }

            // Try inventory glory
            Item invGlory = Inventory.get(item ->
                    item != null && item.getName().contains("Amulet of glory"));

            if (invGlory != null) {
                System.out.println("[LocationManager] Using inventory glory");
                if (invGlory.interact("Rub")) {
                    sleep(1000, 2000);

                    // Select destination based on region
                    int option = 0;
                    if (region.equals("Draynor")) option = 1;
                    else if (region.equals("Al Kharid")) option = 2;
                    else if (region.equals("Edgeville")) option = 3;
                    else if (region.equals("Karamja")) option = 4;

                    // Click the option if valid using Dialogues
                    if (option > 0 && Dialogues.canContinue()) {
                        Dialogues.continueDialogue();
                        sleep(600, 1000);

                        if (Dialogues.chooseOption(option)) {
                            sleep(2000, 3000); // Wait for teleport
                            return true;
                        }
                    }
                }
            }
        }

        // Other teleport implementations can be added here

        return false;
    }

    /**
     * Determines the region of a destination area for teleport selection
     */
    private String determineRegion(Area destination) {
        Tile center = destination.getCenter();

        // Very simplified region detection
        if (center.getX() >= 3070 && center.getX() <= 3100 &&
                center.getY() >= 3230 && center.getY() <= 3260) {
            return "Draynor";
        } else if (center.getX() >= 3260 && center.getX() <= 3290 &&
                center.getY() >= 3150 && center.getY() <= 3180) {
            return "Al Kharid";
        } else if (center.getX() >= 3080 && center.getX() <= 3110 &&
                center.getY() >= 3480 && center.getY() <= 3510) {
            return "Edgeville";
        } else if (center.getX() >= 2800 && center.getX() <= 2900 &&
                center.getY() >= 3000 && center.getY() <= 3100) {
            return "Karamja";
        } else if (center.getX() >= 2640 && center.getX() <= 2680 &&
                center.getY() >= 3270 && center.getY() <= 3310) {
            return "Ardougne";
        } else if (center.getX() >= 3200 && center.getX() <= 3230 &&
                center.getY() >= 3200 && center.getY() <= 3240) {
            return "Lumbridge";
        } else if (center.getX() >= 3200 && center.getX() <= 3240 &&
                center.getY() >= 3400 && center.getY() <= 3440) {
            return "Varrock";
        }

        return "Unknown";
    }

    /**
     * Sleeps for a random time between min and max milliseconds
     */
    private void sleep(int min, int max) {
        try {
            Thread.sleep(min + (int) (Math.random() * (max - min)));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}