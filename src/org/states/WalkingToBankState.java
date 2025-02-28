package org.states;

import org.CodeThiefPro;

public class WalkingToBankState implements ScriptState {
    @Override
    public int execute(CodeThiefPro context) {
        try {
            if (context.getLocationManager().isAtBank()) {
                context.setState(new BankingState());
                return 300;
            }

            context.log("Walking to nearest bank");
            if (context.getLocationManager().walkToBank()) {
                return 1000;
            } else {
                if (context.getLocationManager().isStuck()) {
                    context.log("Multiple bank walk failures. Moving to ERROR state.");
                    context.getLocationManager().resetStuckState();
                    context.setState(new ErrorState());
                }
                return 1000;
            }
        } catch (Exception e) {
            context.log("Error in handleWalkingToBank: " + e.getMessage());
            context.setState(new ErrorState());
            return 1000;
        }
    }
}