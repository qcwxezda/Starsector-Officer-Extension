package officerextension.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import officerextension.CoreScript;
import officerextension.Util;

import java.util.Map;

public class UndoAssignments extends ActionListener {

    private final Map<FleetMemberAPI, PersonAPI> officerMap;
    private final CoreScript injector;

    public UndoAssignments(Map<FleetMemberAPI, PersonAPI> officerMap, CoreScript injector) {
        this.officerMap = officerMap;
        this.injector = injector;
    }

    @Override
    public void trigger(Object... args) {
        boolean failure = false;
        FleetDataAPI fleetData = Global.getSector().getPlayerFleet().getFleetData();
        for (FleetMemberAPI fm : fleetData.getMembersListCopy()) {
            PersonAPI tentativeCaptain = officerMap.get(fm);
            PersonAPI currentCaptain = fm.getCaptain();
            if (tentativeCaptain == currentCaptain) {
                continue;
            }
            // If current captain is unremovable, it has to stay
            if (Misc.isUnremovable(currentCaptain)) {
                failure = true;
                continue;
            }
            // If the tentative captain is unav, clear the current captain
            // to avoid possible duplicate officer issues
            if (Misc.isUnremovable(tentativeCaptain)) {
                fm.setCaptain(Global.getSettings().createPerson());
                failure = true;
                continue;
            }
            OfficerDataAPI officerData = fleetData.getOfficerData(tentativeCaptain);
            // Have to distinguish between officers no longer in the fleet or suspended, vs. unofficered ships
            if (!tentativeCaptain.isDefault()
                    && !tentativeCaptain.isAICore()
                    && !tentativeCaptain.isPlayer()
                    && (!fleetData.getOfficersCopy().contains(officerData)
                        || Util.isSuspended(officerData))) {
                fm.setCaptain(Global.getSettings().createPerson());
                failure = true;
                continue;
            }
            fm.setCaptain(tentativeCaptain);
            if (tentativeCaptain.isPlayer()) {
                fleetData.setFlagship(fm);
            }
        }
        if (failure) {
            Global.getSector().getCampaignUI().getMessageDisplay().addMessage("Could not undo all assignments", Misc.getNegativeHighlightColor());
        }
        injector.updateNumOfficersLabel();
    }
}
