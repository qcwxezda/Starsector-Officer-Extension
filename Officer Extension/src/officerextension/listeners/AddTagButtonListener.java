package officerextension.listeners;

import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import officerextension.ui.Renderable;
import officerextension.ui.UIComponent;
import officerextension.ui.UIPanel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddTagButtonListener extends ActionListener {

    private final TooltipMakerAPI buttonsList;
    private final TextFieldAPI textField;
    private final Map<String, ButtonAPI> buttonMap;
    private final CustomPanelAPI customPanel;
    private final UIPanel customPanelAsPanel;
    private final UIPanel dialog;

    public AddTagButtonListener(TooltipMakerAPI buttonsList,
                                TextFieldAPI textField,
                                Map<String, ButtonAPI> buttonMap,
                                CustomPanelAPI customPanel,
                                UIPanelAPI dialog) {
        this.buttonsList = buttonsList;
        this.textField = textField;
        this.buttonMap = buttonMap;
        this.customPanel = customPanel;
        customPanelAsPanel = new UIPanel(customPanel);
        this.dialog = new UIPanel(dialog);
    }

    private void tryScrollLastElementToPosition(float y) {
        List<?> children = customPanelAsPanel.getChildrenNonCopy();
        Object lastChild = children.get(children.size() - 1);
        try {
            Method setYOffset = lastChild.getClass().getMethod("setYOffset", float.class);
            setYOffset.invoke(lastChild, Math.min(Math.max(0f, buttonsList.getPosition().getHeight() - EditTags.BUTTONS_LIST_HEIGHT), Math.max(0f, y)));
        }
        catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            // Do nothing, it didn't have a scroller
        }
    }

    @Override
    public void trigger(Object... args) {
        if (textField.getText() == null) {
            return;
        }

        UIPanel buttonsListPanel = new UIPanel(buttonsList);

        String text = textField.getText().toLowerCase(Locale.ROOT).trim();

        if ("".equals(text)) {
            textField.setText("");
            return;
        }

        // If the tag already exists, just check it and don't add a new button.
        ButtonAPI existingButton = buttonMap.get(text);
        if (existingButton != null) {
            existingButton.setChecked(true);
            textField.setText("");
            // If scroller exists, scroll it to whatever we just checked
            tryScrollLastElementToPosition(
                    buttonsList.getPosition().getY()
                            + buttonsList.getPosition().getHeight()
                            - existingButton.getPosition().getY()
                            - EditTags.BUTTONS_LIST_HEIGHT
                            + EditTags.BUTTON_PAD);
            return;
        }

        ButtonAPI button = buttonsList.addAreaCheckbox(
                text,
                null,
                Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(),
                Misc.getBrightPlayerColor(),
                EditTags.BUTTON_WIDTH,
                EditTags.BUTTON_HEIGHT,
                buttonMap.size() == 0 ? 0f : EditTags.BUTTON_PAD);
        List<?> children = customPanelAsPanel.getChildrenNonCopy();
        Renderable oldLastChild = new UIComponent(children.get(children.size() - 1));
        customPanelAsPanel.remove(oldLastChild);
        customPanel.addUIElement((TooltipMakerAPI) buttonsListPanel.getInstance()).inTL(EditTags.BUTTONS_LIST_X_PAD, EditTags.BUTTONS_LIST_Y_PAD);

        // If it has a scroller, scroll to bottom
        tryScrollLastElementToPosition(buttonsList.getPosition().getHeight() - EditTags.BUTTONS_LIST_HEIGHT);
        buttonMap.put(text, button);
        button.setChecked(true);

        float newHeight = Math.min(EditTags.BASE_DIALOG_HEIGHT + EditTags.DIALOG_HEIGHT_PER_BUTTON * buttonMap.size(), EditTags.MAX_DIALOG_HEIGHT);
        if (newHeight != dialog.getPosition().getHeight()) {
            try {
                Method setSize = dialog.getInstance().getClass().getMethod("setSize", float.class, float.class);
                setSize.invoke(dialog.getInstance(), dialog.getPosition().getWidth(), newHeight);
                customPanel.getPosition().setSize(customPanel.getPosition().getWidth(), newHeight - EditTags.DIALOG_HEIGHT_PER_BUTTON);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        textField.setText("");
    }
}
