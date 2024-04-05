package officerextension;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import officerextension.listeners.AutoAssignIdleOfficers;
import officerextension.ui.Button;
import officerextension.ui.Label;
import officerextension.ui.UIPanel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class FleetPanelInjector {

    private static Field fleetInfoPanelField;
    private static Field autoAssignButtonField;
    private static Field idleOfficersLabelTableField;

    /** Keep track of the last known fleet info panel to track when it changes */
    private UIPanelAPI fleetInfoPanelRef;

    private boolean injected = false;
    private Label idleOfficersLabel;
    private Button autoAssignButton;

    public void advance() {
        UIPanelAPI fleetInfoPanel = findFleetInfoPanel();

        if (fleetInfoPanel == null
                || fleetInfoPanel != fleetInfoPanelRef) {
            injected = false;
            fleetInfoPanelRef = fleetInfoPanel;
            idleOfficersLabel = null;
            autoAssignButton = null;
            return;
        }

        if (!injected) {
            injected = true;

            Button oldAutoAssignButton = new Button(getAutoAssignButton(fleetInfoPanel));
            oldAutoAssignButton.setOpacity(0f);
            idleOfficersLabel = replaceNumIdleOfficersLabel(fleetInfoPanel);
            autoAssignButton = UtilReflection.makeButton(
                    "   Auto-assign idle officers",
                    new AutoAssignIdleOfficers(oldAutoAssignButton),
                    Misc.getBasePlayerColor(),
                    Misc.getDarkPlayerColor(),
                    Alignment.LMID,
                    CutStyle.BL_TR,
                    oldAutoAssignButton.getPosition().getWidth(),
                    oldAutoAssignButton.getPosition().getHeight());
            new UIPanel(fleetInfoPanel)
                    .add(autoAssignButton)
                    .set(oldAutoAssignButton.getPosition());
        }

        if (idleOfficersLabel != null && autoAssignButton != null) {
            int idle = Util.countIdleOfficers(Global.getSector().getPlayerFleet());
            int assigned = Util.countAssignedNonMercOfficers(Global.getSector().getPlayerFleet());
            boolean hasUnofficeredShip = false;
            for (FleetMemberAPI fm : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
                if (!Misc.isAutomated(fm) && fm.getCaptain().isDefault()) {
                    hasUnofficeredShip = true;
                    break;
                }
            }
            boolean canAutoAssign = idle > 0
                    && assigned < Util.getMaxPlayerOfficers()
                    && hasUnofficeredShip;

            LabelAPI inner = idleOfficersLabel.getInstance();
            inner.setText("" + idle);
            inner.setHighlight("" + idle);
            idleOfficersLabel.autoSize();

            if (autoAssignButton.getInstance().isEnabled() && !canAutoAssign) {
                autoAssignButton.setEnabled(false);
            }
            if (!autoAssignButton.getInstance().isEnabled() && canAutoAssign) {
                autoAssignButton.setEnabled(true);
            }
        }
    }

    public UIPanelAPI findFleetInfoPanel() {
        if (!CoreUITabId.FLEET.equals(Global.getSector().getCampaignUI().getCurrentCoreTab())) {
            return null;
        }
        UIPanelAPI core = UtilReflection.getCoreUI();
        if (core == null) {
            return null;
        }
        UIPanelAPI currentTab = (UIPanelAPI) UtilReflection.invokeGetter(core, "getCurrentTab");
        // Since the current tab ID is fleet, this *should* give us the fleet tab.
        // We need to find the field corresponding to the info panel. There's no good way to do this,
        // other than to go through every declared field, check that it's a UIPanelAPI, then look for
        // a LabelAPI and a CampaignFleetAPI field in that
        if (fleetInfoPanelField == null) {
            outer:
            for (Field field : currentTab.getClass().getDeclaredFields()) {
                if (!UIPanelAPI.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                boolean hasLabelField = false;
                boolean hasFleetField = false;
                for (Field innerField : field.getType().getDeclaredFields()) {
                    if (CampaignFleetAPI.class.isAssignableFrom(innerField.getType())) {
                        hasFleetField = true;
                    }
                    if (LabelAPI.class.isAssignableFrom(innerField.getType())) {
                        hasLabelField = true;
                    }
                    // The outer field is the fleet info panel
                    if (hasFleetField && hasLabelField) {
                        fleetInfoPanelField = field;
                        break outer;
                    }
                }
            }
        }

        if (fleetInfoPanelField == null) {
            throw new RuntimeException("Could not find the fleet info panel for the fleet tab");
        }

        fleetInfoPanelField.setAccessible(true);
        try {
            return (UIPanelAPI) fleetInfoPanelField.get(currentTab);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Returns the label that replaced the old "number of idle officers" label */
    public Label replaceNumIdleOfficersLabel(UIPanelAPI fleetInfoPanel) {
        if (idleOfficersLabelTableField == null) {
            // Look for a field that has a getRow method
            // then check the nameLabel in getRow(0, 0)
            // if it says "Idle officers" we know we found the right label
            for (Field field : fleetInfoPanel.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object o;
                try {
                    o = field.get(fleetInfoPanel);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    o = null;
                }
                if (o == null) {
                    continue;
                }
                try {
                    Method getRow = o.getClass().getMethod("getRow", int.class, int.class);
                    Object row = getRow.invoke(o, 0, 0);
                    Field nameLabel = row.getClass().getDeclaredField("nameLabel");
                    nameLabel.setAccessible(true);
                    String text = ((LabelAPI) nameLabel.get(row)).getText();
                    if (text.trim().startsWith("Idle officers")) {
                        idleOfficersLabelTableField = field;
                        break;
                    }
                }
                catch (Exception e) {
                    // Continue
                }
            }
        }

        if (idleOfficersLabelTableField == null) {
            throw new RuntimeException("Could not find the \"idle officers: x\" text in the fleet info panel");
        }

        idleOfficersLabelTableField.setAccessible(true);
        try {
            Object table = idleOfficersLabelTableField.get(fleetInfoPanel);
            Object row = UtilReflection.invokeGetter(table, "getRow", 0, 0);
            LabelAPI originalLabel = (LabelAPI) UtilReflection.getField(row, "valueLabel");
            originalLabel.setOpacity(0f);
            Label newLabel = new Label(originalLabel).create("" + 0);
            newLabel.getInstance().setHighlight("" + 0);
            newLabel.getInstance().setHighlightColor(Misc.getHighlightColor());
            new UIPanel(fleetInfoPanel).add(newLabel).set(originalLabel.getPosition());
            return newLabel;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ButtonAPI getAutoAssignButton(UIPanelAPI fleetInfoPanel) {
        if (autoAssignButtonField == null) {
            // Find the button that starts with "Auto-assign"
            for (Field field : fleetInfoPanel.getClass().getDeclaredFields()) {
                if (ButtonAPI.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    ButtonAPI button;
                    try {
                        button = (ButtonAPI) field.get(fleetInfoPanel);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        button = null;
                    }
                    if (button != null && new Button(button).getText().trim().startsWith("Auto-assign")) {
                        autoAssignButtonField = field;
                        break;
                    }
                }
            }
        }

        if (autoAssignButtonField == null) {
            throw new RuntimeException("Could not find the auto-assign button");
        }

        autoAssignButtonField.setAccessible(true);
        try {
            return (ButtonAPI) autoAssignButtonField.get(fleetInfoPanel);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
