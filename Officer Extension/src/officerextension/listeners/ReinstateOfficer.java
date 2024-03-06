package officerextension.listeners;

import officerextension.UtilReflection;
import officerextension.ui.OfficerUIElement;

public class ReinstateOfficer extends ActionListener {

    private final OfficerUIElement uiElement;

    public ReinstateOfficer(OfficerUIElement uiElement) {
        this.uiElement = uiElement;
    }

    @Override
    public void trigger(Object... args) {
        ConfirmReinstateOfficer confirmListener = new ConfirmReinstateOfficer(uiElement);
        String name = uiElement.getOfficerData().getPerson().getNameString();
        int level = uiElement.getOfficerData().getPerson().getStats().getLevel();
        String str = String.format("Are you sure you want to reinstate %s (level %s)?", name, level);
        UtilReflection.showConfirmationDialog(
                str,
                "Reinstate",
                "Never mind",
                650f,
                100f,
                confirmListener
        );
    }
}
