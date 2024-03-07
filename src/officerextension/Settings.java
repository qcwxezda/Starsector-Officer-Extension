package officerextension;

import com.fs.starfarer.api.Global;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;
import java.util.TreeSet;

public class Settings {

    public static float SUSPENDED_SALARY_FRACTION;
    public static int DEMOTE_OFFICER_SP_COST;
    public static float DEMOTE_BONUS_XP_FRACTION;
    public static float FORGET_ELITE_BONUS_XP_FRACTION;
    public static float SUSPEND_OFFICER_COST_MULTIPLIER;
    public static boolean SHOW_COMMANDER_SKILLS;
    public static boolean SPLIT_COMMANDER_SKILLS;
    public final static Set<String> PERSISTENT_OFFICER_TAGS = new TreeSet<>();

    /** Unused -- only used pre 0.4.0 */
    public static final String SUSPENDED_OFFICERS_DATA_KEY = "officerextension_SuspendedOfficers";

    public static final String OFFICER_IS_SUSPENDED_KEY = "$officerextension_IsSuspended";
    public static final String SUSPENDED_OFFICERS_NODE = "node_id_suspended_officers";
    public static final String OFFICER_TAGS_DATA_KEY = "$officerextension_OfficerTags";
    public static final String SKILL_TAG_UNREMOVABLE = "officerextension_unremovable";

    public static void load() {
        try {
            JSONObject json = Global.getSettings().loadJSON("officerextension_settings.json");
            SUSPENDED_SALARY_FRACTION = (float) json.getDouble("suspendedOfficerMonthlySalaryFraction");
            DEMOTE_OFFICER_SP_COST = json.getInt("demoteOfficerSPCost");
            DEMOTE_BONUS_XP_FRACTION = (float) json.getDouble("demoteOfficerBonusXPFraction");
            FORGET_ELITE_BONUS_XP_FRACTION = (float) json.getDouble("forgetEliteSkillBonusXPFraction");
            SUSPEND_OFFICER_COST_MULTIPLIER = (float) json.getDouble("suspendOfficerCostMultiplier");
            SHOW_COMMANDER_SKILLS = json.getBoolean("shouldShowFleetCommanderSkills");
            SPLIT_COMMANDER_SKILLS = json.getBoolean("shouldSplitFleetCommanderSkills");
            PERSISTENT_OFFICER_TAGS.clear();
            JSONArray persistentTags = json.getJSONArray("officerFilterPersistentTags");
            for (int i = 0 ; i < persistentTags.length(); i++) {
                PERSISTENT_OFFICER_TAGS.add(persistentTags.getString(i));
            }
        }
        catch (Exception e) {
            Global.getLogger(Settings.class).error("Failure to load \"Officer Extension/officerextension_settings.json\"", e);
        }
    }

}
