package officerextension;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.impl.campaign.intel.PromoteOfficerIntel;
import officerextension.campaign.ModifiedPromoteOfficerIntel;

import java.util.List;

public class FleetListener extends BaseCampaignEventListener {
    public FleetListener() {
        super(false);
    }

    @Override
    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        if (!battle.isPlayerInvolved()) return;
        IntelManagerAPI manager = Global.getSector().getIntelManager();
        List<IntelInfoPlugin> promotionIntel = manager.getIntel(PromoteOfficerIntel.class);
        if (promotionIntel != null) {
            InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
            for (IntelInfoPlugin intel : promotionIntel) {
                if (!(intel instanceof ModifiedPromoteOfficerIntel)) {
                    manager.removeIntel(intel);
                    manager.addIntel(new ModifiedPromoteOfficerIntel(dialog == null ? null : dialog.getTextPanel()));
                }
            }
        }
    }

    @Override
    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
        SectorEntityToken target = dialog.getInteractionTarget();

        if (!(target instanceof CampaignFleetAPI)) {
            return;
        }

        // Show other side's commander's stats, if that option is enabled
        if (!Settings.SHOW_COMMANDER_SKILLS) return;

        CampaignFleetAPI fleet = (CampaignFleetAPI) target;
        PersonAPI commander;
        if (fleet.getBattle() == null) {
            commander = fleet.getCommander();
        }
        // If interacting fleet is currently in a battle, and
        // the player is allowed to join the battle, show the stats
        // of the enemy fleet.
        else {
            BattleAPI.BattleSide side = fleet.getBattle().pickSide(Global.getSector().getPlayerFleet());
            switch (side) {
                case ONE:
                case TWO:
                    CampaignFleetAPI otherSideCombined = fleet.getBattle().getOtherSideCombined(side);
                    if (otherSideCombined == null) return;
                    commander = otherSideCombined.getCommander();
                    break;
                default:
                    return;
            }
        }

        if (commander == null || commander.isPlayer()) {
            return;
        }

        TextPanelAPI textPanel = dialog.getTextPanel();
        textPanel.setFontSmallInsignia();

        if (Settings.SPLIT_COMMANDER_SKILLS) {
            PersonAPI tempPersonPersonal = Global.getSettings().createPerson();
            PersonAPI tempPersonFleet = Global.getSettings().createPerson();
            tempPersonPersonal.setFaction(commander.getFaction().getId());
            tempPersonFleet.setFaction(commander.getFaction().getId());
            List<SkillLevelAPI> skillLevels = commander.getStats().getSkillsCopy();
            boolean hasPersonal = false, hasFleet = false;
            for (SkillLevelAPI level : skillLevels) {
                SkillSpecAPI skill = level.getSkill();
                if (skill.isCombatOfficerSkill()) {
                    tempPersonPersonal.getStats().setSkillLevel(skill.getId(), level.getLevel());
                    hasPersonal = true;
                }
                else if (skill.isAdmiralSkill()) {
                    tempPersonFleet.getStats().setSkillLevel(skill.getId(), level.getLevel());
                    hasFleet = true;
                }
            }
            String text1 = "The opposing fleet's commander, %s (level %s), possesses the following admiral skills: ";
            String text2 = "The opposing fleet's commander, %s (level %s), possesses the following personal combat skills: ";
            String text3 = "In addition, %s also possesses the following personal combat skills: ";
            if (hasFleet) {
                textPanel.addPara(String.format(text1, commander.getNameString(), commander.getStats().getLevel()));
                textPanel.addSkillPanel(tempPersonFleet, false);
            }
            if (hasPersonal) {
                if (!hasFleet) {
                    textPanel.addPara(String.format(text2, commander.getNameString(), commander.getStats().getLevel()));
                }
                else {
                    textPanel.addPara(String.format(text3, commander.getHeOrShe()));
                }
                textPanel.addSkillPanel(tempPersonPersonal, false);
            }
        }
        else {
            if (commander.getStats().getLevel() >= 1) {
                textPanel.addPara(
                        String.format(
                                "The fleet's commander, %s (level %s), possesses the following personal combat skills: ",
                                commander.getNameString(),
                                commander.getStats().getLevel()));
                textPanel.addSkillPanel(commander, false);
            }
        }
        textPanel.setFontInsignia();
    }
}
