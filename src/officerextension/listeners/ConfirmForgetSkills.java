package officerextension.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.plugins.OfficerLevelupPlugin;
import com.fs.starfarer.campaign.CharacterStats;
import officerextension.Settings;
import officerextension.Util;
import officerextension.ui.OfficerUIElement;
import officerextension.ui.SkillButton;

public class ConfirmForgetSkills extends DialogDismissedListener {

    private final OfficerUIElement uiElement;
    private final OfficerDataAPI officerData;

    public ConfirmForgetSkills(OfficerUIElement uiElement) {
        this.uiElement = uiElement;
        this.officerData = uiElement.getOfficerData();
    }

    @Override
    public void trigger(Object... args) {
        // The second argument is 0 if confirmed, 1 if canceled
        int option = (int) args[1];
        if (option == 1) {
            return;
        }
        // Have to do the check again here, since the player can press space bar to confirm despite
        // the confirm button being disabled
        int spCost = Settings.DEMOTE_OFFICER_SP_COST;
        if (Global.getSector().getPlayerStats().getStoryPoints() < spCost) {
            return;
        }
        if (spCost > 0) {
            Global.getSoundPlayer().playUISound("ui_char_spent_story_point_leadership", 1f, 1f);
            Global.getSector().getPlayerStats().spendStoryPoints(
                    spCost,
                    true,
                    null,
                    true,
                    Settings.DEMOTE_BONUS_XP_FRACTION,
                    "Demoted an officer: " + officerData.getPerson().getNameString());
        }
        MutableCharacterStatsAPI stats = officerData.getPerson().getStats();
        // If this was an exceptional pod officer, retain max level and max elite skills data
        MemoryAPI memory = officerData.getPerson().getMemoryWithoutUpdate();
        if (memory.getBoolean(MemFlags.EXCEPTIONAL_SLEEPER_POD_OFFICER)) {
            if (!memory.contains(MemFlags.OFFICER_MAX_LEVEL)) {
                memory.set(MemFlags.OFFICER_MAX_LEVEL, stats.getLevel());
            }
            if (!memory.contains(MemFlags.OFFICER_MAX_ELITE_SKILLS)) {
                memory.set(MemFlags.OFFICER_MAX_ELITE_SKILLS, Util.countEliteSkills(officerData));
            }
        }
        int forgotSkills = 0;
        for (SkillButton button : uiElement.getWrappedSkillButtons()) {
            if (button.isSelected()) {
                String skillId = button.getSkillSpec().getId();
                // If elite, give some bonus XP
                // Default is 0 due to inability to differentiate between bought elite skills
                // and those that were already elite (i.e. cryopod officers)
                if (stats.getSkillLevel(skillId) > 1) {
                    Global.getSector().getPlayerStats().setOnlyAddBonusXPDoNotSpendStoryPoints(true);
                    Global.getSector().getPlayerStats().spendStoryPoints(1, true, null, true, Settings.FORGET_ELITE_BONUS_XP_FRACTION, null);
                    Global.getSector().getPlayerStats().setOnlyAddBonusXPDoNotSpendStoryPoints(false);
                }
                stats.setSkillLevel(skillId, 0);
                forgotSkills++;
            }
        }
        if (forgotSkills > 0) {
            // Preserve the percentage progress towards the next level
            OfficerLevelupPlugin levelUpPlugin = (OfficerLevelupPlugin) Global.getSettings().getPlugin("officerLevelUp");
            int level = stats.getLevel();
            float fractionToNextLevel = 0f;
            if (level < levelUpPlugin.getMaxLevel(officerData.getPerson())) {
                long xpCur = levelUpPlugin.getXPForLevel(level);
                long xpNext = levelUpPlugin.getXPForLevel(level + 1);
                fractionToNextLevel = (float) (stats.getXP() - xpCur) / (xpNext - xpCur);
            }
            int newLevel = level - forgotSkills;
            long newLevelXP = levelUpPlugin.getXPForLevel(newLevel);
            long newXP = (long) (newLevelXP + fractionToNextLevel * (levelUpPlugin.getXPForLevel(newLevel + 1) - newLevelXP));
            ((CharacterStats) stats).setXP(newXP);
            stats.setLevel(stats.getLevel() - forgotSkills);
            uiElement.recreate();
        }
    }
}
