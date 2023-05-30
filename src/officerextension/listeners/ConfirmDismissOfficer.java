package officerextension.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import officerextension.ClassRefs;
import officerextension.UtilReflection;
import officerextension.ui.OfficerUIElement;

import java.lang.reflect.Method;

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
        uiElement.getInjector().updateNumOfficersLabel();
        try {
            Object officerList = UtilReflection.invokeGetter(uiElement.getCaptainPickerDialog(), "getListOfficers");
            Method removeItem = officerList.getClass().getMethod("removeItem", ClassRefs.renderableUIElementInterface);
            removeItem.invoke(officerList, uiElement.getInstance());
            Method collapseEmptySlots = officerList.getClass().getMethod("collapseEmptySlots", boolean.class);
            collapseEmptySlots.invoke(officerList, true);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
