package officerextension;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.intel.PromoteOfficerIntel;
import officerextension.campaign.ModifiedPromoteOfficerIntel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FleetListener extends BaseCampaignEventListener implements EveryFrameScript {
    public FleetListener() {
        super(false);
    }

    @Override
    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        if (!battle.isPlayerInvolved()) return;

        // Fix officer promotion intel to always allow promoting
        IntelManagerAPI manager = Global.getSector().getIntelManager();
        List<IntelInfoPlugin> promotionIntel = manager.getIntel(PromoteOfficerIntel.class);
        Map<IntelInfoPlugin, PersonAPI> toRemove = new HashMap<>();
        if (promotionIntel != null) {
            for (IntelInfoPlugin intel : promotionIntel) {
                if (!(intel instanceof ModifiedPromoteOfficerIntel)) {
                    PersonAPI person = (PersonAPI) UtilReflection.getField(intel, "person");
                    toRemove.put(intel, person);
                }
            }
        }
        for (Map.Entry<IntelInfoPlugin, PersonAPI> entry : toRemove.entrySet()) {
            manager.removeIntel(entry.getKey());
            manager.addIntel(new ModifiedPromoteOfficerIntel(entry.getValue(), null), true);
        }

        if (!officerIdsDeployedInLastBattle.isEmpty() && Settings.IDLE_OFFICERS_XP_FRACTION > 0f) {
            Set<String> idsInShips = new HashSet<>();
            var playerFleet = Global.getSector().getPlayerFleet();
            playerFleet.getFleetData().getMembersListCopy().forEach((fm) -> {
                if (fm.getCaptain() != null && !fm.getCaptain().isDefault()) {
                    idsInShips.add(fm.getCaptain().getId());
                }
            });
            var filtered = playerFleet.getFleetData().getOfficersCopy().stream().filter(x -> !idsInShips.contains(x.getPerson().getId())).toList();
            for (var officer : filtered) {
                TextPanelAPI panel = null;
                var dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
                if (dialog != null) {
                    panel = dialog.getTextPanel();
                }
                officer.addXP((long) (Settings.IDLE_OFFICERS_XP_FRACTION * lastBattleXPGain) / (officerIdsDeployedInLastBattle.size() + filtered.size()), panel);
            }
        }

        lastBattleXPGain = 0f;
        officerIdsDeployedInLastBattle.clear();
    }

    private float lastBattleXPGain = 0f;
    private final Set<String> officerIdsDeployedInLastBattle = new HashSet<>();
    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        EngagementResultForFleetAPI playerResult = result.getLoserResult().isPlayer() ? result.getLoserResult() : result.getWinnerResult();
        EngagementResultForFleetAPI enemyResult = result.getLoserResult().isPlayer() ? result.getWinnerResult() : result.getLoserResult();
        if (enemyResult.getDestroyed().isEmpty() && enemyResult.getDisabled().isEmpty()) return;

        InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        if (dialog != null) {
            InteractionDialogPlugin plugin = dialog.getPlugin();
            if (plugin != null && plugin.getContext() instanceof FleetEncounterContextPlugin context) {
                float xpGained = 0f;
                var destroyed = new ArrayList<>(enemyResult.getDestroyed());
                destroyed.addAll(enemyResult.getDisabled());
                xpGained += (float) destroyed.stream().mapToDouble(fm -> 250f * fm.getFleetPointCost() * (1f + fm.getCaptain().getStats().getLevel() / 5f)).sum();

                float difficulty = (context instanceof FleetEncounterContext fContext) ? fContext.getDifficulty() : 1f;
                xpGained *= 2f * Math.max(1f, difficulty) * context.computePlayerContribFraction();
                xpGained *= Global.getSettings().getFloat("xpGainMult");
                lastBattleXPGain += (long) xpGained;
                // pursuit, no deployed data
                if (playerResult.getAllEverDeployedCopy() == null) {
                    return;
                }

                Set<FleetMemberAPI> playerFleetMembers = new HashSet<>(Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy());
                for (DeployedFleetMemberAPI dfm : playerResult.getAllEverDeployedCopy()) {
                    FleetMemberAPI fm = dfm.getMember();
                    if (dfm.isFighterWing() || fm == null || !playerFleetMembers.contains(fm) || dfm.getShip() == null) continue;
                    if (fm.getCaptain() == null | fm.getCaptain().isDefault() || fm.getCaptain().isAICore() || fm.getCaptain().isPlayer()) continue;
                    officerIdsDeployedInLastBattle.add(fm.getCaptain().getId());
                }
            }
        }
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    private CampaignFleetAPI lastSeenFleet;
    @Override
    public void advance(float amount) {
        if (!Settings.SHOW_COMMANDER_SKILLS) return;
        var dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        if (dialog == null) return;
        var target = dialog.getInteractionTarget();
        if (!(target instanceof CampaignFleetAPI fleet)) return;
        if (fleet != lastSeenFleet) {
            showCommanderSkills(dialog);
        }
        lastSeenFleet = fleet;
    }

    private void showCommanderSkills(InteractionDialogAPI dialog) {
        SectorEntityToken target = dialog.getInteractionTarget();

        if (!(target instanceof CampaignFleetAPI fleet)) {
            return;
        }
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
