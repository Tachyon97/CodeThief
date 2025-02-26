package org;

import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
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

import java.util.*;

/**
 * Manages thieving locations and walking paths for the script.
 * Optimized for teleport handling and robust bank navigation.
 */
public class LocationManager {
    private final Map<String, Area> thievingAreas;
    private final Map<String, Area> bankAreas;
    private final ScriptConfiguration config;

    // Special safespot coordinates for Tea Stall
    private final Tile TEA_STALL_SAFESPOT = new Tile(3268, 3410, 0);

    // Location tracking
    private long lastWalkAttempt;
    private int failedWalkAttempts;
    private Tile lastWalkableTile;
    private int consecutiveBankFailures = 0;
    private long lastTeleportAttempt = 0;
    private static final long TELEPORT_COOLDOWN = 60000; // 1 minute cooldown on teleport attempts

    // Teleport items with their respective options
    private static final Map<String, List<TeleportOption>> TELEPORT_ITEMS = new HashMap<>();

    static {
        // Glory teleports
        List<TeleportOption> gloryOptions = new ArrayList<>();
        gloryOptions.add(new TeleportOption("Edgeville", 1, new Tile(3087, 3496, 0)));
        gloryOptions.add(new TeleportOption("Draynor", 2, new Tile(3105, 3251, 0)));
        gloryOptions.add(new TeleportOption("Al Kharid", 3, new Tile(3293, 3174, 0)));
        gloryOptions.add(new TeleportOption("Karamja", 4, new Tile(2918, 3176, 0)));
        TELEPORT_ITEMS.put("Amulet of glory", gloryOptions);

        // Games necklace
        List<TeleportOption> gamesOptions = new ArrayList<>();
        gamesOptions.add(new TeleportOption("Burthorpe", 1, new Tile(2899, 3554, 0)));
        gamesOptions.add(new TeleportOption("Barbarian Outpost", 2, new Tile(2520, 3571, 0)));
        gamesOptions.add(new TeleportOption("Corporeal Beast", 3, new Tile(2965, 4382, 2)));
        gamesOptions.add(new TeleportOption("Tears of Guthix", 4, new Tile(3245, 9500, 0)));
        TELEPORT_ITEMS.put("Games necklace", gamesOptions);

        // Ring of dueling
        List<TeleportOption> duelingOptions = new ArrayList<>();
        duelingOptions.add(new TeleportOption("Castle Wars", 1, new Tile(2440, 3090, 0)));
        duelingOptions.add(new TeleportOption("Ferox Enclave", 2, new Tile(3148, 3636, 0)));
        duelingOptions.add(new TeleportOption("Soul Wars", 3, new Tile(2209, 2864, 0)));
        TELEPORT_ITEMS.put("Ring of dueling", duelingOptions);

        // Skills necklace
        List<TeleportOption> skillsOptions = new ArrayList<>();
        skillsOptions.add(new TeleportOption("Fishing Guild", 1, new Tile(2610, 3391, 0)));
        skillsOptions.add(new TeleportOption("Mining Guild", 2, new Tile(3049, 9762, 0)));
        skillsOptions.add(new TeleportOption("Crafting Guild", 3, new Tile(2933, 3292, 0)));
        skillsOptions.add(new TeleportOption("Cooking Guild", 4, new Tile(3144, 3443, 0)));
        skillsOptions.add(new TeleportOption("Woodcutting Guild", 5, new Tile(1662, 3505, 0)));
        skillsOptions.add(new TeleportOption("Farming Guild", 6, new Tile(1249, 3719, 0)));
        TELEPORT_ITEMS.put("Skills necklace", skillsOptions);
    }

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
        String closestBankName = null;
        double closestDistance = Double.MAX_VALUE;

        for (Map.Entry<String, Area> entry : bankAreas.entrySet()) {
            double distance = currentArea.getCenter().distance(entry.getValue().getCenter());
            // Add a small random factor to avoid comparison issues when distances are equal
            distance += Math.random() * 0.001;

            if (distance < closestDistance) {
                closestDistance = distance;
                closestBankName = entry.getKey();
            }
        }

        return bankAreas.get(closestBankName != null ? closestBankName : "Default");
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
            if (localPlayer.distance(TEA_STALL_SAFESPOT) <= 3) {
                return true; // Already at tea stall safespot
            }

