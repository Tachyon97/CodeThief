package org.managers.antiban;

import java.awt.Point;
import org.dreambot.api.input.Mouse;

public class RandomMouseMovementAction implements AntiBanAction {
    private final AntiBanProfile profile;

    public RandomMouseMovementAction(AntiBanProfile profile) {
        this.profile = profile;
    }

    @Override
    public void execute() {
        System.out.println("[AntiBan] Moving mouse randomly");

        Point p = profile.getRandomPointNear(
                300,
                300,
                100
        );

        Mouse.move(p);
    }

    @Override
    public String getActionType() {
        return "mouse";
    }
}