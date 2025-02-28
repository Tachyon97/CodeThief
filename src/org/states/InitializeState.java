package org.states;

import org.CodeThiefPro;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;

public class InitializeState implements ScriptState {
    @Override
    public int execute(CodeThiefPro context) {
        int thievingLevel = Skills.getRealLevel(Skill.THIEVING);

        if (context.getConfig().isAutoProgressionEnabled()) {
            String optimalTarget = context.getConfig().determineOptimalTarget(thievingLevel);
            context.getConfig().setCurrentTargetName(optimalTarget);
            context.log("Auto-progression set target to: " + optimalTarget);
        }

        context.getStatsTracker().setCurrentTarget(context.getConfig().getCurrentTargetName());

        if (context.getInventoryManager().needToHeal() && !context.getInventoryManager().hasFood() && context.getConfig().isBankingEnabled()) {
            context.log("Need to heal but no food available. Going to bank first.");
            context.setState(new WalkingToBankState());
            return 300;
        }

        if (context.getLocationManager().isAtThievingArea()) {
            Object target = context.findThievingTarget();
            if (target != null) {
                context.setCurrentTarget(target);
                context.log("Target already in view. Starting thieving without walking.");
                context.setState(new ThievingState());
                return 300;
            }
        }

        Object target = context.findThievingTarget();
        if (target != null) {
            context.setCurrentTarget(target);
            context.log("Target found. Starting thieving.");
            context.setState(new ThievingState());
            return 300;
        }

        context.setState(new WalkingToLocationState());
        return 300;
    }
}