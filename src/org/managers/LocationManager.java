package org.managers;

import org.core.config.ScriptConfiguration;
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

public class LocationManager {
    private final Map<String, Area> thievingAreas;
    private final Map<String, Area> bankAreas;
    private final ScriptConfiguration config;

    private final Tile TEA_STALL_SAFESPOT = new Tile(3268, 3410, 0);

    private long lastWalkAttempt;
    private int failedWalkAttempts;
    private Tile lastWalkableTile;
    private int consecutiveBankFailures = 0;
    private long lastTeleportAttempt = 0;
    private static final long TELEPORT_COOLDOWN = 60000;

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

        // Ardougne teleport
        List<TeleportOption> ardyOptions = new ArrayList<>();
        ardyOptions.add(new TeleportOption("Ardougne", 1, new Tile(2661, 3307, 0)));
        TELEPORT_ITEMS.put("Ardougne teleport", ardyOptions);

        // Combat bracelet
        List<TeleportOption> combatOptions = new ArrayList<>();
        combatOptions.add(new TeleportOption("Warriors' Guild", 1, new Tile(2878, 3546, 0)));
        combatOptions.add(new TeleportOption("Champions' Guild", 2, new Tile(3191, 3368, 0)));
        combatOptions.add(new TeleportOption("Monastery", 3, new Tile(3053, 3487, 0)));
        combatOptions.add(new TeleportOption("Ranging Guild", 4, new Tile(2654, 3442, 0)));
        TELEPORT_ITEMS.put("Combat bracelet", combatOptions);

