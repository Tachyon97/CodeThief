package org.managers.antiban;

import org.util.ScriptUtils;

public class ShortBreakAction implements AntiBanAction {
    private final AntiBanProfile profile;

    public ShortBreakAction(AntiBanProfile profile) {
        this.profile = profile;
    }

    @Override
    public void execute() {
        int breakDuration = profile.getRandomBreakDuration();
        System.out.println("[AntiBan] Taking short break (" + breakDuration + "ms)");
        ScriptUtils.sleep(breakDuration);
    }

    @Override
    public String getActionType() {
        return "break";
    }
}