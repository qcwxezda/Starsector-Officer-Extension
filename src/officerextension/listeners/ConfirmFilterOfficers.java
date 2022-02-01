package officerextension.listeners;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.coreui.CaptainPickerDialog;
import officerextension.CoreScript;
import officerextension.filter.OfficerFilter;

import java.util.*;

public class ConfirmFilterOfficers extends DialogDismissedListener {

    private final CaptainPickerDialog dialog;
    private final Map<ButtonAPI, OfficerFilter> buttonMap;
    private final CoreScript injector;

    public ConfirmFilterOfficers(CaptainPickerDialog dialog, Map<ButtonAPI, OfficerFilter> buttonMap, CoreScript injector) {
        this.dialog = dialog;
        this.buttonMap = buttonMap;
        this.injector = injector;
    }

    @Override
    public void trigger(Object... args) {
        // The second argument is 0 if confirmed, 1 if canceled
        int option = (int) args[1];
        if (option == 1) {
            return;
        }

        Set<OfficerFilter> selectedFilters = new HashSet<>();
        for (Map.Entry<ButtonAPI, OfficerFilter> entry : buttonMap.entrySet()) {
            if (entry.getKey().isChecked()) {
                selectedFilters.add(entry.getValue());
            }
        }

        injector.updateActiveFilters(selectedFilters, dialog);
    }
}
