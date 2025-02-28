package org.managers.antiban;

import java.awt.Point;
import org.dreambot.api.input.Mouse;
import org.util.ScriptUtils;

public class RandomHoverAction implements AntiBanAction {
    private final AntiBanProfile profile;

    public RandomHoverAction(AntiBanProfile profile) {
        this.profile = profile;
    }

    @Override
    public void execute() {
        System.out.println("[AntiBan] Performing random hover");

        // Get random inventory item to hover
        int slot = (int) (Math.random() * 28);
        int slotX = 580 + (slot % 4) * 42;
        int slotY = 225 + (slot / 4) * 36;

        // Move to item
        Point hoverPoint = new Point(slotX, slotY);
        Mouse.move(hoverPoint);

        // Hesitate
        ScriptUtils.sleep(profile.getHesitationTime());

        // Move back to center-ish
        Point centerPoint = profile.getRandomPointNear(300, 300, 100);
        Mouse.move(centerPoint);
    }

    @Override
    public String getActionType() {
        return "hover";
    }
}