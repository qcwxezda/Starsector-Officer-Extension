package officerextension.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.MutableValue;
import officerextension.Settings;
import officerextension.Util;
import officerextension.ui.OfficerUIElement;

public class ConfirmSuspendOfficer extends DialogDismissedListener {

    private final OfficerUIElement uiElement;
    private final OfficerDataAPI officerData;

    public ConfirmSuspendOfficer(OfficerUIElement uiElement) {
        this.uiElement = uiElement;
        this.officerData = uiElement.getOfficerData();
    }

    @Override
    public void trigger(Object... args) {
        // The second argument is 0 if confirmed, 1 if canceled
        int option = (int) args[1];
        if (option == 1) {
            return;
        }
        // Have to do the check again here, since the player can press space bar to confirm despite
        // the confirm button being disabled
        MutableValue credits = Global.getSector().getPlayerFleet().getCargo().getCredits();
        float suspendCost = Settings.SUSPEND_OFFICER_COST_MULTIPLIER * Misc.getOfficerSalary(officerData.getPerson());
        if (credits.get() < suspendCost) {
            return;
        }
        credits.subtract(suspendCost);
        Misc.addCreditsMessage("Spent %s", (int) suspendCost);
        FleetDataAPI playerFleetData = Global.getSector().getPlayerFleet().getFleetData();
        FleetMemberAPI fleetMember = playerFleetData.getMemberWithCaptain(officerData.getPerson());
        if (fleetMember != null) {
            // Default person is the commander for unofficered ships -- commander is never null
            fleetMember.setCaptain(Global.getSettings().createPerson());
        }
        //playerFleetData.removeOfficer(officerData.getPerson());
        Util.suspend(officerData);
        uiElement.recreate();
    }
}
