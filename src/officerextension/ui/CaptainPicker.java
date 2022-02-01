package officerextension.ui;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.coreui.CaptainPickerDialog;
import officerextension.UtilReflection;

import java.lang.reflect.Field;

public class CaptainPicker {

    private static String fleetMemberFieldName;
    private static String officersLabelFieldName;

    public static FleetMemberAPI getFleetMember(CaptainPickerDialog cpd) {
        if (fleetMemberFieldName != null) {
            return (FleetMemberAPI) UtilReflection.getField(cpd, fleetMemberFieldName);
        }
        // Look for a field that's a FleetMemberAPI
        for (Field field : cpd.getClass().getDeclaredFields()) {
            if (FleetMemberAPI.class.isAssignableFrom(field.getType())) {
                fleetMemberFieldName = field.getName();
                return (FleetMemberAPI) UtilReflection.getField(cpd, fleetMemberFieldName);
            }
        }
        // Not found
        throw new RuntimeException("Captain picker dialog's fleet member not found");
    }

    public static LabelAPI getNumOfficersLabel(CaptainPickerDialog cpd) {
        if (officersLabelFieldName != null) {
            return (LabelAPI) UtilReflection.getField(cpd, officersLabelFieldName);
        }
        // Look for a field that's a LabelAPI and that starts with "Officers"
        for (Field field : cpd.getClass().getDeclaredFields()) {
            if (LabelAPI.class.isAssignableFrom(field.getType())) {
                LabelAPI label = (LabelAPI) UtilReflection.getField(cpd, field.getName());
                if (label != null && label.getText().startsWith("Officers:")) {
                    officersLabelFieldName = field.getName();
                    return label;
                }
            }
        }
        // Not found
        throw new RuntimeException("Captain picker dialog's officers: x/y label not found");
    }
}
