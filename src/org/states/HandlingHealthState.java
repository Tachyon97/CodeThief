package org.states;

import org.CodeThiefPro;
import org.util.ScriptUtils;

public class HandlingHealthState implements ScriptState {
    @Override
    public int execute(CodeThiefPro context) {
        if (context.getInventoryManager().hasFood()) {
            if (context.getInventoryManager().eatFood()) {
                context.log("[HealthState] Eating food to heal");
                ScriptUtils.sleep(600, 1200);

                if (!context.getInventoryManager().needToHeal()) {
                    context.setCurrentTarget(context.findThievingTarget());

                    if (context.getCurrentTarget() != null) {
                        context.log("[HealthState] Target in view after eating. Continuing thieving.");
                        context.setState(new ThievingState());
                    } else if (context.getLocationManager().isAtThievingArea()) {
                        context.log("[HealthState] At thieving area after eating. Waiting for target.");
                        context.setState(new ThievingState());
                    } else {
                        context.log("[HealthState] No target visible after eating. Walking to location.");
                        context.setState(new WalkingToLocationState());
                    }
                }
                return 300;
            }
        } else {
            if (context.getConfig().isBankingEnabled()) {
                context.setState(new WalkingToBankState());
            } else if (context.getConfig().isFreestyleMode()) {
                context.log("[HealthState] No food in freestyle mode. Logging out.");
                context.stop();
            } else {
                context.log("[HealthState] WARNING: Low health but no food. Continuing to thieve.");
                context.setState(new ThievingState());
            }
        }

        return 600;
    }
}