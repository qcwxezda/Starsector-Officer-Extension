package officerextension.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CombatDamageData;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import officerextension.Settings;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModifiedFleetEncounterContext extends FleetEncounterContext {

    public ModifiedFleetEncounterContext(
            List<DataForEncounterSide> sideData,
            boolean engagedInHostilities,
            boolean engagedInActualBattle,
            boolean playerOnlyRetreated,
            boolean playerPursued,
            boolean playerDidSeriousDamage,
            BattleAPI battle,
            boolean otherFleetHarriedPlayer,
            boolean ongoingBattle,
            boolean isAutoresolve,
            CombatDamageData runningDamageTotal) {
        this.sideData = sideData;
        this.engagedInHostilities = engagedInHostilities;
        this.engagedInActualBattle = engagedInActualBattle;
        this.playerOnlyRetreated = playerOnlyRetreated;
        this.playerPursued = playerPursued;
        this.playerDidSeriousDamage = playerDidSeriousDamage;
        this.otherFleetHarriedPlayer = otherFleetHarriedPlayer;
        this.battle = battle;
        this.ongoingBattle = ongoingBattle;
        this.isAutoresolve = isAutoresolve;
        this.runningDamageTotal = runningDamageTotal;
    }

    @Override
    public float computeBattleDifficulty() {
        if (computedDifficulty) return difficulty;

        computedDifficulty = true;
        if (battle == null || !battle.isPlayerInvolved()) {
            difficulty = 1f;
            return difficulty;
        }

        float scorePlayer = 0f;
        float scoreEnemy = 0f;

        float officerBase = 30;
        float officerPerLevel = 15;
        //float baseMult = 0.2f;
        float baseMult = 2f;
        float dModMult = 0.9f;

        for (FleetMemberAPI member : battle.getNonPlayerCombined().getFleetData().getMembersListCopy()) {
            if (member.isMothballed()) continue;
            float mult = baseMult;
            if (member.isStation()) mult *= 2f;
            else if (member.isCivilian()) mult *= 0.25f;
            if (member.getCaptain() != null && !member.getCaptain().isDefault()) {
                scoreEnemy += officerBase + officerPerLevel * Math.max(1f, member.getCaptain().getStats().getLevel());
            }
            int dMods = DModManager.getNumDMods(member.getVariant());
            for (int i = 0; i < dMods; i++) {
                mult *= dModMult;
            }
            scoreEnemy += member.getUnmodifiedDeploymentPointsCost() * mult;
        }
        scoreEnemy *= 0.67f;

        float maxPlayserShipScore = 0f;

        officerBase *= 0.5f;
        officerPerLevel *= 0.5f;
        Set<PersonAPI> seenOfficers = new HashSet<>();
        int unofficeredShips = 0;
        for (FleetMemberAPI member : battle.getPlayerCombined().getFleetData().getMembersListCopy()) {
            if (member.isMothballed()) continue;
            float mult = baseMult;
            if (member.isStation()) mult *= 2f;
            else if (member.isCivilian()) mult *= 0.25f;
            if (member.getCaptain() != null && !member.getCaptain().isDefault()) {
                scorePlayer += officerBase + officerPerLevel * Math.max(1f, member.getCaptain().getStats().getLevel());
                seenOfficers.add(member.getCaptain());
            } else if (!member.isCivilian()) {
                unofficeredShips++;
            }
            int dMods = DModManager.getNumDMods(member.getVariant());
            for (int i = 0; i < dMods; i++) {
                mult *= dModMult;
            }
            float currShipBaseScore = member.getUnmodifiedDeploymentPointsCost() * mult;
            scorePlayer += currShipBaseScore;
            if (battle.getSourceFleet(member) != null && battle.getSourceFleet(member).isPlayerFleet()) {
                maxPlayserShipScore = Math.max(maxPlayserShipScore, currShipBaseScore);
            }
        }

        // so that removing officers from ships prior to a fight doesn't increase the XP gained
        // otherwise would usually be optimal to do this prior to every fight for any officers
        // on ships that aren't expected to be deployed
        for (OfficerDataAPI od : Global.getSector().getPlayerFleet().getFleetData().getOfficersCopy()) {
            if (seenOfficers.contains(od.getPerson())) continue;
            if (od.getPerson().isPlayer()) continue;
            if (od.getPerson().hasTag(Settings.OFFICER_IS_SUSPENDED_KEY)) continue;
            if (unofficeredShips <= 0) break;
            unofficeredShips--;
            scorePlayer += officerBase + officerPerLevel * Math.max(1f, od.getPerson().getStats().getLevel());
        }

        scorePlayer = Math.max(scorePlayer, Math.min(scoreEnemy * 0.5f, maxPlayserShipScore * 6f));

        if (scorePlayer < 1) scorePlayer = 1;
        if (scoreEnemy < 1) scoreEnemy = 1;

        difficulty = scoreEnemy / scorePlayer;
        if (difficulty < 0) difficulty = 0;
        if (difficulty > MAX_XP_MULT) difficulty = MAX_XP_MULT;
        return difficulty;
    }
}
