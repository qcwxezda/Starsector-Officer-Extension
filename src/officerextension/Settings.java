package officerextension;

import com.fs.starfarer.api.Global;
import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
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

    public static class SettingsListener implements LunaSettingsListener {
        @Override
        public void settingsChanged(String modId) {
            if (!"officerExtension".equals(modId)) return;

            SUSPENDED_SALARY_FRACTION = getFloat("officerExtension", "general_suspendedOfficerMonthlySalaryFraction", 0f);
            DEMOTE_OFFICER_SP_COST = getInt("officerExtension", "general_demoteOfficerSPCost", 0);
            DEMOTE_BONUS_XP_FRACTION = getFloat("officerExtension", "general_demoteOfficerBonusXPFraction", 0f);
            FORGET_ELITE_BONUS_XP_FRACTION = getFloat("officerExtension", "general_forgetEliteSkillBonusXPFraction", 0f);
            SUSPEND_OFFICER_COST_MULTIPLIER = getFloat("officerExtension", "general_suspendOfficerCostMultiplier", 0f);
            SHOW_COMMANDER_SKILLS = getBoolean("officerExtension", "general_shouldShowFleetCommanderSkills", false);
            SPLIT_COMMANDER_SKILLS = getBoolean("officerExtension", "general_shouldSplitFleetCommanderSkills", false);
            PERSISTENT_OFFICER_TAGS.clear();

            String tagsStr = LunaSettings.getString("officerExtension", "general_officerFilterPersistentTags");
            if (tagsStr == null || tagsStr.trim().equals("")) {
                try {
                    JSONObject json = Global.getSettings().loadJSON("officerextension_settings.json");
                    JSONArray persistentTags = json.getJSONArray("officerFilterPersistentTags");
                    for (int i = 0; i < persistentTags.length(); i++) {
                        PERSISTENT_OFFICER_TAGS.add(persistentTags.getString(i));
                    }
                }
                catch (Exception e) {
                    Global.getLogger(Settings.class).error("Failure to load \"Officer Extension/officerextension_settings.json\"", e);
                }
                return;
            }
            String[] strs = tagsStr.trim().split("\\s*,\\s*");
            PERSISTENT_OFFICER_TAGS.addAll(Arrays.asList(strs));
        }

        public float getFloat(String modId, String fieldId, float ifNull) {
            Float f = LunaSettings.getFloat(modId, fieldId);
            return f == null ? ifNull : f;
        }

        public int getInt(String modId, String fieldId, int ifNull) {
            Integer i = LunaSettings.getInt(modId, fieldId);
            return i == null ? ifNull : i;
        }

        public boolean getBoolean(String modId, String fieldId, boolean ifNull) {
            Boolean b = LunaSettings.getBoolean(modId, fieldId);
            return b == null ? ifNull : b;
        }
    }
}
