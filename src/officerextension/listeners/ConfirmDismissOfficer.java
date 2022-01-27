package officerextension.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import officerextension.Util;
import officerextension.ui.OfficerUIElement;

public class ConfirmDismissOfficer extends DialogDismissedListener {
    private final OfficerUIElement uiElement;
    private final OfficerDataAPI officerData;

    public ConfirmDismissOfficer(OfficerUIElement uiElement) {
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
        FleetDataAPI playerFleetData = Global.getSector().getPlayerFleet().getFleetData();
        FleetMemberAPI fleetMember = playerFleetData.getMemberWithCaptain(officerData.getPerson());
        if (fleetMember != null) {
            fleetMember.setCaptain(null);
        }
        playerFleetData.removeOfficer(officerData.getPerson());
        float scrollPosition = uiElement.getCaptainPickerDialog().getListOfficers().getScroller().getYOffset();
        uiElement.getCaptainPickerDialog().sizeChanged(0f, 0f);
        uiElement.getCaptainPickerDialog().getListOfficers().getScroller().setYOffset(scrollPosition);
        Util.removeSuspendedOfficer(officerData);
    }
}
