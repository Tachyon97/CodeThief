package org.managers.antiban;

import java.util.ArrayList;
import java.util.List;

public class AntiBanActionFactory {
    private final AntiBanProfile profile;

    public AntiBanActionFactory(AntiBanProfile profile) {
        this.profile = profile;
    }

    public List<AntiBanAction> createAllActions() {
        List<AntiBanAction> actions = new ArrayList<>();
        actions.add(new RotateCameraAction(profile));
        actions.add(new CheckSkillsAction(profile));
        actions.add(new ShortBreakAction(profile));
        actions.add(new RandomMouseMovementAction(profile));
        actions.add(new RandomHoverAction(profile));
        actions.add(new CheckRandomTabAction(profile));
        return actions;
    }
}