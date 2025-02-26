package org;

import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.equipment.Equipment;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.item.GroundItems;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.items.GroundItem;
import org.dreambot.api.wrappers.items.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the Rogues' Den minigame for obtaining the Rogues' outfit
 */
public class RoguesDenManager {

    // Constants for item IDs
    private static final int MYSTIC_JEWEL_ID = 5561;
    private static final int FLASH_POWDER_ID = 5560;
    private static final int WALL_SAFE_ID = 7236;
    private static final int ROGUES_DEN_ENTRANCE_ID = 7256;
    private static final int ROGUES_GUARD_ID = 3191;
    private static final int TILE_ID = 5568;
    private static final int TILE_DOOR_ID = 7234;

    // Constants for outfit pieces
    private static final int ROGUES_GLOVES_ID = 5556;
    private static final int ROGUES_BOOTS_ID = 5557;
    private static final int ROGUES_MASK_ID = 5554;
    private static final int ROGUES_TOP_ID = 5553;
    private static final int ROGUES_TROUSERS_ID = 5555;

    // Rogues' Den entrance area
    private static final Area ROGUES_DEN_ENTRANCE_AREA = new Area(3054, 4989, 3058, 4993, 1);

    // Tracking variables
    private int mazeRunsCompleted = 0;
    private int currentObstacleIndex = 0;
    private boolean hasStaminaPotionInBank = true;
    private RoguesDenState currentState = RoguesDenState.PREPARATION;
    private final ScriptConfiguration config;
    private final List<OutfitPiece> obtainedOutfitPieces = new ArrayList<>();

    // Obstacles for the maze
    private RoguesDenObstacle[] obstacles;

    /**
     * Represents the current state of the Rogues' Den script
     */
    public enum RoguesDenState {
        PREPARATION,
        BANKING,
        ENTERING_MAZE,
        NAVIGATING_MAZE,
        CRACKING_SAFE,
        EXITING_MAZE
    }

    /**
     * Represents an outfit piece from the Rogues' Den
     */
    public static class OutfitPiece {
        private final String name;
        private final int itemId;
        private boolean obtained;

        public OutfitPiece(String name, int itemId) {
            this.name = name;
            this.itemId = itemId;
            this.obtained = false;
        }

        public String getName() {
            return name;
        }

        public int getItemId() {
            return itemId;
        }

        public boolean isObtained() {
            return obtained;
        }

        public void setObtained(boolean obtained) {
            this.obtained = obtained;
        }
    }

    /**
     * Creates a new Rogues' Den manager
     *
     * @param config The script configuration
     */
    public RoguesDenManager(ScriptConfiguration config) {
        this.config = config;
        this.initializeOutfitPieces();
        this.initializeObstacles();
    }

    /**
     * Initializes the outfit pieces tracking
     */
    private void initializeOutfitPieces() {
        obtainedOutfitPieces.add(new OutfitPiece("Rogues' gloves", ROGUES_GLOVES_ID));
        obtainedOutfitPieces.add(new OutfitPiece("Rogues' boots", ROGUES_BOOTS_ID));
        obtainedOutfitPieces.add(new OutfitPiece("Rogues' mask", ROGUES_MASK_ID));
        obtainedOutfitPieces.add(new OutfitPiece("Rogues' top", ROGUES_TOP_ID));
        obtainedOutfitPieces.add(new OutfitPiece("Rogues' trousers", ROGUES_TROUSERS_ID));

        // Check if pieces are already owned and mark them as obtained
        updateObtainedPieces();
    }

    /**
     * Updates the list of obtained outfit pieces
     */
    private void updateObtainedPieces() {
        for (OutfitPiece piece : obtainedOutfitPieces) {
            if (Equipment.contains(piece.getItemId()) ||
                    Inventory.contains(piece.getItemId()) ||
                    Bank.contains(piece.getItemId())) {
                piece.setObtained(true);
            }
        }
    }

