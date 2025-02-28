package org.states;

import org.CodeThiefPro;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.util.ScriptUtils;

public class ThievingState implements ScriptState {
    private static final int COIN_POUCH_THRESHOLD = 12;

    @Override
    public int execute(CodeThiefPro context) {
        if (context.getInventoryManager().isInventoryFull()) {
            context.setState(new HandlingInventoryState());
            return 300;
        }

        if (context.getInventoryManager().needToHeal()) {
            context.setState(new HandlingHealthState());
            return 300;
        }

        if (context.getInventoryManager().handleCoinPouches(COIN_POUCH_THRESHOLD)) {
            return 600;
        }

        if (context.getCurrentTarget() == null || context.shouldRefreshTarget()) {
            context.setCurrentTarget(context.findThievingTarget());
        }

        if (context.getCurrentTarget() != null) {
            if (context.getCurrentTarget() instanceof NPC) {
                return handleNPCThieving(context, (NPC) context.getCurrentTarget());
            } else if (context.getCurrentTarget() instanceof GameObject) {
                return handleObjectThieving(context, (GameObject) context.getCurrentTarget());
            }
        }

        if (!context.getLocationManager().isThievingTargetAvailable()) {
            context.log("No thieving target found. Walking to location.");
            context.setState(new WalkingToLocationState());
            return 600;
        }

        context.log("Waiting for target to appear");
        return 1000;
    }

    private int handleNPCThieving(CodeThiefPro context, NPC npc) {
        if (System.currentTimeMillis() - context.getLastThievingAttempt() < 1200) {
            return 100;
        }

        if ((npc == null || !npc.exists() || npc.isInCombat() || !npc.canReach())) {
            if (npc != null) {
                if (!npc.canReach()) {
                    context.log("Target " + npc.getName() + " is not reachable. Finding new target.");
                } else if (npc.isInCombat()) {
                    context.log("Target " + npc.getName() + " is in combat. Finding new target.");
                }
            }
            context.setCurrentTarget(null);
            return 600;
        }

        if (context.getAntiBanManager().shouldMisclick()) {
            context.log("Intentional misclick");
            int currentX = npc.getX();
            int currentY = npc.getY();
            java.awt.Point randomPoint = context.getAntiBanManager().getRandomPointNear(currentX, currentY, 5);
            org.dreambot.api.input.Mouse.move(randomPoint);
            org.dreambot.api.input.Mouse.click(false);
            ScriptUtils.sleep(300, 600);
            context.setLastThievingAttempt(System.currentTimeMillis());
            return 1000;
        }

        if (npc.interact("Pickpocket")) {
            context.log("Pickpocketing " + npc.getName());
            context.setLastThievingAttempt(System.currentTimeMillis());
            ScriptUtils.sleep(600, 1200);
            return 300;
        } else {
            context.incrementFailedInteractionAttempts();
            context.log("Failed to interact with " + npc.getName() + " (Attempt " + context.getFailedInteractionAttempts() + ")");

            if (context.getFailedInteractionAttempts() >= context.MAX_FAILED_INTERACTIONS) {
                context.log("Too many failed interaction attempts. Finding new target.");
                context.setCurrentTarget(null);
                context.resetFailedInteractionAttempts();
            }
            return 600;
        }
    }

    private int handleObjectThieving(CodeThiefPro context, GameObject gameObject) {
        if (System.currentTimeMillis() - context.getLastThievingAttempt() < 1200) {
            return 100;
        }

        if (gameObject == null || !gameObject.exists()) {
            context.log("Target object no longer exists. Finding new target.");
            context.setCurrentTarget(null);
            return 600;
        }

        if (!gameObject.canReach()) {
            context.log("Target " + gameObject.getName() + " is not reachable. Finding new target.");
            context.setCurrentTarget(null);
            return 600;
        }

        String action = "Steal-from";
        boolean isStall = gameObject.getName().equals("Tea Stall") || gameObject.getName().equals("Cake Stall");

        context.log("Attempting to " + action + " " + gameObject.getName() + " (distance: " + gameObject.distance() + ")");

        if (isStall && gameObject.distance() > 2) {
            org.dreambot.api.methods.walking.impl.Walking.walk(gameObject.getTile());
            ScriptUtils.sleep(600, 1000);
            context.log("Moving closer to " + gameObject.getName());
            return 600;
        }

        if (gameObject.interact(action)) {
            context.log("Successfully initiated " + action + " on " + gameObject.getName());
            context.setLastThievingAttempt(System.currentTimeMillis());

            if (isStall) {
                ScriptUtils.sleep(1200, 1800);
            } else {
                ScriptUtils.sleep(600, 1200);
            }

            return 300;
        } else {
            context.incrementFailedInteractionAttempts();
            context.log("Failed to interact with " + gameObject.getName() + " (Attempt " + context.getFailedInteractionAttempts() + ")");

            if (context.getFailedInteractionAttempts() >= context.MAX_FAILED_INTERACTIONS) {
                context.log("Too many failed interaction attempts. Finding new target.");
                context.setCurrentTarget(null);
                context.resetFailedInteractionAttempts();
            }

            return 600;
        }
    }
}