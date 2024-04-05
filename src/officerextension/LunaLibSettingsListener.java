package officerextension;

import com.fs.starfarer.api.Global;
import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;

public class LunaLibSettingsListener implements LunaSettingsListener {

    public static void init() {
        LunaLibSettingsListener settingsListener = new LunaLibSettingsListener();
        LunaSettings.addSettingsListener(settingsListener);
        settingsListener.settingsChanged("officerExtension");
    }

    @Override
    public void settingsChanged(String modId) {
        if (!"officerExtension".equals(modId)) return;

        Settings.SUSPENDED_SALARY_FRACTION =
                getFloat("officerExtension", "officerextension_suspendedOfficerMonthlySalaryFraction", 0f);
        Settings.DEMOTE_OFFICER_SP_COST = getInt("officerExtension", "officerextension_demoteOfficerSPCost", 0);
        Settings.DEMOTE_BONUS_XP_FRACTION = getFloat("officerExtension", "officerextension_demoteOfficerBonusXPFraction", 0f);
        Settings.FORGET_ELITE_BONUS_XP_FRACTION =
                getFloat("officerExtension", "officerextension_forgetEliteSkillBonusXPFraction", 0f);
        Settings.SUSPEND_OFFICER_COST_MULTIPLIER =
                getFloat("officerExtension", "officerextension_suspendOfficerCostMultiplier", 0f);
        Settings.SHOW_COMMANDER_SKILLS =
                getBoolean("officerExtension", "officerextension_shouldShowFleetCommanderSkills", false);
        Settings.SPLIT_COMMANDER_SKILLS =
                getBoolean("officerExtension", "officerextension_shouldSplitFleetCommanderSkills", false);
        Settings.PERSISTENT_OFFICER_TAGS.clear();

        String tagsStr = LunaSettings.getString("officerExtension", "officerextension_officerFilterPersistentTags");
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
