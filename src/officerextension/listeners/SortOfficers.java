package officerextension.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.campaign.fleet.FleetData;
import com.fs.starfarer.coreui.CaptainPickerDialog;
import officerextension.CoreScript;
import officerextension.Util;

import java.util.Collections;
import java.util.Comparator;

public class SortOfficers extends ActionListener {

    private final CaptainPickerDialog dialog;
    private final CoreScript injector;

    public SortOfficers(CaptainPickerDialog dialog, CoreScript injector) {
        this.dialog = dialog;
        this.injector = injector;
    }

    @Override
    public void trigger(Object... args) {

        final CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();

        // Unassigned, then assigned, then suspended
        Collections.sort(((FleetData) playerFleet.getFleetData()).getOfficers(),
                new Comparator<OfficerDataAPI>() {
                    @Override
                    public int compare(OfficerDataAPI o1, OfficerDataAPI o2) {
                        boolean s1 = Util.isSuspended(o1);
                        boolean s2 = Util.isSuspended(o2);
                        if (s1 && !s2) {
                            return 1;
                        }
                        if (!s1 && s2) {
                            return -1;
                        }
                        boolean a1 = Util.isAssigned(o1, playerFleet);
                        boolean a2 = Util.isAssigned(o2, playerFleet);
                        if (a1 && !a2) {
                            return 1;
                        }
                        if (!a1 && a2) {
                            return -1;
                        }
                        Integer l1 = o1.getPerson().getStats().getLevel();
                        Integer l2 = o2.getPerson().getStats().getLevel();
                        if (!l1.equals(l2)) {
                            return l2.compareTo(l1);
                        }
                        return o1.getPerson().getNameString().compareTo(o2.getPerson().getNameString());
                    }
                }
        );

        dialog.sizeChanged(0f, 0f);
        dialog.getListOfficers().getScroller().setYOffset(0f);
        injector.injectCaptainPickerDialog(dialog);
    }
}