        // Ardougne cloak
        List<TeleportOption> cloakOptions = new ArrayList<>();
        cloakOptions.add(new TeleportOption("Ardougne", 1, new Tile(2634, 3348, 0)));
        TELEPORT_ITEMS.put("Ardougne cloak", cloakOptions);
    }

    public LocationManager(ScriptConfiguration config) {
        this.config = config;
        this.thievingAreas = initializeThievingAreas();
        this.bankAreas = initializeBankAreas();
        this.lastWalkAttempt = 0;
        this.failedWalkAttempts = 0;
        this.lastWalkableTile = null;
    }

    public Area getCurrentThievingArea() {
        String targetName = config.getCurrentTargetName();
        return thievingAreas.getOrDefault(targetName, null);
    }

    public Area getNearestBankArea() {
        Area currentArea = getCurrentThievingArea();
        if (currentArea == null) {
            return bankAreas.get("Default");
        }

        String closestBankName = null;
        double closestDistance = Double.MAX_VALUE;

        for (Map.Entry<String, Area> entry : bankAreas.entrySet()) {
            double distance = currentArea.getCenter().distance(entry.getValue().getCenter());
            distance += Math.random() * 0.001;

            if (distance < closestDistance) {
                closestDistance = distance;
                closestBankName = entry.getKey();
            }
        }

        return bankAreas.get(closestBankName != null ? closestBankName : "Default");
    }

    public boolean walkToThievingArea() {
        String targetName = config.getCurrentTargetName();
        Area area = getCurrentThievingArea();

        if (area == null) {
            System.out.println("[LocationManager] No thieving area defined for " + targetName);
            return false;
        }

        Player localPlayer = Players.getLocal();

        // Special case for Tea Stall
        if (targetName.equals("Tea Stall")) {
            if (localPlayer.distance(TEA_STALL_SAFESPOT) <= 3) {
                return true;
            }

            System.out.println("[LocationManager] Walking to Tea Stall safespot: " + TEA_STALL_SAFESPOT);
            return Walking.walkExact(TEA_STALL_SAFESPOT);
        }

        // Normal handling for other targets
        if (area.contains(localPlayer)) {
            return true;
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
            destination = findAlternativeWalkableTile(area);
            System.out.println("[LocationManager] Possibly stuck, trying alternative tile: " + destination);
        }

        if (destination != null) {
            // Ensure run is enabled for efficiency if we have enough energy
            if (!Walking.isRunEnabled() && Walking.getRunEnergy() > Walking.getRunThreshold()) {
                Walking.toggleRun();
            }

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

    private boolean isPlayerStuck() {
        if (failedWalkAttempts >= 3) {
            return true;
        }

        long walkTime = System.currentTimeMillis() - lastWalkAttempt;
        if (walkTime > 10000 && failedWalkAttempts > 0) {
            return true;
        }

        Tile destination = Walking.getDestination();
        if (destination != null) {
            Player local = Players.getLocal();
            if (local.distance(destination) > 5 && !local.isMoving() && walkTime > 5000) {
                return true;
            }
        }

        return false;
    }

    private Tile findAlternativeWalkableTile(Area area) {
        Player localPlayer = Players.getLocal();
        Tile playerTile = localPlayer.getTile();

        List<Tile> reachableTiles = new ArrayList<>();

        for (Tile tile : area.getTiles()) {
            if (Walking.canWalk(tile)) {
                reachableTiles.add(tile);
            }
        }

        if (reachableTiles.isEmpty()) {
            Tile areaCenter = area.getCenter();
            int dx = areaCenter.getX() - playerTile.getX();
            int dy = areaCenter.getY() - playerTile.getY();

            int stepSize = 5;
            int stepX = dx > 0 ? stepSize : (dx < 0 ? -stepSize : 0);
            int stepY = dy > 0 ? stepSize : (dy < 0 ? -stepSize : 0);

            Tile stepTile = new Tile(
                    playerTile.getX() + stepX,
                    playerTile.getY() + stepY,
                    playerTile.getZ()
            );

            Tile mapTile = Walking.getClosestTileOnMap(stepTile);
            if (mapTile != null) {
                return mapTile;
            }

            return stepTile;
        }

        List<TileDistance> tileDistances = new ArrayList<>();
        for (Tile tile : reachableTiles) {
            double distance = tile.distance(playerTile);
            distance += Math.random() * 0.001;
            tileDistances.add(new TileDistance(tile, distance));
        }

        Collections.sort(tileDistances, Comparator.comparingDouble(TileDistance::getDistance));

        if (lastWalkableTile != null) {
            tileDistances.removeIf(td -> td.getTile().equals(lastWalkableTile));
        }

        return tileDistances.isEmpty() ? area.getCenter() : tileDistances.get(0).getTile();
    }

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
                Tile npcTile = target.getTile();

                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        if (x == 0 && y == 0) continue;

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
                distance += Math.random() * 0.001;
                walkableTiles.add(new TileDistance(tile, distance));
            }
        }

        if (!walkableTiles.isEmpty()) {
            Collections.sort(walkableTiles, Comparator.comparingDouble(TileDistance::getDistance));
            return walkableTiles.get(0).getTile();
        }

        return Walking.getClosestTileOnMap(area.getCenter());
    }

    public boolean walkToBank() {
        try {
            if (Bank.isOpen()) {
                System.out.println("[LocationManager] Already at bank");
                consecutiveBankFailures = 0;
                return true;
            }

            Area bank = getNearestBankArea();
            Player localPlayer = Players.getLocal();

            if (bank != null && bank.contains(localPlayer)) {
                System.out.println("[LocationManager] In bank area");
                consecutiveBankFailures = 0;
                return true;
            }

            if (!Walking.isRunEnabled() && Walking.getRunEnergy() > Walking.getRunThreshold()) {
                Walking.toggleRun();
            }

            // Try using teleport if far from bank
            if (shouldUseTeleport(bank)) {
                if (useTeleport(bank)) {
                    System.out.println("[LocationManager] Used teleport to get closer to bank");
                    return true;
                }
            }

            // Walk to nearest bank booth
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
                Tile walkableTile = null;

                for (Tile tile : bank.getTiles()) {
                    if (Walking.canWalk(tile)) {
                        walkableTile = tile;
                        break;
                    }
                }

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

    public boolean isThievingTargetAvailable() {
        String targetName = config.getCurrentTargetName();
        Player localPlayer = Players.getLocal();

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

    public boolean isAtBank() {
        if (Bank.isOpen()) {
            return true;
        }

        Player localPlayer = Players.getLocal();
        for (Area bankArea : bankAreas.values()) {
            if (bankArea.contains(localPlayer)) {
                return true;
            }
        }

        return false;
    }

    public boolean getTeleportItemsFromBank() {
        try {
            // First check for Ardougne-specific items if target is in Ardougne
            String targetName = config.getCurrentTargetName();
            boolean isArdougneTarget = targetName.equals("Knight of Ardougne") ||
                    targetName.equals("Paladin") ||
                    targetName.equals("Hero") ||
                    targetName.equals("Cake Stall");

            if (isArdougneTarget) {
                // Check if we already have Ardougne teleport
                if (hasTeleportItem("Ardougne teleport") || hasTeleportItem("Ardougne cloak")) {
                    System.out.println("[LocationManager] Already have Ardougne teleport item");
                    return true;
                }
            }

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

            // If target is in Ardougne, check for Ardougne-specific teleports first
            if (isArdougneTarget) {
                // Try Ardougne teleport tablets first
                Item ardyTeleport = Bank.get(item ->
                        item != null && item.getName().toLowerCase().contains("ardougne teleport"));

                if (ardyTeleport != null) {
                    System.out.println("[LocationManager] Found Ardougne teleport in bank");
                    if (Bank.withdraw(ardyTeleport.getName(), 5)) {
                        sleep(600, 900);
                        return true;
                    }
                }

                // Try Ardougne cloak next
                Item ardyCloak = Bank.get(item ->
                        item != null && item.getName().toLowerCase().contains("ardougne cloak"));

                if (ardyCloak != null) {
                    System.out.println("[LocationManager] Found Ardougne cloak in bank");
                    if (Bank.withdraw(ardyCloak.getName(), 1)) {
                        sleep(600, 900);
                        return true;
                    }
                }
            }

            // Check for each teleport item in bank
            for (String teleportItem : TELEPORT_ITEMS.keySet()) {
                // Look for items containing the name (with any charges)
                Item bankItem = Bank.get(item ->
                        item != null &&
                                item.getName().toLowerCase().contains(teleportItem.toLowerCase()));

                if (bankItem != null) {
                    System.out.println("[LocationManager] Found teleport item in bank: " + bankItem.getName());
                    if (Bank.withdraw(bankItem.getName(), teleportItem.contains("teleport") ? 5 : 1)) {
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

        // Tea Stall
        areas.put("Tea Stall", new Area(
                new Tile(3266, 3408, 0),
                new Tile(3270, 3412, 0)
        )); // Varrock

        // Cake Stall
        areas.put("Cake Stall", new Area(
                new Tile(2643, 3295, 0),
                new Tile(2647, 3299, 0)
        )); // Ardougne market

        return areas;
    }

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

    public boolean shouldUseTeleport(Area destination) {
        if (destination == null) {
            return false;
        }

        if (System.currentTimeMillis() - lastTeleportAttempt < TELEPORT_COOLDOWN) {
            return false;
        }

        Player localPlayer = Players.getLocal();
        String currentRegion = determineRegion(new Area(localPlayer.getTile(), localPlayer.getTile()));
        String destinationRegion = determineRegion(destination);

        if (currentRegion.equals(destinationRegion) && !currentRegion.equals("Unknown")) {
            return false;
        }

        double distance = localPlayer.distance(destination.getCenter());

        if (destinationRegion.equals("Ardougne") && (hasTeleportItem("Ardougne teleport") || hasTeleportItem("Ardougne cloak"))) {
            return true;
        }

        if (distance > 50) {
            for (String teleportItem : TELEPORT_ITEMS.keySet()) {
                if (hasTeleportItem(teleportItem)) {
                    return true;
                }
            }
        }

        return false;
    }
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
        String teleportItemName = TELEPORT_ITEMS.entrySet().stream()
                .filter(entry -> entry.getValue().contains(bestOption))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse(null);

        if (teleportItemName == null) {
            System.out.println("[LocationManager] Could not find teleport item for option");
            return false;
        }

        // Try equipped item first
        Item equipped = null;
        if (teleportItemName.contains("amulet") || teleportItemName.contains("glory")) {
            equipped = Equipment.getItemInSlot(EquipmentSlot.AMULET.getSlot());
        } else if (teleportItemName.contains("bracelet")) {
            equipped = Equipment.getItemInSlot(EquipmentSlot.HANDS.getSlot());
        } else if (teleportItemName.contains("ring")) {
            equipped = Equipment.getItemInSlot(EquipmentSlot.RING.getSlot());
        } else if (teleportItemName.contains("necklace")) {
            equipped = Equipment.getItemInSlot(EquipmentSlot.AMULET.getSlot());
        } else if (teleportItemName.contains("cloak")) {
            equipped = Equipment.getItemInSlot(EquipmentSlot.CAPE.getSlot());
        }

        if (equipped != null && equipped.getName().toLowerCase().contains(teleportItemName.toLowerCase())) {
            if (equipped.interact("Rub") || equipped.interact("Teleport") || equipped.interact("Operate")) {
                System.out.println("[LocationManager] Using equipped " + equipped.getName());
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
            if (invItem.interact("Rub") || invItem.interact("Teleport") || invItem.interact("Break")) {
                sleep(1000, 2000);

                // For tablets that don't have dialogue options
                if (invItem.getName().toLowerCase().contains("teleport")) {
                    sleep(3000, 4000); // Wait for teleport
                    return true;
                }

                if (selectDialogueOption(bestOption.getDialogueOption())) {
                    sleep(3000, 4000); // Wait for teleport
                    return true;
                }
            }
        }

        System.out.println("[LocationManager] Failed to use teleport");
        return false;
    }
    private Item getArdougneCloak() {
        // Check equipped cloak first
        Item equippedCloak = Equipment.getItemInSlot(EquipmentSlot.CAPE.getSlot());
        if (equippedCloak != null && equippedCloak.getName().toLowerCase().contains("ardougne cloak")) {
            return equippedCloak;
        }

        // Check inventory
        return Inventory.get(item ->
                item != null && item.getName().toLowerCase().contains("ardougne cloak"));
    }

    private TeleportOption findBestTeleportOption(Area destination) {
        Tile destCenter = destination.getCenter();
        TeleportOption bestOption = null;
        double bestDistance = Double.MAX_VALUE;

        for (List<TeleportOption> options : TELEPORT_ITEMS.values()) {
            for (TeleportOption option : options) {
                double distance = option.getLocation().distance(destCenter);
                distance += Math.random() * 0.001;

                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestOption = option;
                }
            }
        }

        return bestOption;
    }

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
        } else if (center.getX() >= 2560 && center.getX() <= 2680 &&  // Expanded Ardougne region
                center.getY() >= 3270 && center.getY() <= 3350) {
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

    private void sleep(int min, int max) {
        try {
            Thread.sleep(min + (int) (Math.random() * (max - min)));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

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