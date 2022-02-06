package officerextension.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.OfficerLevelupPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;

public class CryopodAwareOfficerLevelUpPlugin extends OfficerLevelupPluginImpl {
    @Override
    public int getMaxLevel(PersonAPI person) {
        if (person != null && person.getMemoryWithoutUpdate().getBoolean(MemFlags.EXCEPTIONAL_SLEEPER_POD_OFFICER)) {
            return Global.getSettings().getInt("exceptionalSleeperPodsOfficerLevel");
        }
        return super.getMaxLevel(person);
    }

    @Override
    public int getMaxEliteSkills(PersonAPI person) {
        if (person != null && person.getMemoryWithoutUpdate().getBoolean(MemFlags.EXCEPTIONAL_SLEEPER_POD_OFFICER)) {
            return Global.getSettings().getInt("exceptionalSleeperPodsOfficerEliteSkills");
        }
        return super.getMaxEliteSkills(person);
    }
}
