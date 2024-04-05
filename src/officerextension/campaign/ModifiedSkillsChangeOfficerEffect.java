package officerextension.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.SkillsChangeOfficerEffect;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import officerextension.Util;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ModifiedSkillsChangeOfficerEffect extends SkillsChangeOfficerEffect {
    @Override
    public OfficerDataMap getEffects(MutableCharacterStatsAPI from, MutableCharacterStatsAPI to) {
        to.getOfficerNumber().modifyFlat("officerextension_temp", 10000000);
        OfficerDataMap effects = super.getEffects(from, to);
        to.getOfficerNumber().unmodify("officerextension_temp");
        return effects;
    }

    @Override
    public boolean hasEffects(MutableCharacterStatsAPI from, MutableCharacterStatsAPI to) {
        int cur = Util.countAssignedNonMercOfficers(Global.getSector().getPlayerFleet());
        int max = Util.getMaxPlayerOfficers();
        return super.hasEffects(from, to) || cur > max;
    }

    @Override
    public void applyEffects(MutableCharacterStatsAPI from, MutableCharacterStatsAPI to, Map<String, Object> dataMap) {
        super.applyEffects(from, to, dataMap);
        // Just un-assign officers over the new limit, no need to make mercenaries or whatever
        int cur = Util.countAssignedNonMercOfficers(Global.getSector().getPlayerFleet());
        int max = Util.getMaxPlayerOfficers();
        if (cur > max) {
            List<OfficerDataAPI> officers = Global.getSector().getPlayerFleet().getFleetData().getOfficersCopy();
            Collections.sort(officers, new Comparator<OfficerDataAPI>() {
                @Override
                public int compare(OfficerDataAPI o1, OfficerDataAPI o2) {
                    int levelDiff = o1.getPerson().getStats().getLevel() - o2.getPerson().getStats().getLevel();
                    if (levelDiff != 0) return levelDiff;
                    return Util.countEliteSkills(o1) - Util.countEliteSkills(o2);
                }
            });
            int removed = 0;
            for (OfficerDataAPI data : officers) {
                FleetMemberAPI member;
                if ((member = Global.getSector().getPlayerFleet().getFleetData().getMemberWithCaptain(data.getPerson())) != null) {
                    member.setCaptain(Global.getFactory().createPerson());
                    removed++;
                    if (removed >= cur - max) break;
                }
            }
            if (removed > 0) {
                Global.getSector().getCampaignUI().getMessageDisplay().addMessage(String.format(removed == 1 ? "Unassigned %s officer" : "Unassigned %s officers", removed),
                                                              Misc.getNegativeHighlightColor());
            }
        }
    }
}
