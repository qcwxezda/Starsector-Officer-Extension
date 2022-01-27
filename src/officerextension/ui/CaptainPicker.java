package officerextension.ui;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.coreui.CaptainPickerDialog;
import officerextension.Util;

import java.lang.reflect.Field;

public class CaptainPicker {

    public static String fleetMemberFieldName = null;

    public static FleetMemberAPI getFleetMember(CaptainPickerDialog cpd) {
        if (fleetMemberFieldName != null) {
            return (FleetMemberAPI) Util.getField(cpd, fleetMemberFieldName);
        }
        // Look for a field that's a FleetMemberAPI
        for (Field field : cpd.getClass().getDeclaredFields()) {
            if (FleetMemberAPI.class.isAssignableFrom(field.getType())) {
                fleetMemberFieldName = field.getName();
                return (FleetMemberAPI) Util.getField(cpd, fleetMemberFieldName);
            }
        }
        // Not found
        throw new RuntimeException("Captain picker dialog's fleet member not found");
    }
}
