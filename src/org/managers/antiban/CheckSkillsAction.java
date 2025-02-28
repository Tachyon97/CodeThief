package org.managers.antiban;

import java.awt.Point;

import org.dreambot.api.input.Mouse;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.util.ScriptUtils;

public class CheckSkillsAction implements AntiBanAction {
    private final AntiBanProfile profile;

    public CheckSkillsAction(AntiBanProfile profile) {
        this.profile = profile;
    }

    @Override
    public void execute() {
        System.out.println("[AntiBan] Checking skills tab");

        if (Tabs.open(Tab.SKILLS)) {
            ScriptUtils.sleep(profile.getRandomActionDelay());

            Skill skillToCheck;
            switch (profile.getSkillCheckStyle()) {
                case 0:
                    skillToCheck = Skill.THIEVING;
                    break;
                case 1:
                    skillToCheck = (Math.random() < 0.7) ? Skill.THIEVING :
                            (Math.random() < 0.5 ? Skill.ATTACK : Skill.STRENGTH);
                    break;
                case 2:
                    Skill[] allSkills = Skill.values();
                    skillToCheck = allSkills[(int) (Math.random() * allSkills.length)];
                    break;
                case 3:
                    double rand = Math.random();
                    if (rand < 0.6) skillToCheck = Skill.THIEVING;
                    else if (rand < 0.8) skillToCheck = Skill.AGILITY;
                    else skillToCheck = Skill.HITPOINTS;
                    break;
                default:
                    skillToCheck = Skill.THIEVING;
            }

            int skillLevel = Skills.getRealLevel(skillToCheck);
            System.out.println("[AntiBan] Checking " + skillToCheck.name() + " (Level " + skillLevel + ")");

            Point skillLocation = getSkillTabLocation(skillToCheck);
            Mouse.move(skillLocation);

            ScriptUtils.sleep(profile.getHesitationTime());
            ScriptUtils.sleep(profile.getRandomActionDelay());

            Tabs.open(Tab.INVENTORY);
        }
    }

    @Override
    public String getActionType() {
        return "skills";
    }

    private Point getSkillTabLocation(Skill skill) {
        int baseX = 550;
        int baseY = 205;

        int column, row;

        switch (skill) {
            case ATTACK:
                column = 0;
                row = 0;
                break;
            case STRENGTH:
                column = 1;
                row = 0;
                break;
            case DEFENCE:
                column = 2;
                row = 0;
                break;
            case RANGED:
                column = 3;
                row = 0;
                break;
            case PRAYER:
                column = 0;
                row = 1;
                break;
            case MAGIC:
                column = 1;
                row = 1;
                break;
            case RUNECRAFTING:
                column = 2;
                row = 1;
                break;
            case CONSTRUCTION:
                column = 3;
                row = 1;
                break;
            case HITPOINTS:
                column = 0;
                row = 2;
                break;
            case AGILITY:
                column = 1;
                row = 2;
                break;
            case HERBLORE:
                column = 2;
                row = 2;
                break;
            case THIEVING:
                column = 3;
                row = 2;
                break;
            case CRAFTING:
                column = 0;
                row = 3;
                break;
            case FLETCHING:
                column = 1;
                row = 3;
                break;
            case SLAYER:
                column = 2;
                row = 3;
                break;
            case HUNTER:
                column = 3;
                row = 3;
                break;
            default:
                column = 0;
                row = 4;
        }

        int posX = baseX + (column * 63) + (int) (Math.random() * 10) - 5;
        int posY = baseY + (row * 33) + (int) (Math.random() * 10) - 5;

        return new Point(posX, posY);
    }
}