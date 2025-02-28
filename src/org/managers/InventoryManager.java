package org.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.core.config.ScriptConfiguration;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.wrappers.items.Item;
import org.util.ScriptUtils;

public class InventoryManager {
    private final ScriptConfiguration config;
    private LocationManager locationManager;

    private int foodEaten = 0;
    private int itemsDropped = 0;
    private int itemsBanked = 0;
    private long lastFoodCheck = 0;

    private static final long FOOD_CHECK_COOLDOWN = 3000;

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

    public InventoryManager(ScriptConfiguration config) {
        this.config = config;
    }

    public void setLocationManager(LocationManager locationManager) {
        this.locationManager = locationManager;
    }

    public boolean handleFullInventory() {
        try {
            if (config.isFreestyleMode()) {
                return executeDropStrategy(new TargetBasedDropStrategy(config.getCurrentTargetName()));
            } else if (config.isBankingEnabled()) {
                return false;
            } else {
                return executeDropStrategy(new ValueBasedDropStrategy());
            }
        } catch (Exception e) {
            System.out.println("Error handling full inventory: " + e.getMessage());
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
                    ScriptUtils.sleep(100, 200);
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
                ScriptUtils.sleep(600, 900);
            }

            System.out.println("Depositing all items...");
            int itemsBeforeDeposit = Inventory.fullSlotCount();

            if (Bank.depositAllItems()) {
                ScriptUtils.sleep(600, 900);
                itemsBanked += (itemsBeforeDeposit - Inventory.fullSlotCount());
                System.out.println("Deposited " + (itemsBeforeDeposit - Inventory.fullSlotCount()) + " items");
            } else {
                System.out.println("Failed to deposit items");
            }

            withdrawFoodBasedOnPriority();

            System.out.println("Closing bank...");
            Bank.close();
            ScriptUtils.sleep(300, 600);

            return hasFood();
        } catch (Exception e) {
            System.out.println("Banking error: " + e.getMessage());
            return false;
        }
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
                    ScriptUtils.sleep(300, 500);
                    return true;
                }
            }
        }

        System.out.println("No food found in bank at all");
        return false;
    }

    private boolean executeDropStrategy(DropStrategy strategy) {
        List<Item> itemsToDrop = strategy.getItemsToDrop(Inventory.all());
        int itemsDroppedCount = 0;

        for (Item item : itemsToDrop) {
            if (item != null && item.interact("Drop")) {
                ScriptUtils.sleep(100, 200);
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

        Item foodItem = findFoodItemToEat();
        if (foodItem != null) {
            System.out.println("Eating food: " + foodItem.getName());
            if (eatFoodItem(foodItem)) {
                foodEaten++;
                return true;
            }
        }

        return false;
    }

    private Item findFoodItemToEat() {
        List<String> configuredFoods = config.getFoodItems();

        for (String foodName : configuredFoods) {
            Item food = Inventory.get(item ->
                    item != null && item.getName().equalsIgnoreCase(foodName));

            if (food != null) {
                return food;
            }
        }

        for (String foodName : configuredFoods) {
            Item food = Inventory.get(item ->
                    item != null && item.getName().toLowerCase().contains(foodName.toLowerCase()));

            if (food != null) {
                return food;
            }
        }

        return Inventory.get(this::isEdibleItem);
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

    public int getFoodEaten() {
        return foodEaten;
    }

    public int getItemsDropped() {
        return itemsDropped;
    }

    public int getItemsBanked() {
        return itemsBanked;
    }

    private interface DropStrategy {
        List<Item> getItemsToDrop(List<Item> inventory);
    }

    private class ValueBasedDropStrategy implements DropStrategy {
        @Override
        public List<Item> getItemsToDrop(List<Item> inventory) {
            List<String> valuableItems = new ArrayList<>();

            for (String keyword : VALUABLE_KEYWORDS) {
                valuableItems.add(keyword.toLowerCase());
            }

            config.getFoodItems().forEach(food -> valuableItems.add(food.toLowerCase()));

            return inventory.stream()
                    .filter(item -> item != null)
                    .filter(item -> {
                        String itemName = item.getName().toLowerCase();
                        return valuableItems.stream().noneMatch(keepName ->
                                keepName != null && itemName.contains(keepName.toLowerCase()));
                    })
                    .collect(Collectors.toList());
        }
    }

    private class TargetBasedDropStrategy implements DropStrategy {
        private final String targetName;

        public TargetBasedDropStrategy(String targetName) {
            this.targetName = targetName;
        }

        @Override
        public List<Item> getItemsToDrop(List<Item> inventory) {
            if (targetName.equals("Master Farmer")) {
                return getJunkSeedsDropList(inventory);
            } else if (targetName.toLowerCase().contains("stall")) {
                return getStallJunkDropList(inventory);
            } else {
                return new ValueBasedDropStrategy().getItemsToDrop(inventory);
            }
        }

        private List<Item> getJunkSeedsDropList(List<Item> inventory) {
            String[] junkSeeds = {
                    "potato seed", "onion seed", "cabbage seed", "tomato seed",
                    "sweetcorn seed", "strawberry seed", "gardening seed", "barley seed",
                    "hammerstone seed", "asgarnian seed", "jute seed", "yanillian seed",
                    "krandorian seed", "wildblood seed"
            };

            return inventory.stream()
                    .filter(item -> item != null)
                    .filter(item -> {
                        String itemName = item.getName().toLowerCase();
                        for (String seedName : junkSeeds) {
                            if (itemName.contains(seedName.toLowerCase())) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        }

        private List<Item> getStallJunkDropList(List<Item> inventory) {
            List<String> valuableItems = new ArrayList<>();

            switch (targetName) {
                case "Gem Stall":
                    valuableItems.add("sapphire");
                    valuableItems.add("emerald");
                    valuableItems.add("ruby");
                    valuableItems.add("diamond");
                    break;
                case "Silk Stall":
                    valuableItems.add("silk");
                    break;
                case "Silver Stall":
                    valuableItems.add("silver");
                    break;
                case "Cake Stall":
                    valuableItems.add("cake");
                    break;
                case "Tea Stall":
                    valuableItems.add("tea");
                    valuableItems.add("cup");
                    break;
            }

            valuableItems.add("coin");
            config.getFoodItems().forEach(food -> valuableItems.add(food.toLowerCase()));

            return inventory.stream()
                    .filter(item -> item != null)
                    .filter(item -> {
                        String itemName = item.getName().toLowerCase();
                        return valuableItems.stream().noneMatch(keepName ->
                                keepName != null && itemName.contains(keepName.toLowerCase()));
                    })
                    .collect(Collectors.toList());
        }
    }
}