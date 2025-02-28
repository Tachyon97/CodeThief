package org.managers.antiban;

import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.util.ScriptUtils;

public class CheckRandomTabAction implements AntiBanAction {
    private final AntiBanProfile profile;

    public CheckRandomTabAction(AntiBanProfile profile) {
        this.profile = profile;
    }

    @Override
    public void execute() {
        Tab[] commonTabs = {Tab.INVENTORY, Tab.EQUIPMENT, Tab.PRAYER, Tab.COMBAT};
        Tab tabToCheck = commonTabs[(int) (Math.random() * commonTabs.length)];

        System.out.println("[AntiBan] Checking " + tabToCheck.name() + " tab");

        if (Tabs.open(tabToCheck)) {
            ScriptUtils.sleep(profile.getRandomActionDelay());

            // Simulate looking at the tab content
            ScriptUtils.sleep(200, 700);

            // Return to inventory
            Tabs.open(Tab.INVENTORY);
        }
    }

    @Override
    public String getActionType() {
        return "tabs";
    }
}