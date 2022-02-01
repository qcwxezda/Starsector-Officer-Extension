package officerextension.listeners;

import officerextension.UtilReflection;
import officerextension.ui.OfficerUIElement;

public class DismissOfficer extends ActionListener {

    private final OfficerUIElement uiElement;

    public DismissOfficer(OfficerUIElement uiElement) {
        this.uiElement = uiElement;
    }

    @Override
    public void trigger(Object... args) {
        ConfirmDismissOfficer confirmListener = new ConfirmDismissOfficer(uiElement);
        String name = uiElement.getOfficerData().getPerson().getNameString();
        int level = uiElement.getOfficerData().getPerson().getStats().getLevel();
        String str = "Are you sure you want to dismiss " + name + " (level " + level + ")?\n\nThis action is irreversible.";
        UtilReflection.showConfirmationDialog(str, "Dismiss", "Never mind", 650f, 160f, confirmListener);
    }
}
