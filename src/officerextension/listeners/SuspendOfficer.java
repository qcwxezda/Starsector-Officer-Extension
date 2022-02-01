package officerextension.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.util.Misc;
import officerextension.Settings;
import officerextension.UtilReflection.ConfirmDialogData;
import officerextension.UtilReflection;
import officerextension.ui.Button;
import officerextension.ui.OfficerUIElement;

public class SuspendOfficer extends ActionListener {

    private final OfficerUIElement uiElement;

    public SuspendOfficer(OfficerUIElement uiElement) {
        this.uiElement = uiElement;
    }

    @Override
    public void trigger(Object... args) {
        ConfirmSuspendOfficer confirmListener = new ConfirmSuspendOfficer(uiElement);
        PersonAPI officer = uiElement.getOfficerData().getPerson();
        String name = officer.getNameString();
        int level = officer.getStats().getLevel();
        String suspendCost = Misc.getDGSCredits(Settings.SUSPEND_OFFICER_COST_MULTIPLIER * Misc.getOfficerSalary(officer));
        String salaryPercent = (int) (100f * Settings.SUSPENDED_SALARY_FRACTION) + "%";
        String str = String.format("Are you sure you want to suspend %s (level %s)?" +
                "\n\nSuspended officers receive %s of their usual pay." +
                "\n\nSuspending this officer will incur an upfront fee of %s. " +
                "Reinstating a suspended officer is free and can be done at any time.",
                name,
                level,
                salaryPercent,
                suspendCost);
        boolean canAfford = Global.getSector().getPlayerFleet().getCargo().getCredits().get()
                >= Settings.SUSPEND_OFFICER_COST_MULTIPLIER * Misc.getOfficerSalary(officer);
        ConfirmDialogData data = UtilReflection.showConfirmationDialog(
                str,
                "Suspend",
                "Never mind",
                650f,
                230f,
                confirmListener);
        if (data == null) {
            return;
        }
        LabelAPI label = data.textLabel;
        label.setHighlight(salaryPercent, suspendCost);
        label.setHighlightColors(
                Misc.getHighlightColor(),
                canAfford ? Misc.getHighlightColor() : Misc.getNegativeHighlightColor());
        Button yesButton = data.confirmButton;
        if (!canAfford) {
            yesButton.setEnabled(false);
        }
    }
}