    /**
     * Initializes the obstacles for the maze
     */
    private void initializeObstacles() {
        List<RoguesDenObstacle> obstacleList = new ArrayList<>();

        // Add all the obstacles based on the path from the provided code
        obstacleList.add(new RoguesDenObstacle(3048, 4997, 7251, ""));
        obstacleList.add(new RoguesDenObstacle(3039, 4999, -1, "Stand"));
        obstacleList.add(new RoguesDenObstacle(3029, 5003, -1, "Run"));
        obstacleList.add(new RoguesDenObstacle(3023, 5001, 7255, "Open"));
        obstacleList.add(new RoguesDenObstacle(3011, 5005, -1, "Run"));
        obstacleList.add(new RoguesDenObstacle(3004, 5003, -1, "Run"));
        obstacleList.add(new RoguesDenObstacle(2988, 5004, 7240, ""));
        obstacleList.add(new RoguesDenObstacle(2969, 5016, -1, "Stand"));
        obstacleList.add(new RoguesDenObstacle(2967, 5016, -1, "Stand"));
        obstacleList.add(new RoguesDenObstacle(2958, 5031, 7239, ""));

        // Filter obstacles based on thieving level
        int thievingLevel = Skills.getRealLevel(Skill.THIEVING);

        // Add high level path if thieving level is 80+
        if (thievingLevel >= 80) {
            // Add the 80+ thieving path obstacles
            obstacleList.add(new RoguesDenObstacle(2958, 5035, -1, "Stand"));
            obstacleList.add(new RoguesDenObstacle(2962, 5050, -1, "Stand"));
            obstacleList.add(new RoguesDenObstacle(2963, 5056, -1, "Run"));
            obstacleList.add(new RoguesDenObstacle(2968, 5061, 7246, "Stand"));
            obstacleList.add(new RoguesDenObstacle(2974, 5059, 7251, "Stand"));
            obstacleList.add(new RoguesDenObstacle(2989, 5058, -1, "Stand"));
            obstacleList.add(new RoguesDenObstacle(2990, 5058, 7255, "Open"));
        } else {
            // Add the lower level path obstacles
            obstacleList.add(new RoguesDenObstacle(2957, 5072, 7219, "Open"));
            obstacleList.add(new RoguesDenObstacle(2957, 5076, -1, "Run"));
            obstacleList.add(new RoguesDenObstacle(2955, 5092, -1, "Stand"));
            obstacleList.add(new RoguesDenObstacle(2955, 5098, 7219, "Open"));
            obstacleList.add(new RoguesDenObstacle(2963, 5105, -1, "Stand"));
            obstacleList.add(new RoguesDenObstacle(2972, 5098, -1, "Stand"));
            obstacleList.add(new RoguesDenObstacle(2972, 5094, 7219, "Open"));
            obstacleList.add(new RoguesDenObstacle(2972, 5093, 7255, "Open"));
            obstacleList.add(new RoguesDenObstacle(2976, 5087, -1, "Stand"));
            obstacleList.add(new RoguesDenObstacle(2982, 5087, 7240, "Climb"));
            obstacleList.add(new RoguesDenObstacle(2992, 5088, -1, "Stand"));
            obstacleList.add(new RoguesDenObstacle(2992, 5088, 7249, "Search"));
        }

        // Add the final path that's common to both routes
        obstacleList.add(new RoguesDenObstacle(3009, 5063, -1, "Take"));
        obstacleList.add(new RoguesDenObstacle(3028, 5056, -1, "Run"));
        obstacleList.add(new RoguesDenObstacle(3028, 5047, -1, "Walk"));
        obstacleList.add(new RoguesDenObstacle(3018, 5047, 7237, "Crack"));

        obstacles = obstacleList.toArray(new RoguesDenObstacle[0]);
    }

    /**
     * Main method to handle Rogues' Den operations
     *
     * @return True if action was taken, false otherwise
     */
    public boolean handleRoguesDen() {
        if (!isSkillRequirementsMet()) {
            System.out.println("Skill requirements not met for Rogues' Den. Requires 50+ Thieving and Agility.");
            return false;
        }

        // Check if we have all the outfit pieces
        if (isFullOutfitObtained() && config.isStopAfterFullOutfit()) {
            System.out.println("Full Rogues' outfit obtained. Script complete.");
            return false;
        }

        // Handle current state
        switch (currentState) {
            case PREPARATION:
                return handlePreparation();
            case BANKING:
                return handleBanking();
            case ENTERING_MAZE:
                return handleEnteringMaze();
            case NAVIGATING_MAZE:
                return handleNavigatingMaze();
            case CRACKING_SAFE:
                return handleCrackingSafe();
            case EXITING_MAZE:
                return handleExitingMaze();
            default:
                return false;
        }
    }

    /**
     * Checks if the player has the required skills for Rogues' Den
     *
     * @return True if requirements are met
     */
    private boolean isSkillRequirementsMet() {
        int thievingLevel = Skills.getRealLevel(Skill.THIEVING);
        int agilityLevel = Skills.getRealLevel(Skill.AGILITY);

        return thievingLevel >= 50 && agilityLevel >= 50;
    }

