package officerextension.listeners;

import com.fs.starfarer.coreui.CaptainPickerDialog;
import officerextension.CoreScript;
import officerextension.filter.OfficerFilter;

import java.util.HashSet;

public class ClearFilters extends ActionListener {

    private final CaptainPickerDialog dialog;
    private final CoreScript injector;

    public ClearFilters(CaptainPickerDialog dialog, CoreScript injector) {
        this.dialog = dialog;
        this.injector = injector;
    }

    @Override
    public void trigger(Object... args) {
        injector.updateActiveFilters(new HashSet<OfficerFilter>(), dialog);
    }
}
