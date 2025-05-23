package officerextension;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.util.Misc;
import officerextension.filter.PersonalityFilter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;
import java.util.List;

public class Util {

    public static final List<PersonalityFilter> personalityFilters = new ArrayList<>();
    /** Map from skill aptitudes to their elite icons */
    public static final Map<String, String> eliteIcons = new HashMap<>();

    static {
        personalityFilters.add(new PersonalityFilter("timid", "Timid"));
        personalityFilters.add(new PersonalityFilter("cautious", "Cautious"));
        personalityFilters.add(new PersonalityFilter("steady", "Steady"));
        personalityFilters.add(new PersonalityFilter("aggressive", "Aggressive"));
        personalityFilters.add(new PersonalityFilter("reckless", "Reckless"));

        try {
            JSONArray array = Global.getSettings().getMergedSpreadsheetData("id", "data/characters/skills/aptitude_data.csv");
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = array.getJSONObject(i);
                eliteIcons.put(json.getString("id"), json.optString("elite_overlay", null));
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not load data/characters/skills/aptitude_data.csv from the base game");
        }
    }

    public static void updateOfficerTags(OfficerDataAPI officer, Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            officer.getPerson().getMemoryWithoutUpdate().unset(Settings.OFFICER_TAGS_DATA_KEY);
            return;
        }
        // Remove tags containing only whitespace
        tags.removeIf(s -> s.matches("\\s*"));
        officer.getPerson().getMemoryWithoutUpdate().set(Settings.OFFICER_TAGS_DATA_KEY, tags);
    }

    @SuppressWarnings("unchecked")
    public static Set<String> getOfficerTags(OfficerDataAPI officer) {
        Object tags = officer.getPerson().getMemoryWithoutUpdate().get(Settings.OFFICER_TAGS_DATA_KEY);
        return tags == null ? new HashSet<>() : (Set<String>) tags;
    }

    public static boolean hasTag(OfficerDataAPI officer, String tag) {
        return getOfficerTags(officer).contains(tag);
    }

    public static Set<String> getAllTags() {
        Set<String> allTags = new TreeSet<>((s1, s2) -> {
            boolean b1 = Settings.PERSISTENT_OFFICER_TAGS.contains(s1);
            boolean b2 = Settings.PERSISTENT_OFFICER_TAGS.contains(s2);
            if (b1 && !b2) {
                return -1;
            }
            if (!b1 && b2) {
                return 1;
            }
            return s1.compareTo(s2);
        });
        allTags.addAll(Settings.PERSISTENT_OFFICER_TAGS);
        for (OfficerDataAPI officer : Global.getSector().getPlayerFleet().getFleetData().getOfficersCopy()) {
            allTags.addAll(getOfficerTags(officer));
        }
        return allTags;
    }

    public static List<OfficerDataAPI> getSuspendedOfficers() {
        List<OfficerDataAPI> suspended = new ArrayList<>();
        for (OfficerDataAPI officer : Global.getSector().getPlayerFleet().getFleetData().getOfficersCopy()) {
            if (isSuspended(officer)) {
                suspended.add(officer);
            }
        }
        return suspended;
    }

    public static boolean isSuspended(OfficerDataAPI officer) {
        // Safety check -- if [officer] is a mercenary, remove the tag and return [false]
        // Can happen if the player reassigns an officer skill and selects to convert to mercenary
        if (Misc.isMercenary(officer.getPerson())) {
            officer.getPerson().removeTag(Settings.OFFICER_IS_SUSPENDED_KEY);
            return false;
        }
        return officer.getPerson().hasTag(Settings.OFFICER_IS_SUSPENDED_KEY);
    }

    public static void suspend(OfficerDataAPI officer) {
        officer.getPerson().addTag(Settings.OFFICER_IS_SUSPENDED_KEY);
    }

    public static void reinstate(OfficerDataAPI officer) {
        officer.getPerson().removeTag(Settings.OFFICER_IS_SUSPENDED_KEY);
    }

    public static int countAssignedNonMercOfficers(CampaignFleetAPI fleet) {
        int cnt = 0;
        for (OfficerDataAPI officer : fleet.getFleetData().getOfficersCopy()) {
            if (fleet.getFleetData().getMemberWithCaptain(officer.getPerson()) != null && !Misc.isMercenary(officer.getPerson())) {
                cnt++;
            }
        }
        return cnt;
    }

    public static int countIdleOfficers(CampaignFleetAPI fleet) {
        int cnt = 0;
        for (OfficerDataAPI officer : fleet.getFleetData().getOfficersCopy()) {
            if (isSuspended(officer)) {
                continue;
            }
            if (fleet.getFleetData().getMemberWithCaptain(officer.getPerson()) == null) {
                cnt++;
            }
        }
        return cnt;
    }

    public static boolean isAssigned(OfficerDataAPI officer, CampaignFleetAPI fleet) {
        return fleet.getFleetData().getMemberWithCaptain(officer.getPerson()) != null;
    }

    public static int getMaxPlayerOfficers() {
        MutableStat officerNumber = Global.getSector().getPlayerPerson().getStats().getOfficerNumber();
        MutableStat.StatMod mod = officerNumber.getFlatStatMod(DialogHandler.officerNumberId);
        // Don't count the temporary nonsense we added in the dialog handler
        officerNumber.unmodify(DialogHandler.officerNumberId);
        int num = Misc.getMaxOfficers(Global.getSector().getPlayerFleet());
        if (mod != null) {
            officerNumber.modifyFlat(DialogHandler.officerNumberId, mod.value);
        }
        return num;
    }

    public static <T> T instantiateClassNoParams(Class<T> cls) throws NoSuchMethodException, IllegalAccessException {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle mh = lookup.findConstructor(cls, MethodType.methodType(void.class));
        try {
            //noinspection unchecked
            return (T) mh.invoke();
        }
        catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int countEliteSkills(OfficerDataAPI data) {
        int count = 0;
        for (MutableCharacterStatsAPI.SkillLevelAPI level : data.getPerson().getStats().getSkillsCopy()) {
            if (level.getLevel() > 1f) {
                count++;
            }
        }
        return count;
    }
}