package org.states;

import org.CodeThiefPro;

public class BankingState implements ScriptState {
    @Override
    public int execute(CodeThiefPro context) {
        if (!context.getLocationManager().isAtBank()) {
            context.log("[BankingState] Not at bank yet, moving to WALKING_TO_BANK state");
            context.setState(new WalkingToBankState());
            return 300;
        }

        if (context.getInventoryManager().handleBanking()) {
            Object target = context.findThievingTarget();

            if (target != null) {
                context.setCurrentTarget(target);
                context.log("[BankingState] Found target near bank. No need to walk to thieving area.");
                context.setState(new ThievingState());
            } else {
                context.setState(new WalkingToLocationState());
            }
            return 300;
        } else {
            context.log("[BankingState] Banking failed, retrying...");
        }

        return 600;
    }
}