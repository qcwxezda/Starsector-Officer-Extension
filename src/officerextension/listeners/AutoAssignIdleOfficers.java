package officerextension.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.campaign.fleet.FleetData;
import officerextension.DialogHandler;
import officerextension.Util;
import officerextension.ui.Button;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AutoAssignIdleOfficers extends ActionListener {

    private final Object originalListener;
    private final Button originalButton;

    public AutoAssignIdleOfficers(Button originalButton) {
        this.originalButton = originalButton;
        this.originalListener = originalButton.getListener();
    }
    @Override
    public void trigger(Object... args) {
        // We will temporarily remove suspended officers from the player's fleet,
        // then sort in the following order: assigned officers, unassigned officers
        // officers in the same category will be sorted by level
        // After we finish the call to the original listener, we will set the player's officers
        // back to the original list.

        final CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        FleetData fleetData = (FleetData) playerFleet.getFleetData();
        List<OfficerDataAPI> officersCopy = fleetData.getOfficersCopy();

        for (OfficerDataAPI officer : officersCopy) {
            if (Util.isSuspended(officer)) {
                fleetData.removeOfficer(officer.getPerson());
            }
        }

        // If inside a dialog, reduce player's max officers by the temporary increase
        int increase = 0;
        MutableStat stat = Global.getSector().getPlayerPerson().getStats().getOfficerNumber();
        MutableStat.StatMod mod = stat.getFlatStatMod(
                DialogHandler.officerNumberId);
        if (mod != null) {
            increase = (int) mod.value;
            stat.unmodify(DialogHandler.officerNumberId);
        }

        Collections.sort(fleetData.getOfficers(),
                new Comparator<OfficerDataAPI>() {
                    @Override
                    public int compare(OfficerDataAPI o1, OfficerDataAPI o2) {
                        boolean a1 = Util.isAssigned(o1, playerFleet);
                        boolean a2 = Util.isAssigned(o2, playerFleet);
                        if (a1 && !a2) {
                            return -1;
                        }
                        if (!a1 && a2) {
                            return 1;
                        }
                        Integer l1 = o1.getPerson().getStats().getLevel();
                        Integer l2 = o2.getPerson().getStats().getLevel();
                        return l2.compareTo(l1);
                    }
                }
            );

        try {
            Method origActionPerformed = originalListener.getClass().getMethod("actionPerformed", Object.class, Object.class);
            origActionPerformed.invoke(originalListener, args[0], originalButton.getInstance());

            // Add back the suspended officers -- need to set their fleet pointer back to the player's fleet
            for (OfficerDataAPI officer : officersCopy) {
                if (Util.isSuspended(officer)) {
                    fleetData.addOfficer(officer);
                }
            }

            // Now it's safe to set the officer field to officersCopy
            Field officersField = fleetData.getClass().getDeclaredField("officers");
            officersField.setAccessible(true);
            officersField.set(fleetData, officersCopy);

            // Add back the temporary increase, if needed
            if (increase > 0) {
                stat.modifyFlat(DialogHandler.officerNumberId, increase);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
