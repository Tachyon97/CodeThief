package org.states;

import org.CodeThiefPro;

public class WalkingToLocationState implements ScriptState {
    @Override
    public int execute(CodeThiefPro context) {
        Object target = context.findThievingTarget();
        if (target != null) {
            context.setCurrentTarget(target);
            context.log("Found target while preparing to walk. Skipping walk.");
            context.setState(new ThievingState());
            return 300;
        }

        if (context.getInventoryManager().needToHeal()) {
            context.setState(new HandlingHealthState());
            return 300;
        }

        if (context.getLocationManager().isAtThievingArea()) {
            context.log("Arrived at thieving location");
            context.setState(new ThievingState());
            return 300;
        }

        if (context.getLocationManager().walkToThievingArea()) {
            context.log("Walking to thieving location: " + context.getConfig().getCurrentTargetName());
            return 1000;
        } else {
            if (context.getLocationManager().isStuck()) {
                context.log("Walking appears to be stuck. Trying to recover.");
                context.getLocationManager().resetStuckState();
                context.setState(new ErrorState());
            }
            return 1000;
        }
    }
}