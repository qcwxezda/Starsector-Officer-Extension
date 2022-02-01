package officerextension.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import officerextension.Util;
import officerextension.UtilReflection.ConfirmDialogData;
import officerextension.UtilReflection;
import officerextension.ui.OfficerUIElement;

import java.util.Set;

public class EditTags extends ActionListener {

    private final OfficerUIElement uiElement;

    public EditTags(OfficerUIElement element) {
        uiElement = element;
    }

    @Override
    public void trigger(Object... args) {
        CustomPanelAPI customPanel = Global.getSettings().createCustom(500f, 150f, null);
        TooltipMakerAPI tooltipMaker = customPanel.createUIElement(500f, 150f, false);
        TextFieldAPI textField = tooltipMaker.addTextField(500f, 50f);
        textField.setVerticalCursor(false);
        Set<String> tags = Util.getOfficerTags(uiElement.getOfficerData());
        StringBuilder tagsStr = new StringBuilder();
        for (String tag : tags) {
            tagsStr.append(tag).append(", ");
        }
        textField.setText(tagsStr.toString());
        textField.grabFocus(false);

        ConfirmEditTags confirmListener = new ConfirmEditTags(uiElement, textField);
        ConfirmDialogData data = UtilReflection.showConfirmationDialog(
                "Enter a comma-separated list of tag names: ",
                "Confirm",
                "Cancel",
                650f,
                160f,
                confirmListener);
        if (data == null) {
            return;
        }

        customPanel.addUIElement(tooltipMaker);
        data.panel.addComponent(customPanel).inMid();
    }
}
