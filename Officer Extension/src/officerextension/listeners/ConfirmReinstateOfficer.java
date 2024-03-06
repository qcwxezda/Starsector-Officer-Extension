package officerextension.listeners;

import com.fs.starfarer.api.characters.OfficerDataAPI;
import officerextension.Util;
import officerextension.ui.OfficerUIElement;

public class ConfirmReinstateOfficer extends DialogDismissedListener {

    private final OfficerUIElement uiElement;
    private final OfficerDataAPI officerData;

    public ConfirmReinstateOfficer(OfficerUIElement uiElement) {
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
        // Global.getSector().getPlayerFleet().getFleetData().addOfficer(officerData);
        Util.reinstate(officerData);
        uiElement.recreate();
    }
}