            System.out.println("[LocationManager] Walking to Tea Stall safespot: " + TEA_STALL_SAFESPOT);
            // Try to walk exactly to the safespot for precision
            return Walking.walkExact(TEA_STALL_SAFESPOT);
        }

        // Normal handling for other targets
        if (area.contains(localPlayer)) {
            return true; // Already in the right area
        }

        // Check for teleport if far away
        if (shouldUseTeleport(area)) {
            if (useTeleport(area)) {
                System.out.println("[LocationManager] Used teleport to get closer to destination");
                return true;
            }
        }

        // Calculate where in the area to walk
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
            // Ensure run is enabled for efficiency if we have enough energy
            if (!Walking.isRunEnabled() && Walking.getRunEnergy() > Walking.getRunThreshold()) {
                Walking.toggleRun();
            }

            // Walk to the destination - this uses web/local pathfinding as needed
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

        // If we haven't moved in a while and have a destination
        Tile destination = Walking.getDestination();
        if (destination != null) {
            Player local = Players.getLocal();
            if (local.distance(destination) > 5 && !local.isMoving() && walkTime > 5000) {
                return true;
            }
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

        // Create list for reachable tiles
        List<Tile> reachableTiles = new ArrayList<>();

        // Check each tile in the area to find reachable ones
        for (Tile tile : area.getTiles()) {
            // Use walking API to check if we can walk to it
            if (Walking.canWalk(tile)) {
                reachableTiles.add(tile);
            }
        }

        // If no reachable tiles, try to find any walkable tile in the general direction
        if (reachableTiles.isEmpty()) {
            Tile areaCenter = area.getCenter();
            int dx = areaCenter.getX() - playerTile.getX();
            int dy = areaCenter.getY() - playerTile.getY();

            // Create a tile that's a few steps in the general direction
            int stepSize = 5;
            int stepX = dx > 0 ? stepSize : (dx < 0 ? -stepSize : 0);
            int stepY = dy > 0 ? stepSize : (dy < 0 ? -stepSize : 0);

            Tile stepTile = new Tile(
                    playerTile.getX() + stepX,
                    playerTile.getY() + stepY,
                    playerTile.getZ()
            );

            // Use getClosestTileOnMap to find a walkable version of this tile
            Tile mapTile = Walking.getClosestTileOnMap(stepTile);
            if (mapTile != null) {
                return mapTile;
            }

            return stepTile;
        }

        // Sort tiles by distance with a stable approach (no risk of comparison issues)
        List<TileDistance> tileDistances = new ArrayList<>();
        for (Tile tile : reachableTiles) {
            double distance = tile.distance(playerTile);
            // Add a tiny random factor to ensure no identical distances
            distance += Math.random() * 0.001;
            tileDistances.add(new TileDistance(tile, distance));
        }

        // Sort by distance
        Collections.sort(tileDistances, Comparator.comparingDouble(TileDistance::getDistance));

        // Skip the last tile we tried
        if (lastWalkableTile != null) {
            tileDistances.removeIf(td -> td.getTile().equals(lastWalkableTile));
        }

        return tileDistances.isEmpty() ? area.getCenter() : tileDistances.get(0).getTile();
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
            GameObject cakeStall = GameObjects.closest(obj ->
                    obj.getName().toLowerCase().contains("cake") &&
                            obj.getName().toLowerCase().contains("stall") &&
                            area.contains(obj));

            if (cakeStall != null) {
                // Find a valid tile near the stall
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        Tile nearbyTile = new Tile(
                                cakeStall.getTile().getX() + x,
                                cakeStall.getTile().getY() + y,
                                cakeStall.getTile().getZ()
                        );

                        if (Walking.canWalk(nearbyTile)) {
                            return nearbyTile;
                        }
                    }
                }
                return cakeStall.getTile();
            }
        }

        // For NPCs, try to find a spot near an actual NPC
        if (!targetName.toLowerCase().contains("stall") &&
                !targetName.equals("Chest") &&
                !targetName.equals("Wall Safe") &&
                !targetName.equals("Pyramid Plunder")) {

            NPC target = NPCs.closest(npc ->
                    npc.getName().equals(targetName) &&
                            area.contains(npc) &&
                            !npc.isInCombat() &&
                            Walking.canWalk(npc));

            if (target != null) {
                // Get a tile near the NPC but not exactly on it
                Tile npcTile = target.getTile();

                // Try several nearby tiles to find a reachable one
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        if (x == 0 && y == 0) continue; // Skip the exact NPC tile

                        Tile nearbyTile = new Tile(
                                npcTile.getX() + x,
                                npcTile.getY() + y,
                                npcTile.getZ()
                        );

                        if (Walking.canWalk(nearbyTile)) {
                            return nearbyTile;
                        }
                    }
                }
                return npcTile;
            }
        }

        // Find any walkable tile in the area
        List<TileDistance> walkableTiles = new ArrayList<>();
        for (Tile tile : area.getTiles()) {
            if (Walking.canWalk(tile)) {
                double distance = tile.distance(localPlayer);
                // Add tiny random factor to ensure no comparison issues
                distance += Math.random() * 0.001;
                walkableTiles.add(new TileDistance(tile, distance));
            }
        }

        if (!walkableTiles.isEmpty()) {
            // Sort by distance
            Collections.sort(walkableTiles, Comparator.comparingDouble(TileDistance::getDistance));
            return walkableTiles.get(0).getTile();
        }

        // Last resort: use the center of the area
        return Walking.getClosestTileOnMap(area.getCenter());
    }

    /**
     * Walks to the nearest bank
     *
     * @return True if already at the bank or successfully walking to it
     */
    public boolean walkToBank() {
        try {
            // First check if we're already at a bank
            if (Bank.isOpen() || Bank.isOpen()) {
                System.out.println("[LocationManager] Already at bank");
                consecutiveBankFailures = 0;
                return true;
            }

            // Then check if in a bank area
            Area bank = getNearestBankArea();
            Player localPlayer = Players.getLocal();

            if (bank != null && bank.contains(localPlayer)) {
                System.out.println("[LocationManager] In bank area");
                consecutiveBankFailures = 0;
                return true;
            }

            // Ensure run is enabled for efficiency if we have enough energy
            if (!Walking.isRunEnabled() && Walking.getRunEnergy() > Walking.getRunThreshold()) {
                Walking.toggleRun();
            }

            // Walk to nearest bank booth - this should work in most cases
            if (Bank.getClosestBankLocation() != null) {
                System.out.println("[LocationManager] Walking to nearest bank booth");
                boolean result = Walking.walk(Bank.getClosestBankLocation().getCenter());

                if (result) {
                    consecutiveBankFailures = 0;
                    return true;
                }
            }

            // If bank booth walking failed, try our manual bank areas
            if (bank != null) {
                // Find a walkable tile in the bank area
                Tile walkableTile = null;

                for (Tile tile : bank.getTiles()) {
                    if (Walking.canWalk(tile)) {
                        // If we can directly walk to this tile, use it
                        walkableTile = tile;
                        break;
                    }
                }

                // If we didn't find a directly walkable tile, use the center
                if (walkableTile == null) {
                    walkableTile = Walking.getClosestTileOnMap(bank.getCenter());
                }

                if (walkableTile != null) {
                    System.out.println("[LocationManager] Walking to bank area at: " + walkableTile);
                    boolean result = Walking.walk(walkableTile);

                    if (result) {
                        consecutiveBankFailures = 0;
                        return true;
                    }
                }
            }

            // If all else fails, try clicking a random bank tile on the minimap
            if (bank != null) {
                Tile randomTile = bank.getRandomTile();
                System.out.println("[LocationManager] Last resort: Clicking bank area on minimap");
                boolean result = Walking.clickTileOnMinimap(randomTile);

                if (result) {
                    consecutiveBankFailures = 0;
                    return true;
                }
            }

            // All walking attempts failed
            System.out.println("[LocationManager] All bank walking attempts failed");
            consecutiveBankFailures++;
            return false;

        } catch (Exception e) {
            System.out.println("[LocationManager] Error in walkToBank: " + e.getMessage());
            e.printStackTrace();
            consecutiveBankFailures++;
            return false;
        }
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

        // Special case for Tea Stall
        if (targetName.equals("Tea Stall")) {
            List<GameObject> teaStalls = GameObjects.all(obj ->
                    obj.getName().toLowerCase().contains("tea") &&
                            obj.getName().toLowerCase().contains("stall") &&
                            obj.distance(TEA_STALL_SAFESPOT) < 5);

            return !teaStalls.isEmpty();
        }

        // Check for cake stall specifically
        if (targetName.equals("Cake Stall")) {
            GameObject target = GameObjects.closest(obj ->
                    obj.getName().toLowerCase().contains("cake") &&
                            obj.getName().toLowerCase().contains("stall") &&
                            obj.distance(localPlayer) < 7);

            return target != null;
        }

        // Check for NPC targets
        if (!targetName.equals("Chest") && !targetName.equals("Wall Safe") && !targetName.equals("Pyramid Plunder")) {
            NPC target = NPCs.closest(npc ->
                    npc.getName().equals(targetName) &&
                            npc.hasAction("Pickpocket") &&
                            npc.distance(localPlayer) < 15 &&
                            !npc.isInCombat() &&
                            Walking.canWalk(npc));

            return target != null;
        }

        // Special cases
        if (targetName.equals("Chest")) {
            GameObject target = GameObjects.closest(obj ->
                    obj.getName().toLowerCase().contains("chest") &&
                            obj.hasAction("Open") &&
                            obj.distance(localPlayer) < 15 &&
                            Walking.canWalk(obj));

            return target != null;

        } else if (targetName.equals("Wall Safe")) {
            GameObject target = GameObjects.closest(obj ->
                    obj.getName().toLowerCase().contains("wall safe") &&
                            obj.hasAction("Crack") &&
                            obj.distance(localPlayer) < 15 &&
                            Walking.canWalk(obj));

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
        // First check if the bank is open or nearby
        if (Bank.isOpen() || Bank.isOpen()) {
            return true;
        }

        // Then check if in a bank area
        Player localPlayer = Players.getLocal();
        for (Area bankArea : bankAreas.values()) {
            if (bankArea.contains(localPlayer)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets teleport items from bank and manages bank interactions
     *
     * @return True if teleport items were successfully obtained
     */
    public boolean getTeleportItemsFromBank() {
        try {
            // Check if we have teleport items already
            for (String teleportItem : TELEPORT_ITEMS.keySet()) {
                if (hasTeleportItem(teleportItem)) {
                    System.out.println("[LocationManager] Already have teleport item: " + teleportItem);
                    return true;
                }
            }

            // Need to get teleport items from bank
            if (!Bank.isOpen()) {
                if (!Bank.open()) {
                    System.out.println("[LocationManager] Failed to open bank");
                    return false;
                }
                sleep(600, 900); // Wait for bank to open
            }

            // Check for each teleport item in bank
            for (String teleportItem : TELEPORT_ITEMS.keySet()) {
                // Look for items containing the name (with any charges)
                Item bankItem = Bank.get(item ->
                        item != null &&
                                item.getName().toLowerCase().contains(teleportItem.toLowerCase()));

                if (bankItem != null) {
                    System.out.println("[LocationManager] Found teleport item in bank: " + bankItem.getName());
                    if (Bank.withdraw(bankItem.getName(), 1)) {
                        sleep(600, 900);
                        return true;
                    }
                }
            }

            System.out.println("[LocationManager] No teleport items found in bank");
            return false;

        } catch (Exception e) {
            System.out.println("[LocationManager] Error getting teleport items: " + e.getMessage());
            return false;
        }
    }

    /**
     * Initializes all thieving areas
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
        )); // Ardougne market

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

    /**
     * Checks if we should use a teleport to reach a destination
     *
     * @param destination The destination area
     * @return True if we should use a teleport
     */
    public boolean shouldUseTeleport(Area destination) {
        if (destination == null) {
            return false;
        }

        // Don't teleport too frequently
        if (System.currentTimeMillis() - lastTeleportAttempt < TELEPORT_COOLDOWN) {
            return false;
        }

        Player localPlayer = Players.getLocal();
        double distance = localPlayer.distance(destination.getCenter());

        // Only use teleports for long distances
        if (distance > 50) {
            // Check if we have teleport items
            for (String teleportItem : TELEPORT_ITEMS.keySet()) {
                if (hasTeleportItem(teleportItem)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if player has a specific teleport item
     *
     * @param itemName The teleport item name
     * @return True if player has the item
     */
    private boolean hasTeleportItem(String itemName) {
        // Check inventory for any item containing the name
        if (Inventory.contains(item ->
                item != null && item.getName().toLowerCase().contains(itemName.toLowerCase()))) {
            return true;
        }

        // Check equipment (glory, etc.)
        if (Equipment.contains(item ->
                item != null && item.getName().toLowerCase().contains(itemName.toLowerCase()))) {
            return true;
        }

        return false;
    }

    /**
     * Uses an appropriate teleport item to get closer to destination
     *
     * @param destination The destination area
     * @return True if teleport successful
     */
    public boolean useTeleport(Area destination) {
        if (destination == null) {
            return false;
        }

        lastTeleportAttempt = System.currentTimeMillis();
        String region = determineRegion(destination);
        System.out.println("[LocationManager] Trying to teleport to region: " + region);

        // Find the closest teleport option to our destination
        TeleportOption bestOption = findBestTeleportOption(destination);
        if (bestOption == null) {
            System.out.println("[LocationManager] No suitable teleport option found");
            return false;
        }

        System.out.println("[LocationManager] Best teleport option: " + bestOption.getLocationName());

        // Find the teleport item for this option
        String teleportItemName = TELEPORT_ITEMS.entrySet().stream().filter(entry -> entry.getValue().contains(bestOption)).findFirst().map(Map.Entry::getKey).orElse(null);

        if (teleportItemName == null) {
            System.out.println("[LocationManager] Could not find teleport item for option");
            return false;
        }

        // Try equipped item first
        Item equipped = Equipment.getItemInSlot(EquipmentSlot.AMULET.getSlot());
        if (equipped != null && equipped.getName().toLowerCase().contains(teleportItemName.toLowerCase())) {
            if (equipped.interact("Rub") || equipped.interact("Teleport")) {
                System.out.println("[LocationManager] Rubbing equipped " + equipped.getName());
                sleep(1000, 2000);

                if (selectDialogueOption(bestOption.getDialogueOption())) {
                    sleep(3000, 4000); // Wait for teleport
                    return true;
                }
            }
        }

        // Try inventory item
        Item invItem = Inventory.get(item ->
                item != null && item.getName().toLowerCase().contains(teleportItemName.toLowerCase()));

        if (invItem != null) {
            System.out.println("[LocationManager] Using inventory " + invItem.getName());
            if (invItem.interact("Rub") || invItem.interact("Teleport")) {
                sleep(1000, 2000);

                if (selectDialogueOption(bestOption.getDialogueOption())) {
                    sleep(3000, 4000); // Wait for teleport
                    return true;
                }
            }
        }

        System.out.println("[LocationManager] Failed to use teleport");
        return false;
    }

    /**
     * Finds the best teleport option for a destination
     *
     * @param destination The destination area
     * @return The best teleport option
     */
    private TeleportOption findBestTeleportOption(Area destination) {
        Tile destCenter = destination.getCenter();
        TeleportOption bestOption = null;
        double bestDistance = Double.MAX_VALUE;

        for (List<TeleportOption> options : TELEPORT_ITEMS.values()) {
            for (TeleportOption option : options) {
                double distance = option.getLocation().distance(destCenter);
                // Add a tiny random factor to avoid comparison issues
                distance += Math.random() * 0.001;

                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestOption = option;
                }
            }
        }

        return bestOption;
    }

    /**
     * Selects a dialogue option
     *
     * @param option The dialogue option index (1-based)
     * @return True if option was selected
     */
    private boolean selectDialogueOption(int option) {
        try {
            // Try multiple ways to select dialogue option
            if (Dialogues.canContinue()) {
                Dialogues.continueDialogue();
                sleep(600, 1000);
            }

            if (Dialogues.isProcessing()) {
                sleep(600, 1000);
            }

            if (Dialogues.chooseOption(option)) {
                System.out.println("[LocationManager] Selected dialogue option " + option);
                return true;
            }

            System.out.println("[LocationManager] Failed to select dialogue option");
            return false;
        } catch (Exception e) {
            System.out.println("[LocationManager] Error in dialogue: " + e.getMessage());
            return false;
        }
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

    /**
     * Helper class to store a tile with its distance for stable sorting
     */
    private static class TileDistance {
        private final Tile tile;
        private final double distance;

        public TileDistance(Tile tile, double distance) {
            this.tile = tile;
            this.distance = distance;
        }

        public Tile getTile() {
            return tile;
        }

        public double getDistance() {
            return distance;
        }
    }

    /**
     * Represents a teleport option for a teleport item
     */
    private static class TeleportOption {
        private final String locationName;
        private final int dialogueOption;
        private final Tile location;

        public TeleportOption(String locationName, int dialogueOption, Tile location) {
            this.locationName = locationName;
            this.dialogueOption = dialogueOption;
            this.location = location;
        }

        public String getLocationName() {
            return locationName;
        }

        public int getDialogueOption() {
            return dialogueOption;
        }

        public Tile getLocation() {
            return location;
        }
    }
}