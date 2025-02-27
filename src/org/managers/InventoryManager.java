package org.managers;

import org.core.config.ScriptConfiguration;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.equipment.Equipment;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.wrappers.items.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InventoryManager {
    private final ScriptConfiguration config;

    private static final String[] TELEPORT_ITEMS = {
            "Amulet of glory", "Games necklace", "Ring of dueling",
            "Skills necklace", "Ring of wealth", "Combat bracelet",
            "Ardougne teleport", "Ardougne cloak", "Varrock teleport",
            "Falador teleport", "Camelot teleport", "Lumbridge teleport"
    };

    private static final String[] FOOD_KEYWORDS = {
            "fish", "cake", "bread", "pizza", "pie", "stew", "wine",
            "potion", "brew", "food", "shark", "lobster", "salmon", "tuna",
            "monkfish", "karambwan", "fruit", "potato", "curry", "soup"
    };

    private static final String[] VALUABLE_KEYWORDS = {
            "coin", "token", "nugget", "gem", "sapphire", "emerald", "ruby",
            "diamond", "gold", "silver", "torstol", "ranarr", "snapdragon",
            "necklace", "bracelet", "ring", "amulet", "clue scroll"
    };

    private int foodEaten = 0;
    private int itemsDropped = 0;
    private int itemsBanked = 0;
    private long lastFoodCheck = 0;
    private static final long FOOD_CHECK_COOLDOWN = 3000;

    private LocationManager locationManager;

    public InventoryManager(ScriptConfiguration config) {
        this.config = config;
    }

    public boolean isInventoryFull() {
        return Inventory.isFull();
    }

    public int getFreeSlots() {
        return Inventory.emptySlotCount();
    }

    public int getItemCount() {
        return Inventory.fullSlotCount();
    }

    public boolean hasSpaceFor(int n) {
        return Inventory.emptySlotCount() >= n;
    }

    public boolean handleFullInventory() {
        try {
            if (config.isFreestyleMode()) {
                return dropJunkItems();
            } else if (config.isBankingEnabled()) {
                return false;
            } else {
                return dropAllExceptValuable();
            }
        } catch (Exception e) {
            System.out.println("Error handling full inventory: " + e.getMessage());
            e.printStackTrace();
            return emergencyDropItems();
        }
    }

    private boolean emergencyDropItems() {
        try {
            System.out.println("EMERGENCY: Attempting to drop some items to free space");
            int initialCount = Inventory.fullSlotCount();

            for (Item item : Inventory.all()) {
                if (item != null && !isValuableItem(item) && !isEdibleItem(item)) {
                    item.interact("Drop");
                    sleep(100, 200);
                    itemsDropped++;

                    if (initialCount - Inventory.fullSlotCount() >= 3) {
                        break;
                    }
                }
            }

            return initialCount > Inventory.fullSlotCount();
        } catch (Exception e) {
            System.out.println("Even emergency drop failed: " + e.getMessage());
            return false;
        }
    }

    public boolean handleBanking() {
        try {
            if (!Bank.isOpen()) {
                System.out.println("Opening bank...");
                if (!Bank.open()) {
                    System.out.println("Failed to open bank");
                    return false;
                }
                sleep(600, 900);
            }

            System.out.println("Depositing all items...");
            int itemsBeforeDeposit = Inventory.fullSlotCount();

            if (Bank.depositAllItems()) {
                sleep(600, 900);
                itemsBanked += (itemsBeforeDeposit - Inventory.fullSlotCount());
                System.out.println("Deposited " + (itemsBeforeDeposit - Inventory.fullSlotCount()) + " items");
            } else {
                System.out.println("Failed to deposit items");
            }

            // Check if we're already at the thieving area, and only withdraw teleport if we're not
            boolean atThievingArea = locationManager != null && locationManager.isAtThievingArea();
            if (!atThievingArea) {
                // Only withdraw teleport items if we need to travel back
                withdrawTargetSpecificTeleport();
                sleep(300, 600);
            } else {
                System.out.println("Already at thieving area, skipping teleport item withdrawal");
            }

            // Always withdraw food based on priorities
            if (!withdrawFoodBasedOnPriority()) {
                System.out.println("Failed to withdraw food");
            }

            System.out.println("Closing bank...");
            Bank.close();
            sleep(300, 600);

            // Log what we have
            if (hasTeleportItem() && hasFood()) {
                System.out.println("Successfully got food and teleport items from bank");
            } else if (hasTeleportItem()) {
                System.out.println("Got teleport items but no food");
            } else if (hasFood()) {
                System.out.println("Got food but no teleport items");
            } else {
                System.out.println("WARNING: Failed to get food or teleport items");
            }

            return true;
        } catch (Exception e) {
            System.out.println("Banking error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean withdrawTargetSpecificTeleport() {
        String targetName = config.getCurrentTargetName();
        boolean isArdougneTarget = targetName.equals("Knight of Ardougne") ||
                targetName.equals("Paladin") ||
                targetName.equals("Hero") ||
                targetName.equals("Cake Stall");

        // Check Ardougne teleport for Ardougne targets
        if (isArdougneTarget) {
            // Try Ardougne teleport tablet first
            Item ardyTeleport = Bank.get(item ->
                    item != null && item.getName().toLowerCase().contains("ardougne teleport"));

            if (ardyTeleport != null) {
                System.out.println("Found Ardougne teleport in bank");
                if (Bank.withdraw(ardyTeleport.getName(), 5)) {
                    sleep(300, 600);
                    return true;
                }
            }

            // Try Ardougne cloak next
            Item ardyCloak = Bank.get(item ->
                    item != null && item.getName().toLowerCase().contains("ardougne cloak"));

            if (ardyCloak != null) {
                System.out.println("Found Ardougne cloak in bank");
                if (Bank.withdraw(ardyCloak.getName(), 1)) {
                    sleep(300, 600);
                    return true;
                }
            }
        }

        // Varrock teleport for Tea Stall
        if (targetName.equals("Tea Stall")) {
            Item varrockTele = Bank.get(item ->
                    item != null && item.getName().toLowerCase().contains("varrock teleport"));

            if (varrockTele != null) {
                System.out.println("Found Varrock teleport in bank");
                if (Bank.withdraw(varrockTele.getName(), 5)) {
                    sleep(300, 600);
                    return true;
                }
            }
        }

        // Draynor teleport options for farmers/master farmers
        if (targetName.equals("Farmer") || targetName.equals("Master Farmer")) {
            // First try glory for Draynor
            Item glory = Bank.get(item ->
                    item != null && item.getName().toLowerCase().contains("glory"));

            if (glory != null) {
                System.out.println("Found Amulet of Glory in bank for Draynor teleport");
                if (Bank.withdraw(glory.getName(), 1)) {
                    sleep(300, 600);
                    return true;
                }
            }
        }

        // Generic teleport items as fallback
        for (String teleportItem : TELEPORT_ITEMS) {
            Item item = Bank.get(i ->
                    i != null && i.getName().toLowerCase().contains(teleportItem.toLowerCase()));

            if (item != null) {
                int amountToWithdraw = item.getName().toLowerCase().contains("teleport") ? 5 : 1;
                System.out.println("Found teleport item: " + item.getName());
                if (Bank.withdraw(item.getName(), amountToWithdraw)) {
                    sleep(300, 500);
                    return true;
                }
            }
        }

        return false;
    }

    private boolean withdrawFoodBasedOnPriority() {
        List<String> foodPriorities = config.getFoodItems();

        if (foodPriorities.isEmpty()) {
            System.out.println("No food preferences configured");
            return withdrawAnyFood();
        }

        System.out.println("Food priorities: " + foodPriorities);

        String primaryFood = foodPriorities.get(0);
        System.out.println("Trying primary food: " + primaryFood);

        if (tryWithdrawFood(primaryFood, 10)) {
            System.out.println("Withdrew primary food: " + primaryFood);
            return true;
        }

        System.out.println("Primary food not found, trying alternatives...");
        for (int i = 1; i < foodPriorities.size(); i++) {
            String altFood = foodPriorities.get(i);
            if (tryWithdrawFood(altFood, 10)) {
                System.out.println("Withdrew alternative food: " + altFood);
                return true;
            }
        }

        System.out.println("No configured food found, trying any food...");
        return withdrawAnyFood();
    }

    private boolean tryWithdrawFood(String foodName, int amount) {
        if (Bank.contains(item ->
                item != null &&
                        item.getName().equalsIgnoreCase(foodName))) {
            return Bank.withdraw(foodName, amount);
        }

        Item bankItem = Bank.get(item ->
                item != null &&
                        item.getName().toLowerCase().contains(foodName.toLowerCase()));

        if (bankItem != null) {
            return Bank.withdraw(bankItem.getName(), amount);
        }

        return false;
    }

    private boolean withdrawAnyFood() {
        for (String keyword : FOOD_KEYWORDS) {
            Item foodItem = Bank.get(item ->
                    item != null &&
                            item.getName().toLowerCase().contains(keyword.toLowerCase()));

            if (foodItem != null) {
                System.out.println("Found food item: " + foodItem.getName());
                if (Bank.withdraw(foodItem.getName(), 10)) {
                    System.out.println("Withdrew food: " + foodItem.getName());
                    sleep(300, 500);
                    return true;
                }
            }
        }

        System.out.println("No food found in bank at all");
        return false;
    }

    public boolean dropJunkItems() {
        String currentTarget = config.getCurrentTargetName();

        if (currentTarget.equals("Master Farmer")) {
            return dropJunkSeeds();
        } else if (currentTarget.toLowerCase().contains("stall")) {
            return dropStallJunk();
        } else {
            return dropAllExceptValuable();
        }
    }

    private boolean dropJunkSeeds() {
        String[] junkSeeds = {
                "potato seed", "onion seed", "cabbage seed", "tomato seed",
                "sweetcorn seed", "strawberry seed", "gardening seed", "barley seed",
                "hammerstone seed", "asgarnian seed", "jute seed", "yanillian seed",
                "krandorian seed", "wildblood seed"
        };

        int itemsDroppedCount = 0;

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
            System.out.println("Dropped " + itemsDroppedCount + " junk seeds");
        }

        return itemsDroppedCount > 0;
    }

    private boolean dropStallJunk() {
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
        } else if (stallType.equals("Cake Stall")) {
            valuableItems.add("cake");
        } else if (stallType.equals("Tea Stall")) {
            valuableItems.add("tea");
            valuableItems.add("cup");
        }

        valuableItems.add("coin");
        config.getFoodItems().forEach(food -> valuableItems.add(food.toLowerCase()));

        for (String teleport : TELEPORT_ITEMS) {
            valuableItems.add(teleport.toLowerCase());
        }

        return dropAllExcept(valuableItems);
    }

    private boolean dropAllExceptValuable() {
        List<String> valuableItems = new ArrayList<>();

        for (String keyword : VALUABLE_KEYWORDS) {
            valuableItems.add(keyword.toLowerCase());
        }

        config.getFoodItems().forEach(food -> valuableItems.add(food.toLowerCase()));

        for (String teleport : TELEPORT_ITEMS) {
            valuableItems.add(teleport.toLowerCase());
        }

        return dropAllExcept(valuableItems);
    }

    private boolean dropAllExcept(List<String> itemsToKeep) {
        List<Item> itemsToDrop = Inventory.all().stream()
                .filter(item -> item != null)
                .filter(item -> {
                    String itemName = item.getName().toLowerCase();
                    for (String keepName : itemsToKeep) {
                        if (keepName == null) continue;
                        if (itemName.contains(keepName.toLowerCase())) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        int itemsDroppedCount = 0;

        for (Item item : itemsToDrop) {
            if (item != null && item.interact("Drop")) {
                sleep(100, 200);
                itemsDroppedCount++;
            }
        }

        if (itemsDroppedCount > 0) {
            itemsDropped += itemsDroppedCount;
            System.out.println("Dropped " + itemsDroppedCount + " items");
        }

        return itemsDroppedCount > 0;
    }

    public boolean needToHeal() {
        if (System.currentTimeMillis() - lastFoodCheck < FOOD_CHECK_COOLDOWN) {
            return false;
        }

        lastFoodCheck = System.currentTimeMillis();

        int currentHP = Skills.getBoostedLevel(Skill.HITPOINTS);
        int maxHP = Skills.getRealLevel(Skill.HITPOINTS);
        int healthPercent = (int) (((double) currentHP / maxHP) * 100);

        return healthPercent < config.getHealthThreshold();
    }

    public boolean eatFood() {
        if (!hasFood()) {
            return false;
        }

        List<String> configuredFoods = config.getFoodItems();

        for (String foodName : configuredFoods) {
            Item food = Inventory.get(item ->
                    item != null && item.getName().equalsIgnoreCase(foodName));

            if (food != null) {
                System.out.println("Eating configured food: " + food.getName());
                if (eatFoodItem(food)) {
                    foodEaten++;
                    return true;
                }
            }
        }

        for (String foodName : configuredFoods) {
            Item food = Inventory.get(item ->
                    item != null && item.getName().toLowerCase().contains(foodName.toLowerCase()));

            if (food != null) {
                System.out.println("Eating food (partial match): " + food.getName());
                if (eatFoodItem(food)) {
                    foodEaten++;
                    return true;
                }
            }
        }

        Item possibleFood = Inventory.get(this::isEdibleItem);
        if (possibleFood != null) {
            System.out.println("Trying to eat possible food: " + possibleFood.getName());
            if (eatFoodItem(possibleFood)) {
                foodEaten++;
                return true;
            }
        }

        return false;
    }

    private boolean eatFoodItem(Item food) {
        if (food == null) {
            return false;
        }

        if (food.hasAction("Eat")) {
            return food.interact("Eat");
        }

        if (food.hasAction("Drink")) {
            return food.interact("Drink");
        }

        if (food.hasAction("Consume")) {
            return food.interact("Consume");
        }

        return food.interact();
    }

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

    public boolean hasFood() {
        return Inventory.contains(this::isEdibleItem);
    }

    public boolean isEdibleItem(Item item) {
        if (item == null) {
            return false;
        }

        String name = item.getName().toLowerCase();

        for (String foodName : config.getFoodItems()) {
            if (foodName != null && name.contains(foodName.toLowerCase())) {
                return true;
            }
        }

        return couldBeFood(name);
    }

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

    public boolean hasTeleportItem() {
        for (String teleport : TELEPORT_ITEMS) {
            if (Inventory.contains(item ->
                    item != null && item.getName().toLowerCase().contains(teleport.toLowerCase()))) {
                return true;
            }
        }

        for (String teleport : TELEPORT_ITEMS) {
            if (Equipment.contains(item ->
                    item != null && item.getName().toLowerCase().contains(teleport.toLowerCase()))) {
                return true;
            }
        }

        return false;
    }

    public Item getTeleportItem() {
        for (String teleport : TELEPORT_ITEMS) {
            Item item = Inventory.get(i ->
                    i != null && i.getName().toLowerCase().contains(teleport.toLowerCase()));

            if (item != null) {
                return item;
            }
        }

        for (String teleport : TELEPORT_ITEMS) {
            Item item = Equipment.get(i ->
                    i != null && i.getName().toLowerCase().contains(teleport.toLowerCase()));

            if (item != null) {
                return item;
            }
        }

        return null;
    }

    public boolean hasCertainTeleportItem(String teleportType) {
        // Check inventory first
        if (Inventory.contains(item ->
                item != null && item.getName().toLowerCase().contains(teleportType.toLowerCase()))) {
            return true;
        }

        // Then check equipment
        return Equipment.contains(item ->
                item != null && item.getName().toLowerCase().contains(teleportType.toLowerCase()));
    }

    public String getStatistics() {
        return "Food eaten: " + foodEaten +
                ", Items dropped: " + itemsDropped +
                ", Items banked: " + itemsBanked;
    }

    public boolean handleCoinPouches(int threshold) {
        int pouchCount = Inventory.count("Coin pouch");

        if (pouchCount >= threshold) {
            System.out.println("Opening " + pouchCount + " coin pouches");
            Item pouch = Inventory.get("Coin pouch");

            if (pouch != null) {
                if (pouch.hasAction("Open-all")) {
                    return pouch.interact("Open-all");
                } else {
                    return pouch.interact("Open");
                }
            }
        }

        return false;
    }

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

    public void setLocationManager(LocationManager locationManager) {
        this.locationManager = locationManager;
    }
}