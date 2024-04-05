package officerextension;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.SleeperPodsSpecial;

import java.util.HashSet;
import java.util.Set;

public class ExceptionalOfficerChecker extends BaseCampaignEventListener {

    public static final String SEEN_EXCEPTIONAL_OFFICERS_KEY = "$officerextension_SeenExceptionalOfficers";

    public ExceptionalOfficerChecker() {
        super(false);
    }

    @Override
    public void reportEconomyTick(int iterIndex) {
        Set<String> seenExceptionalIds = new HashSet<>();
        for (OfficerDataAPI data : Global.getSector().getPlayerFleet().getFleetData().getOfficersCopy()) {
            if (data.getPerson().getMemoryWithoutUpdate().getBoolean(MemFlags.EXCEPTIONAL_SLEEPER_POD_OFFICER)) {
                seenExceptionalIds.add(data.getPerson().getId());
            }
        }

        //noinspection unchecked
        Set<String> saveSeen = (Set<String>) Global.getSector().getPersistentData().get(SEEN_EXCEPTIONAL_OFFICERS_KEY);
        if (saveSeen == null) {
            saveSeen = seenExceptionalIds;
            Global.getSector().getPersistentData().put(SEEN_EXCEPTIONAL_OFFICERS_KEY, saveSeen);
        } else {
            saveSeen.addAll(seenExceptionalIds);
        }
    }

    @Override
    public void reportEconomyMonthEnd() {
        //noinspection unchecked
        Set<String> saveSeen = (Set<String>) Global.getSector().getPersistentData().get(SEEN_EXCEPTIONAL_OFFICERS_KEY);
        Set<String> allSeen = new HashSet<>();
        if (saveSeen != null) allSeen.addAll(saveSeen);

        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            for (SectorEntityToken entity : system.getAllEntities()) {
                String id;
                if ((id = getExceptionalOfficerId(entity)) != null) {
                    allSeen.add(id);
                }
            }
        }

        Global.getSector().getMemoryWithoutUpdate().set("$SleeperPodsSpecialCreator_exceptionalCount", allSeen.size());
    }

    public static String getExceptionalOfficerId(SectorEntityToken entity) {
        MemoryAPI memory = entity.getMemoryWithoutUpdate();
        if (memory == null || !memory.contains(MemFlags.SALVAGE_SPECIAL_DATA)) {
            return null;
        }
        Object o = memory.get(MemFlags.SALVAGE_SPECIAL_DATA);
        if (!(o instanceof SleeperPodsSpecial.SleeperPodsSpecialData)) return null;
        SleeperPodsSpecial.SleeperPodsSpecialData data =
                (SleeperPodsSpecial.SleeperPodsSpecialData) o;
        if (data.officer == null) return null;
        MemoryAPI officerMemory = data.officer.getMemoryWithoutUpdate();
        return officerMemory != null && officerMemory.getBoolean(MemFlags.EXCEPTIONAL_SLEEPER_POD_OFFICER) ? data.officer.getId() : null;
    }
}
