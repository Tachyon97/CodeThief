package org;

import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.equipment.Equipment;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.wrappers.items.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enhanced inventory manager for the thieving script.
 * Handles food management, teleport items, and loot organization.
 */
public class InventoryManager {
    private final ScriptConfiguration config;

    // Teleport items that can be used for navigation
    private static final String[] TELEPORT_ITEMS = {
            "Amulet of glory", "Games necklace", "Ring of dueling",
            "Skills necklace", "Ring of wealth", "Combat bracelet"
    };

    // Common food keywords for auto-detection
    private static final String[] FOOD_KEYWORDS = {
            "fish", "cake", "bread", "pizza", "pie", "stew", "wine",
            "potion", "brew", "food", "shark", "lobster", "salmon", "tuna",
            "monkfish", "karambwan", "fruit", "potato", "curry", "soup"
    };

    // Valuable loot keywords
    private static final String[] VALUABLE_KEYWORDS = {
            "coin", "token", "nugget", "gem", "sapphire", "emerald", "ruby",
            "diamond", "gold", "silver", "torstol", "ranarr", "snapdragon",
            "necklace", "bracelet", "ring", "amulet", "clue scroll"
    };

    // Tracking stats
    private int foodEaten = 0;
    private int itemsDropped = 0;
    private int itemsBanked = 0;
    private long lastFoodCheck = 0;
    private static final long FOOD_CHECK_COOLDOWN = 3000; // 3 second cooldown on food checks

    /**
     * Creates a new inventory manager
     *
     * @param config The script configuration
     */
    public InventoryManager(ScriptConfiguration config) {
        this.config = config;
    }

    /**
     * Checks if the inventory is full
     *
     * @return True if inventory is full
     */
    public boolean isInventoryFull() {
        return Inventory.isFull();
    }

    /**
     * Gets the number of free inventory slots
     *
     * @return Number of empty slots
     */
    public int getFreeSlots() {
        return Inventory.emptySlotCount();
    }

    /**
     * Gets the item count in inventory
     *
     * @return Number of items in inventory
     */
    public int getItemCount() {
        return Inventory.fullSlotCount();
    }

    /**
     * Checks if we have space for at least n more items
     *
     * @param n Number of items to check space for
     * @return True if there's enough space
     */
    public boolean hasSpaceFor(int n) {
        return Inventory.emptySlotCount() >= n;
    }

    /**
     * Handles a full inventory based on configuration
     *
     * @return True if handled successfully, false if needs to bank
     */
    public boolean handleFullInventory() {
        try {
            if (config.isFreestyleMode()) {
                return dropJunkItems();
            } else if (config.isBankingEnabled()) {
                return false; // Signal to transition to banking state
            } else {
                return dropAllExceptValuable();
            }
        } catch (Exception e) {
            log("Error handling full inventory: " + e.getMessage());
            e.printStackTrace();
            // If there's an error, try to drop some items as a fallback
            return emergencyDropItems();
        }
    }

