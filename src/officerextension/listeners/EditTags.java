package officerextension.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import officerextension.Settings;
import officerextension.Util;
import officerextension.UtilReflection.ConfirmDialogData;
import officerextension.UtilReflection;
import officerextension.ui.Button;
import officerextension.ui.OfficerUIElement;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EditTags extends ActionListener {

    private final OfficerUIElement uiElement;

    public static float BUTTONS_LIST_HEIGHT = 440f, BUTTONS_LIST_X_PAD = 30f, BUTTONS_LIST_Y_PAD = 20f;
    public static float DIALOG_WIDTH = 425f;
    public static float BASE_DIALOG_HEIGHT = 144f;
    public static float DIALOG_HEIGHT_PER_BUTTON = 40f;
    public static float MAX_DIALOG_HEIGHT = 600f;
    public static float BUTTON_WIDTH = 260f, BUTTON_HEIGHT = 30f, BUTTON_PAD = 10f;

    public EditTags(OfficerUIElement element) {
        uiElement = element;
    }

    @Override
    public void trigger(Object... args) {

        Set<String> allTags = Util.getAllTags();
        float height = Math.min(BASE_DIALOG_HEIGHT + DIALOG_HEIGHT_PER_BUTTON * allTags.size(), MAX_DIALOG_HEIGHT);

        CustomPanelAPI customPanel = Global.getSettings().createCustom(DIALOG_WIDTH - 20f, height - 40f, null);
        TooltipMakerAPI buttonsList = customPanel.createUIElement(DIALOG_WIDTH - 80f, BUTTONS_LIST_HEIGHT, true);
        Map<String, ButtonAPI> buttonMap = new HashMap<>();
        boolean shouldPad = false;
        for (String str : allTags) {
            Color textColor = Settings.PERSISTENT_OFFICER_TAGS.contains(str)
                    ? Color.WHITE
                    : Misc.getBrightPlayerColor();
            ButtonAPI button = buttonsList.addAreaCheckbox(
                    str,
                    null,
                    Misc.getBasePlayerColor(),
                    Misc.getDarkPlayerColor(),
                    textColor,
                    BUTTON_WIDTH,
                    BUTTON_HEIGHT,
                    shouldPad ? BUTTON_PAD : 0);
            shouldPad = true;
            button.setChecked(Util.hasTag(uiElement.getOfficerData(), str));
            buttonMap.put(str, button);
        }

        TooltipMakerAPI textFieldMaker = customPanel.createUIElement(DIALOG_WIDTH - 80f, BUTTON_HEIGHT, false);
        TextFieldAPI textField = textFieldMaker.addTextField(BUTTON_WIDTH, BUTTON_HEIGHT);
        textField.setVerticalCursor(false);
        textField.grabFocus(false);

        Button addButton = new Button(textFieldMaker.addButton("Add", null, 75f, 25f, 0f));
        addButton.setShortcut(15, true);

        ConfirmEditTags confirmListener = new ConfirmEditTags(uiElement, buttonMap);
        ConfirmDialogData data = UtilReflection.showConfirmationDialog(
                "Select or add tags: ",
                "Confirm",
                "Cancel",
                DIALOG_WIDTH,
                height,
                confirmListener);
        if (data == null) {
            return;
        }

        final AddTagButtonListener addTagListener = new AddTagButtonListener(buttonsList, textField, buttonMap, customPanel, data.dialog);
        addButton.setListener(addTagListener);
        addButton.getPosition().rightOfMid((UIComponentAPI) textField, 0f).setXAlignOffset(10f);

        customPanel.addUIElement(textFieldMaker).inBL(30f, 30f);
        customPanel.addUIElement(buttonsList).inTL(BUTTONS_LIST_X_PAD, BUTTONS_LIST_Y_PAD);
        data.panel.addComponent(customPanel).inTL(10f, 30f);
    }
}
