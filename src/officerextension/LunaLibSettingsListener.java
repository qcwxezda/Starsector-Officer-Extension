package officerextension;

import com.fs.starfarer.api.Global;
import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;

public class LunaLibSettingsListener implements LunaSettingsListener {
    
    public static final String id = "officerExtension";

    public static void init() {
        LunaLibSettingsListener settingsListener = new LunaLibSettingsListener();
        LunaSettings.addSettingsListener(settingsListener);
        settingsListener.settingsChanged(id);
    }

    @Override
    public void settingsChanged(String modId) {
        if (!id.equals(modId)) return;

        Settings.SUSPENDED_SALARY_FRACTION =
                getFloat(id, "officerextension_suspendedOfficerMonthlySalaryFraction", 0f);
        Settings.DEMOTE_OFFICER_SP_COST = getInt(id, "officerextension_demoteOfficerSPCost", 0);
        Settings.DEMOTE_BONUS_XP_FRACTION = getFloat(id, "officerextension_demoteOfficerBonusXPFraction", 0f);
        Settings.FORGET_ELITE_BONUS_XP_FRACTION =
                getFloat(id, "officerextension_forgetEliteSkillBonusXPFraction", 0f);
        Settings.SUSPEND_OFFICER_COST_MULTIPLIER =
                getFloat(id, "officerextension_suspendOfficerCostMultiplier", 0f);
        Settings.IDLE_OFFICERS_XP_FRACTION = getFloat(id, "officerextension_idleOfficersXPFraction", 0f);
        Settings.SKILL_CHOICES_NOT_MENTORED = getInt(id, "officerextension_skillChoicesNotMentored", 0);
        Settings.SKILL_CHOICES_MENTORED = getInt(id, "officerextension_skillChoicesMentored", 0);
        Settings.SHOW_COMMANDER_SKILLS =
                getBoolean(id, "officerextension_shouldShowFleetCommanderSkills", false);
        Settings.SPLIT_COMMANDER_SKILLS =
                getBoolean(id, "officerextension_shouldSplitFleetCommanderSkills", false);
        Settings.PERSISTENT_OFFICER_TAGS.clear();

        String tagsStr = LunaSettings.getString(id, "officerextension_officerFilterPersistentTags");
        if (tagsStr == null || tagsStr.trim().isEmpty()) {
            try {
                JSONObject json = Global.getSettings().loadJSON("officerextension_settings.json");
                JSONArray persistentTags = json.getJSONArray("officerFilterPersistentTags");
                for (int i = 0; i < persistentTags.length(); i++) {
                    Settings.PERSISTENT_OFFICER_TAGS.add(persistentTags.getString(i));
                }
            } catch (Exception e) {
                Global.getLogger(Settings.class)
                      .error("Failure to load \"Officer Extension/officerextension_settings.json\"", e);
            }
            return;
        }
        String[] strs = tagsStr.trim().split("\\s*,\\s*");
        Settings.PERSISTENT_OFFICER_TAGS.addAll(Arrays.asList(strs));
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