    /**
     * Emergency method to drop some items when other methods fail
     *
     * @return True if some items were dropped
     */
    private boolean emergencyDropItems() {
        try {
            log("EMERGENCY: Attempting to drop some items to free space");
            int initialCount = Inventory.fullSlotCount();

            // Try to drop non-valuable items first
            for (Item item : Inventory.all()) {
                if (item != null && !isValuableItem(item) && !isEdibleItem(item)) {
                    item.interact("Drop");
                    sleep(100, 200);
                    itemsDropped++;

                    // Stop after dropping a few items
                    if (initialCount - Inventory.fullSlotCount() >= 3) {
                        break;
                    }
                }
            }

            return initialCount > Inventory.fullSlotCount();
        } catch (Exception e) {
            log("Even emergency drop failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Handles banking operations
     *
     * @return True if banking is complete
     */
    public boolean handleBanking() {
        try {
            // Open bank if not open
            if (!Bank.isOpen()) {
                log("Opening bank...");
                if (!Bank.open()) {
                    log("Failed to open bank");
                    return false;
                }
                sleep(600, 900); // Wait for bank to open
            }

            // First deposit everything - we don't need tools for thieving
            log("Depositing all items...");
            int itemsBeforeDeposit = Inventory.fullSlotCount();

            if (Bank.depositAllItems()) {
                sleep(600, 900);
                itemsBanked += (itemsBeforeDeposit - Inventory.fullSlotCount());
                log("Deposited " + (itemsBeforeDeposit - Inventory.fullSlotCount()) + " items");
            } else {
                log("Failed to deposit items");
            }

            // Check for teleport items first
            if (!hasTeleportItem()) {
                log("Looking for teleport items...");
                withdrawTeleportItem();
                sleep(300, 600);
            }

            // Now withdraw food based on priorities
            if (!withdrawFoodBasedOnPriority()) {
                log("Failed to withdraw food");
                // Continue anyway, as teleport items are more important for navigation
            }

            // Close the bank
            log("Closing bank...");
            Bank.close();
            sleep(300, 600);

            // Log what we have
            if (hasTeleportItem() && hasFood()) {
                log("Successfully got food and teleport items from bank");
            } else if (hasTeleportItem()) {
                log("Got teleport items but no food");
            } else if (hasFood()) {
                log("Got food but no teleport items");
            } else {
                log("WARNING: Failed to get food or teleport items");
            }

            return true;
        } catch (Exception e) {
            log("Banking error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Withdraw food based on priority set in GUI
     *
     * @return True if any food was withdrawn
     */
    private boolean withdrawFoodBasedOnPriority() {
        List<String> foodPriorities = config.getFoodItems();

        if (foodPriorities.isEmpty()) {
            log("No food preferences configured");
            return withdrawAnyFood();
        }

        log("Food priorities: " + foodPriorities);

        // Try primary food (first in the list) first
        String primaryFood = foodPriorities.get(0);
        log("Trying primary food: " + primaryFood);

        if (tryWithdrawFood(primaryFood, 10)) {
            log("Withdrew primary food: " + primaryFood);
            return true;
        }

        // Try other selected foods
        log("Primary food not found, trying alternatives...");
        for (int i = 1; i < foodPriorities.size(); i++) {
            String altFood = foodPriorities.get(i);
            if (tryWithdrawFood(altFood, 10)) {
                log("Withdrew alternative food: " + altFood);
                return true;
            }
        }

        // If no configured food found, try any food
        log("No configured food found, trying any food...");
        return withdrawAnyFood();
    }

    /**
     * Try to withdraw a specific food
     *
     * @param foodName The food name to search for
     * @param amount   Amount to withdraw
     * @return True if successfully withdrawn
     */
    private boolean tryWithdrawFood(String foodName, int amount) {
        // Try exact match first
        if (Bank.contains(item ->
                item != null &&
                        item.getName().equalsIgnoreCase(foodName))) {
            return Bank.withdraw(foodName, amount);
        }

        // Try partial match
        Item bankItem = Bank.get(item ->
                item != null &&
                        item.getName().toLowerCase().contains(foodName.toLowerCase()));

        if (bankItem != null) {
            return Bank.withdraw(bankItem.getName(), amount);
        }

        return false;
    }

    /**
     * Try to withdraw any food available in bank
     *
     * @return True if food was withdrawn
     */
    private boolean withdrawAnyFood() {
        // Try common foods first
        for (String keyword : FOOD_KEYWORDS) {
            Item foodItem = Bank.get(item ->
                    item != null &&
                            item.getName().toLowerCase().contains(keyword.toLowerCase()));

            if (foodItem != null) {
                log("Found food item: " + foodItem.getName());
                if (Bank.withdraw(foodItem.getName(), 10)) {
                    log("Withdrew food: " + foodItem.getName());
                    sleep(300, 500);
                    return true;
                }
            }
        }

        log("No food found in bank at all");
        return false;
    }

    /**
     * Drops junk items based on thieving method
     *
     * @return True if items were dropped
     */
    public boolean dropJunkItems() {
        String currentTarget = config.getCurrentTargetName();

        // Different handling for different targets
        if (currentTarget.equals("Master Farmer")) {
            return dropJunkSeeds();
        } else if (currentTarget.toLowerCase().contains("stall")) {
            return dropStallJunk();
        } else {
            return dropAllExceptValuable();
        }
    }

    /**
     * Drops low-value seeds from Master Farmer pickpocketing
     *
     * @return True if seeds were dropped
     */
    private boolean dropJunkSeeds() {
        // List of junk seed names that should be dropped
        String[] junkSeeds = {
                "potato seed", "onion seed", "cabbage seed", "tomato seed",
                "sweetcorn seed", "strawberry seed", "gardening seed", "barley seed",
                "hammerstone seed", "asgarnian seed", "jute seed", "yanillian seed",
                "krandorian seed", "wildblood seed"
        };

        int itemsDroppedCount = 0;

        // Drop all junk seeds
        for (String seedName : junkSeeds) {
            List<Item> seeds = Inventory.all(item ->
                    item != null && item.getName().toLowerCase().contains(seedName.toLowerCase()));

            for (Item seed : seeds) {
                if (seed != null && seed.interact("Drop")) {
                    sleep(100, 200);
                    itemsDroppedCount++;
                }
            }
        }

        if (itemsDroppedCount > 0) {
            itemsDropped += itemsDroppedCount;
            log("Dropped " + itemsDroppedCount + " junk seeds");
        }

        return itemsDroppedCount > 0;
    }

    /**
     * Drops junk items from stall thieving
     *
     * @return True if items were dropped
     */
    private boolean dropStallJunk() {
        // Items to keep depend on the stall type
        String stallType = config.getCurrentTargetName();
        List<String> valuableItems = new ArrayList<>();

        if (stallType.equals("Gem Stall")) {
            valuableItems.add("sapphire");
            valuableItems.add("emerald");
            valuableItems.add("ruby");
            valuableItems.add("diamond");
        } else if (stallType.equals("Silk Stall")) {
            valuableItems.add("silk");
        } else if (stallType.equals("Silver Stall")) {
            valuableItems.add("silver");
        }

        // Always keep coins and food
        valuableItems.add("coin");
        config.getFoodItems().forEach(food -> valuableItems.add(food.toLowerCase()));

        // Add teleport items
        for (String teleport : TELEPORT_ITEMS) {
            valuableItems.add(teleport.toLowerCase());
        }

        // Drop items that aren't valuable
        return dropAllExcept(valuableItems);
    }

    /**
     * Drops all items except valuable ones
     *
     * @return True if items were dropped
     */
    private boolean dropAllExceptValuable() {
        List<String> valuableItems = new ArrayList<>();

        // Basic valuable items
        for (String keyword : VALUABLE_KEYWORDS) {
            valuableItems.add(keyword.toLowerCase());
        }

        // Food items
        config.getFoodItems().forEach(food -> valuableItems.add(food.toLowerCase()));

        // Teleport items
        for (String teleport : TELEPORT_ITEMS) {
            valuableItems.add(teleport.toLowerCase());
        }

        // Drop items that aren't valuable
        return dropAllExcept(valuableItems);
    }

    /**
     * Drops all items except those containing specified strings
     *
     * @param itemsToKeep List of item name substrings to keep
     * @return True if items were dropped
     */
    private boolean dropAllExcept(List<String> itemsToKeep) {
        // Copy the inventory first to avoid modification during iteration
        List<Item> itemsToDrop = Inventory.all().stream()
                .filter(item -> item != null)
                .filter(item -> {
                    String itemName = item.getName().toLowerCase();
                    // Check if this item contains any of the keywords we want to keep
                    for (String keepName : itemsToKeep) {
                        if (keepName == null) continue;
                        if (itemName.contains(keepName.toLowerCase())) {
                            return false; // Don't drop this item
                        }
                    }
                    return true; // Drop this item
                })
                .collect(Collectors.toList());

        int itemsDroppedCount = 0;

        // Drop items in the filtered list
        for (Item item : itemsToDrop) {
            if (item != null && item.interact("Drop")) {
                sleep(100, 200);
                itemsDroppedCount++;
            }
        }

        if (itemsDroppedCount > 0) {
            itemsDropped += itemsDroppedCount;
            log("Dropped " + itemsDroppedCount + " items");
        }

        return itemsDroppedCount > 0;
    }

    /**
     * Checks if the player needs to heal
     *
     * @return True if health is below threshold
     */
    public boolean needToHeal() {
        // Add a cooldown to prevent constant checking
        if (System.currentTimeMillis() - lastFoodCheck < FOOD_CHECK_COOLDOWN) {
            return false;
        }

        lastFoodCheck = System.currentTimeMillis();

        int currentHP = Skills.getBoostedLevel(Skill.HITPOINTS);
        int maxHP = Skills.getRealLevel(Skill.HITPOINTS);
        int healthPercent = (int) (((double) currentHP / maxHP) * 100);

        return healthPercent < config.getHealthThreshold();
    }

    /**
     * Attempts to eat food from inventory
     *
     * @return True if food was eaten
     */
    public boolean eatFood() {
        // If we don't have food, don't try to eat
        if (!hasFood()) {
            return false;
        }

        // Try to eat configured foods first
        List<String> configuredFoods = config.getFoodItems();

        // First try exact matches from configuration
        for (String foodName : configuredFoods) {
            Item food = Inventory.get(item ->
                    item != null && item.getName().equalsIgnoreCase(foodName));

            if (food != null) {
                log("Eating configured food: " + food.getName());
                if (eatFoodItem(food)) {
                    foodEaten++;
                    return true;
                }
            }
        }

        // Then try partial matches
        for (String foodName : configuredFoods) {
            Item food = Inventory.get(item ->
                    item != null && item.getName().toLowerCase().contains(foodName.toLowerCase()));

            if (food != null) {
                log("Eating food (partial match): " + food.getName());
                if (eatFoodItem(food)) {
                    foodEaten++;
                    return true;
                }
            }
        }

        // Last resort - try to find anything edible
        Item possibleFood = Inventory.get(this::isEdibleItem);
        if (possibleFood != null) {
            log("Trying to eat possible food: " + possibleFood.getName());
            if (eatFoodItem(possibleFood)) {
                foodEaten++;
                return true;
            }
        }

        return false;
    }

    /**
     * Tries to eat a specific food item with better action handling
     *
     * @param food The food item to eat
     * @return True if successfully eaten
     */
    private boolean eatFoodItem(Item food) {
        if (food == null) {
            return false;
        }

        // Try "Eat" action first
        if (food.hasAction("Eat")) {
            return food.interact("Eat");
        }

        // Try "Drink" for potions
        if (food.hasAction("Drink")) {
            return food.interact("Drink");
        }

        // Try "Consume" as another possibility
        if (food.hasAction("Consume")) {
            return food.interact("Consume");
        }

        // Last resort - try default interaction
        return food.interact();
    }

    /**
     * Checks if an item name could possibly be food
     */
    private boolean couldBeFood(String name) {
        if (name == null) return false;

        name = name.toLowerCase();

        for (String keyword : FOOD_KEYWORDS) {
            if (name.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if player has food in inventory
     *
     * @return True if food is present
     */
    public boolean hasFood() {
        return Inventory.contains(this::isEdibleItem);
    }

    /**
     * Checks if an item is edible
     *
     * @param item The item to check
     * @return True if item is edible
     */
    public boolean isEdibleItem(Item item) {
        if (item == null) {
            return false;
        }

        String name = item.getName().toLowerCase();

        // Check against configured food items first
        for (String foodName : config.getFoodItems()) {
            if (foodName != null && name.contains(foodName.toLowerCase())) {
                return true;
            }
        }

        // Check for generic food items
        return couldBeFood(name);
    }

    /**
     * Checks if an item is valuable enough to keep
     *
     * @param item The item to check
     * @return True if item is valuable
     */
    public boolean isValuableItem(Item item) {
        if (item == null) return false;

        String name = item.getName().toLowerCase();

        for (String valuable : VALUABLE_KEYWORDS) {
            if (name.contains(valuable.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if player has a teleport item
     *
     * @return True if a teleport item is found
     */
    public boolean hasTeleportItem() {
        // Check inventory
        for (String teleport : TELEPORT_ITEMS) {
            if (Inventory.contains(item ->
                    item != null && item.getName().toLowerCase().contains(teleport.toLowerCase()))) {
                return true;
            }
        }

        // Check equipment
        for (String teleport : TELEPORT_ITEMS) {
            if (Equipment.contains(item ->
                    item != null && item.getName().toLowerCase().contains(teleport.toLowerCase()))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the teleport item from inventory or equipment
     *
     * @return The teleport item or null if not found
     */
    public Item getTeleportItem() {
        // Check inventory first
        for (String teleport : TELEPORT_ITEMS) {
            Item item = Inventory.get(i ->
                    i != null && i.getName().toLowerCase().contains(teleport.toLowerCase()));

            if (item != null) {
                return item;
            }
        }

        // Then check equipment
        for (String teleport : TELEPORT_ITEMS) {
            Item item = Equipment.get(i ->
                    i != null && i.getName().toLowerCase().contains(teleport.toLowerCase()));

            if (item != null) {
                return item;
            }
        }

        return null;
    }

    /**
     * Attempts to withdraw a teleport item from bank
     *
     * @return True if successful
     */
    private boolean withdrawTeleportItem() {
        for (String teleport : TELEPORT_ITEMS) {
            Item item = Bank.get(i ->
                    i != null && i.getName().toLowerCase().contains(teleport.toLowerCase()));

            if (item != null) {
                log("Found teleport item: " + item.getName());
                if (Bank.withdraw(item.getName(), 1)) {
                    sleep(300, 500);
                    log("Successfully withdrew " + item.getName());
                    return true;
                }
            }
        }

        log("No teleport items found in bank");
        return false;
    }

    /**
     * Gets statistics about inventory management
     *
     * @return A string with stats
     */
    public String getStatistics() {
        return "Food eaten: " + foodEaten +
                ", Items dropped: " + itemsDropped +
                ", Items banked: " + itemsBanked;
    }

    /**
     * Handles coin pouches in inventory
     *
     * @param threshold The number of pouches to accumulate before opening
     * @return True if pouches were opened
     */
    public boolean handleCoinPouches(int threshold) {
        int pouchCount = Inventory.count("Coin pouch");

        if (pouchCount >= threshold) {
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
     * Helper method to log messages
     */
    private void log(String message) {
        System.out.println("[InventoryManager] " + message);
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

    public int getFoodEaten() {
        return foodEaten;
    }

    public int getItemsDropped() {
        return itemsDropped;
    }

    public int getItemsBanked() {
        return itemsBanked;
    }
}