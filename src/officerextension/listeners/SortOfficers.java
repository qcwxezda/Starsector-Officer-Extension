package officerextension.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.fleet.FleetData;
import com.fs.starfarer.coreui.CaptainPickerDialog;
import officerextension.CoreScript;
import officerextension.Util;
import officerextension.UtilReflection;

import java.lang.reflect.Method;
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

        // Mercenaries first. This is because the base game will float mercenaries up to position 8 (or 10)
        // in the list if they are below that, every time you open the dialog.
        // Unassigned, then assigned, then suspended
        Collections.sort(((FleetData) playerFleet.getFleetData()).getOfficers(),
                new Comparator<OfficerDataAPI>() {
                    @Override
                    public int compare(OfficerDataAPI o1, OfficerDataAPI o2) {
                        boolean m1 = Misc.isMercenary(o1.getPerson());
                        boolean m2 = Misc.isMercenary(o2.getPerson());
                        if (m1 && !m2) {
                            return -1;
                        }
                        if (!m1 && m2) {
                            return 1;
                        }
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
                        Integer e1 = Misc.getNumEliteSkills(o1.getPerson());
                        Integer e2 = Misc.getNumEliteSkills(o2.getPerson());
                        if (!e1.equals(e2)) {
                            return e2.compareTo(e1);
                        }
                        return o1.getPerson().getNameString().compareTo(o2.getPerson().getNameString());
                    }
                }
        );

        dialog.sizeChanged(0f, 0f);
        Object scroller = UtilReflection.invokeGetter(UtilReflection.invokeGetter(dialog, "getListOfficers"), "getScroller");
        try {
            Method setYOffset = scroller.getClass().getDeclaredMethod("setYOffset", float.class);
            setYOffset.invoke(scroller, 0f);
        }
        catch (Exception e) {
            // Fall through
        }
        injector.injectCaptainPickerDialog(dialog);
    }
}
