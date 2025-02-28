package org.states;

import org.CodeThiefPro;

public class HandlingInventoryState implements ScriptState {
    @Override
    public int execute(CodeThiefPro context) {
        if (context.getConfig().isFreestyleMode() || !context.getConfig().isBankingEnabled()) {
            if (context.getInventoryManager().handleFullInventory()) {
                context.setState(new ThievingState());
                return 300;
            }
        } else {
            context.setState(new WalkingToBankState());
            return 300;
        }
        return 600;
    }
}