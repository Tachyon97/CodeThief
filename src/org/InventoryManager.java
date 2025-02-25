package org;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.wrappers.items.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages inventory operations for the thieving script.
 */
public class InventoryManager {
    private final ScriptConfiguration config;

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
     * Handles a full inventory based on configuration
     *
     * @return True if handled successfully, false if needs to bank
     */
    public boolean handleFullInventory() {
        if (config.isFreestyleMode()) {
            return dropJunkItems();
        } else if (config.isBankingEnabled()) {
            return false; // Signal to transition to banking state
        } else {
            return dropAllExceptValuable();
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
            Bank.depositAllItems();
            sleep(600, 900);

            // Now withdraw food based on priorities from GUI
            if (!withdrawFoodBasedOnPriority()) {
                log("Failed to withdraw food");
                return false;
            }

            // Close the bank
            log("Closing bank...");
            Bank.close();
            sleep(300, 600);

            // Verify we have food
            if (!hasFood()) {
                log("WARNING: No food in inventory after banking");
                // Continue anyway, maybe there's no food in bank
            } else {
                log("Successfully got food from bank");
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
            Bank.withdraw(foodName, amount);
            sleep(300, 500);
            return true;
        }

        // Try partial match
        for (Item bankItem : Bank.all()) {
            if (bankItem != null &&
                    bankItem.getName().toLowerCase().contains(foodName.toLowerCase())) {
                Bank.withdraw(bankItem.getName(), amount);
                sleep(300, 500);
                return true;
            }
        }

        return false;
    }

    /**
     * Try to withdraw any food available in bank
     *
     * @return True if food was withdrawn
     */
    private boolean withdrawAnyFood() {
        // Common food items to try
        String[] commonFoods = {
                "monkfish", "shark", "lobster", "swordfish", "tuna", "salmon",
                "trout", "pike", "cake", "bread", "wine", "potion", "brew"
        };

        // Try common foods first
        for (String food : commonFoods) {
            if (tryWithdrawFood(food, 10)) {
                log("Withdrew common food: " + food);
                return true;
            }
        }

        // Last resort: check all bank items for anything that might be food
        log("No common food found, searching for any food-like items...");
        for (Item item : Bank.all()) {
            if (item != null && couldBeFood(item.getName())) {
                Bank.withdraw(item.getName(), 10);
                sleep(300, 500);
                log("Withdrew possible food: " + item.getName());
                return true;
            }
        }

        log("No food found in bank at all");
        return false;
    }

    /**
     * Check if an item name could possibly be food
     */
    private boolean couldBeFood(String name) {
        if (name == null) return false;

        name = name.toLowerCase();
        String[] foodKeywords = {
                "fish", "cake", "bread", "pizza", "pie", "stew", "wine",
                "potion", "brew", "food", "shark", "lobster", "salmon", "tuna",
                "monkfish", "karambwan", "fruit", "potato", "curry", "soup"
        };

        for (String keyword : foodKeywords) {
            if (name.contains(keyword)) {
                return true;
            }
        }

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

        // Drop all junk seeds
        for (String seedName : junkSeeds) {
            @NonNull List<Item> seeds = Inventory.all(item ->
                    item != null && item.getName().toLowerCase().contains(seedName));

            for (Item seed : seeds) {
                seed.interact("Drop");
                sleep(100, 200);
            }
        }

        return true;
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
        valuableItems.add("coin");
        valuableItems.add("gem");
        valuableItems.add("sapphire");
        valuableItems.add("emerald");
        valuableItems.add("ruby");
        valuableItems.add("diamond");
        valuableItems.add("gold");
        valuableItems.add("silver");

        // Food items
        config.getFoodItems().forEach(food -> valuableItems.add(food.toLowerCase()));

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
        @NonNull List<Item> items = Inventory.all(item -> {
            if (item == null) return false;

            String itemName = item.getName().toLowerCase();
            for (String keepName : itemsToKeep) {
                if (keepName == null) continue;
                if (itemName.contains(keepName.toLowerCase())) {
                    return false; // Don't drop this item
                }
            }
            return true; // Drop this item
        });

        for (Item item : items) {
            item.interact("Drop");
            sleep(100, 200);
        }

        return true;
    }

    /**
     * Checks if the player needs to heal
     *
     * @return True if health is below threshold
     */
    public boolean needToHeal() {
        return Skills.getBoostedLevel(Skill.HITPOINTS) < config.getHealthThreshold();
    }

    /**
     * Attempts to eat food from inventory
     *
     * @return True if food was eaten
     */
    public boolean eatFood() {
        // Get food items from configuration
        List<String> configuredFoods = config.getFoodItems();

        // First try exact matches from configuration
        for (String foodName : configuredFoods) {
            Item food = Inventory.get(item ->
                    item != null && item.getName().equalsIgnoreCase(foodName));

            if (food != null) {
                log("Eating configured food: " + food.getName());
                // Try different interaction methods
                if (food.hasAction("Eat")) {
                    return food.interact("Eat");
                } else {
                    // Some foods might use different action names
                    return food.interact();
                }
            }
        }

        // Then try partial matches
        for (String foodName : configuredFoods) {
            Item food = Inventory.get(item ->
                    item != null && item.getName().toLowerCase().contains(foodName.toLowerCase()));

            if (food != null) {
                log("Eating food (partial match): " + food.getName());
                if (food.hasAction("Eat")) {
                    return food.interact("Eat");
                } else {
                    return food.interact();
                }
            }
        }

        // Last resort - try to find anything edible
        Item possibleFood = Inventory.get(this::couldBeFood);
        if (possibleFood != null) {
            log("Trying to eat possible food: " + possibleFood.getName());
            if (possibleFood.hasAction("Eat")) {
                return possibleFood.interact("Eat");
            } else {
                return possibleFood.interact();
            }
        }

        return false;
    }

    // Helper method to check if item could be food based on name
    private boolean couldBeFood(Item item) {
        if (item == null) return false;

        String name = item.getName().toLowerCase();
        String[] foodKeywords = {
                "fish", "cake", "bread", "pizza", "pie", "stew", "wine",
                "potion", "brew", "food", "shark", "lobster", "salmon", "tuna",
                "monkfish", "karambwan", "fruit", "potato", "curry", "soup"
        };

        for (String keyword : foodKeywords) {
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
            if (foodName != null && name.contains(foodName)) {
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

        // List of valuable items
        String[] valuableItems = {
                "coin", "token", "nugget", "gem", "sapphire", "emerald", "ruby",
                "diamond", "gold", "silver", "ranarr", "snapdragon", "torstol"
        };

        for (String valuable : valuableItems) {
            if (name.contains(valuable)) {
                return true;
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
}