    /**
     * Checks if the full Rogues' outfit has been obtained
     *
     * @return True if all pieces are obtained
     */
    private boolean isFullOutfitObtained() {
        for (OutfitPiece piece : obtainedOutfitPieces) {
            if (!piece.isObtained()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Handles the preparation state - ensuring no equipment is worn
     *
     * @return True if action was taken
     */
    private boolean handlePreparation() {
        // Check if we need to bank to deposit equipment
        if (!Equipment.isEmpty() || !Inventory.isEmpty()) {
            currentState = RoguesDenState.BANKING;
            return true;
        }

        // Ready to enter the maze
        currentState = RoguesDenState.ENTERING_MAZE;
        return true;
    }

    /**
     * Handles the banking state - deposits all items and withdraws stamina potion if needed
     *
     * @return True if action was taken
     */
    private boolean handleBanking() {
        if (Bank.isOpen() || Bank.open()) {
            // Deposit all equipment
            if (!Equipment.isEmpty()) {
                Bank.depositAllEquipment();
                sleep(600, 900);
                return true;
            }

            // Deposit all inventory items
            if (!Inventory.isEmpty()) {
                Bank.depositAllItems();
                sleep(600, 900);
                return true;
            }

            // Check if we should withdraw stamina potion
            if (config.isUseStaminaPotions() && !Walking.isStaminaActive() && hasStaminaPotionInBank) {
                if (Bank.contains("Stamina potion")) {
                    Bank.withdraw("Stamina potion", 1);
                    sleep(600, 900);

                    if (Inventory.contains("Stamina potion")) {
                        Bank.close();
                        Inventory.interact("Stamina potion", "Drink");
                        sleep(600, 900);
                        return true;
                    }
                } else {
                    hasStaminaPotionInBank = false;
                    System.out.println("No stamina potion found in bank. Continuing without it.");
                }
            }

            Bank.close();
            sleep(600, 900);
            currentState = RoguesDenState.ENTERING_MAZE;
            return true;
        }

        return true;
    }

    /**
     * Handles entering the Rogues' Den maze
     *
     * @return True if action was taken
     */
    private boolean handleEnteringMaze() {
        // Check if already in the maze
        if (Inventory.contains(MYSTIC_JEWEL_ID)) {
            currentState = RoguesDenState.NAVIGATING_MAZE;
            currentObstacleIndex = 0;
            return true;
        }

        // Walk to the entrance if not already there
        if (!ROGUES_DEN_ENTRANCE_AREA.contains(Players.getLocal())) {
            Walking.walk(ROGUES_DEN_ENTRANCE_AREA.getCenter());
            sleep(1000, 1500);
            return true;
        }

        // Enter the maze
        GameObject entrance = GameObjects.closest(ROGUES_DEN_ENTRANCE_ID);
        if (entrance != null && entrance.interact("Enter")) {
            sleep(2000, 3000);
            return true;
        }

        return true;
    }

    /**
     * Handles navigating the Rogues' Den maze
     *
     * @return True if action was taken
     */
    private boolean handleNavigatingMaze() {
        // Check if we're animating or moving
        if (Players.getLocal().isAnimating() || Players.getLocal().isMoving()) {
            return true;
        }

        // Handle flash powder for stunning the guard
        if (Inventory.contains(FLASH_POWDER_ID) && Players.getLocal().getX() < 3026) {
            NPC guard = NPCs.closest(ROGUES_GUARD_ID);
            if (guard != null) {
                System.out.println("Stunning guard with flash powder");
                Item flashPowder = Inventory.get(FLASH_POWDER_ID);
                if (flashPowder != null && flashPowder.useOn(guard)) {
                    sleep(1000, 1500);
                    return true;
                }
            }
        }

        // Handle tile for the tile door
        if (Inventory.contains(TILE_ID)) {
            GameObject tileDoor = GameObjects.closest(TILE_DOOR_ID);
            if (tileDoor != null && tileDoor.interact("Open")) {
                sleep(1000, 1500);
                if (Dialogues.canContinue()) {
                    Dialogues.continueDialogue();
                    sleep(1000, 1500);
                    return true;
                }
            }
        }

        // Find the nearest obstacle
        int closestIndex = findClosestObstacleIndex();
        if (closestIndex == -1 || closestIndex >= obstacles.length - 1) {
            // If we're at the last obstacle, move to cracking the safe
            currentState = RoguesDenState.CRACKING_SAFE;
            return true;
        }

        currentObstacleIndex = closestIndex;

        // Handle the current obstacle
        RoguesDenObstacle currentObstacle = obstacles[currentObstacleIndex];
        RoguesDenObstacle nextObstacle = obstacles[currentObstacleIndex + 1];

        // Special handling for the first obstacle
        if (currentObstacleIndex == 0 && !Players.getLocal().getTile().equals(currentObstacle.getTile())) {
            GameObject firstObstacle = GameObjects.closest(obj ->
                    obj.getID() == currentObstacle.getObjectId() &&
                            obj.getTile().equals(currentObstacle.getTile()));

            if (firstObstacle != null && firstObstacle.interact()) {
                sleep(1000, 1500);
                return true;
            }
        }

        // If at the current obstacle, interact with the next one
        if (Players.getLocal().getTile().equals(currentObstacle.getTile())) {
            return handleObstacle(nextObstacle);
        } else {
            // Special check for flash powder
            if (currentObstacle.getTile().getX() == 3028 && currentObstacle.getTile().getY() == 5056) {
                if (!Inventory.contains(FLASH_POWDER_ID)) {
                    return handleObstacle(currentObstacle);
                }
            }

            // Not at the obstacle, so interact with the current one
            return handleObstacle(currentObstacle);
        }
    }

    /**
     * Finds the closest obstacle to the player
     *
     * @return The index of the closest obstacle
     */
    private int findClosestObstacleIndex() {
        Player local = Players.getLocal();
        int closestIndex = -1;
        double closestDistance = Double.MAX_VALUE;

        for (int i = currentObstacleIndex; i < obstacles.length; i++) {
            RoguesDenObstacle obstacle = obstacles[i];
            double distance = local.distance(obstacle.getTile());

            if (distance < closestDistance) {
                closestDistance = distance;
                closestIndex = i;
            }
        }

        return closestIndex;
    }

    /**
     * Handles interacting with a specific obstacle
     *
     * @param obstacle The obstacle to interact with
     * @return True if action was taken
     */
    private boolean handleObstacle(RoguesDenObstacle obstacle) {
        // If it's the wall safe, we're finishing the maze
        if (obstacle.getHint().equalsIgnoreCase("Crack")) {
            currentState = RoguesDenState.CRACKING_SAFE;
            return true;
        }

        // Check run energy for run actions
        if ((obstacle.getHint().equalsIgnoreCase("run") || obstacle.getHint().equalsIgnoreCase("take"))
                && Walking.getRunEnergy() < 10) {
            System.out.println("Low run energy. Waiting to recharge...");
            sleep(5000, 7000);
            return true;
        }

        // Handle obstacle based on type
        if (obstacle.getObjectId() != -1) {
            GameObject gameObject = GameObjects.closest(obj ->
                    obj.getID() == obstacle.getObjectId() &&
                            obj.distance() < 15);

            if (gameObject != null) {
                if (obstacle.getHint().equalsIgnoreCase("search")) {
                    return gameObject.interact("Search");
                } else {
                    return gameObject.interact();
                }
            }
        } else if (obstacle.getHint().equalsIgnoreCase("take") && !Inventory.contains(FLASH_POWDER_ID)) {
            GroundItem flashPowder = GroundItems.closest(FLASH_POWDER_ID);
            if (flashPowder != null && flashPowder.interact("Take")) {
                sleep(1000, 1500);
                return true;
            }
        } else if (obstacle.getHint().equalsIgnoreCase("tile") && !Inventory.contains(TILE_ID)) {
            GroundItem tile = GroundItems.closest(TILE_ID);
            if (tile != null && tile.interact("Take")) {
                sleep(1000, 1500);
                return true;
            }
        } else {
            // Just walk to the tile
            if (!Walking.walkExact(obstacle.getTile())) {
                Walking.walk(obstacle.getTile());
            }
            sleep(obstacle.getWaitTime(), obstacle.getWaitTime() + 300);
            return true;
        }

        return false;
    }

    /**
     * Handles cracking the wall safe
     *
     * @return True if action was taken
     */
    private boolean handleCrackingSafe() {
        GameObject wallSafe = GameObjects.closest(WALL_SAFE_ID);
        if (wallSafe != null && wallSafe.interact("Crack")) {
            sleep(3000, 4000);
            mazeRunsCompleted++;
            currentState = RoguesDenState.EXITING_MAZE;
            return true;
        }

        return true;
    }

    /**
     * Handles exiting the maze after completing a run
     *
     * @return True if action was taken
     */
    private boolean handleExitingMaze() {
        // Check for any new outfit pieces and update
        updateObtainedPieces();

        // Check if we need to continue or are done
        if (isFullOutfitObtained() && config.isStopAfterFullOutfit()) {
            System.out.println("Full Rogues' outfit obtained. Script complete.");
            return false;
        }

        // Check if we're still in the maze (have the mystic jewel)
        if (Inventory.contains(MYSTIC_JEWEL_ID)) {
            // Need to exit - drop the mystic jewel
            Item jewel = Inventory.get(MYSTIC_JEWEL_ID);
            if (jewel != null && jewel.interact("Drop")) {
                sleep(1000, 1500);
                return true;
            }
        } else {
            // We're out of the maze, reset to preparation
            currentState = RoguesDenState.PREPARATION;
            currentObstacleIndex = 0;
            return true;
        }

        return true;
    }

    /**
     * Gets the number of maze runs completed
     *
     * @return The number of maze runs completed
     */
    public int getMazeRunsCompleted() {
        return mazeRunsCompleted;
    }

    /**
     * Gets the list of obtained outfit pieces
     *
     * @return The list of outfit pieces
     */
    public List<OutfitPiece> getObtainedOutfitPieces() {
        return obtainedOutfitPieces;
    }

    /**
     * Utility method for random sleep
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
}