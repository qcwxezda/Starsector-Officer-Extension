package officerextension.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.coreui.CaptainPickerDialog;
import officerextension.Util;

import java.lang.reflect.*;
import java.util.List;

/** This is the UI element for a single officer
 *  in the officer selection screen. */
public class OfficerUIElement extends RenderableUIElement {

    private Button forgetSkillsButton;
    private Button suspendButton;
    private Button reinstateButton;
    private List<SkillButton> wrappedSkillButtons;

    private static String skillButtonsFieldName;
    private static String levelUpButtonFieldName;
    private static String captainPickerDialogFieldName;
    private static String fleetMemberLabelFieldName;
    private static String statusLabelFieldName;
    private static String dismissButtonFieldName;

    /** [o] should be an instance of the underlying obfuscated officer's UI panel */
    public OfficerUIElement(Object o) {
        super(o);
    }

    public OfficerDataAPI getOfficerData() {
        return (OfficerDataAPI) Util.invokeGetter(inner, "getOfficerData");
    }

    public Object getInstance() {
        return inner;
    }

    /** Will throw NPE if [forgetSkillsButton] hasn't been set */
    public Button getLevelUpButton() {
        if (levelUpButtonFieldName != null) {
            return new Button((ButtonAPI) Util.getField(inner, levelUpButtonFieldName));
        }
        // Not memoized, have to search through every field
        Field[] fields = inner.getClass().getDeclaredFields();
        float minDist = Float.MAX_VALUE;
        Field levelUpField = null;
        for (Field field : fields) {
            if (ButtonAPI.class.isAssignableFrom(field.getType())) {
                // Check the position to see if it's at the right position
                // We'll pick the field with the minimum (Manhattan) distance from the "forget..." button
                // and assume that's the level up button
                ButtonAPI button = (ButtonAPI) Util.getField(inner, field.getName());
                if (button != null) {
                    float buttonX = button.getPosition().getX();
                    float buttonY = button.getPosition().getY();
                    Button forgetButton = getForgetSkillsButton();
                    float fButtonX = forgetButton.getPosition().getX();
                    float fButtonY = forgetButton.getPosition().getY();
                    float dist = Math.abs(fButtonX - buttonX) + Math.abs(fButtonY - buttonY);
                    if (dist < minDist) {
                        minDist = dist;
                        levelUpField = field;
                    }
                }
            }
        }
        if (levelUpField == null) {
            throw new RuntimeException("Could not find the \"Level up!\" button");
        }
        levelUpButtonFieldName = levelUpField.getName();
        return new Button((ButtonAPI) Util.getField(inner, levelUpField.getName()));
    }

    /** Will throw NPE if [forgetSkillsButton] or [levelUpButtonFieldName] hasn't been set */
    public Button getDismissButton() {
        if (dismissButtonFieldName != null) {
            return new Button((ButtonAPI) Util.getField(inner, dismissButtonFieldName));
        }
        // Not memoized, have to search through every field
        Field[] fields = inner.getClass().getDeclaredFields();
        float minDist = Float.MAX_VALUE;
        Field dismissField = null;
        for (Field field : fields) {
            if (ButtonAPI.class.isAssignableFrom(field.getType())) {
                // Check the position to see if it's at the right position
                // We'll pick the field with the minimum y-distance from the "forget..." button
                // that isn't the level up button, and assume that that's the dismiss button
                ButtonAPI button = (ButtonAPI) Util.getField(inner, field.getName());
                if (button != null && !field.getName().equals(levelUpButtonFieldName)) {
                    Button forgetButton = getForgetSkillsButton();
                    float dist = Math.abs(forgetButton.getPosition().getY() - button.getPosition().getY());
                    if (dist < minDist) {
                        minDist = dist;
                        dismissField = field;
                    }
                }
            }
        }
        if (dismissField == null) {
            throw new RuntimeException("Could not find the \"Dismiss\" button");
        }
        dismissButtonFieldName = dismissField.getName();
        return new Button((ButtonAPI) Util.getField(inner, dismissField.getName()));
    }

    public Button getForgetSkillsButton() {
        return forgetSkillsButton;
    }

    public void setForgetSkillsButton(Button button) {
        forgetSkillsButton = button;
    }

    public Button getSuspendButton() {
        return suspendButton;
    }

    public void setReinstateButton(Button button) { reinstateButton = button; }

    public Button getReinstateButton() { return reinstateButton; }

    public void setSuspendButton(Button button) {
        suspendButton = button;
    }

    public List<SkillButton> getWrappedSkillButtons() {
        return wrappedSkillButtons;
    }

    public void setWrappedSkillButtons(List<SkillButton> buttons) {
        wrappedSkillButtons = buttons;
    }

    public LabelAPI getSalaryLabel() {
        List<?> children = (List<?>) Util.invokeGetter(inner, "getChildrenNonCopy");
        for (Object o : children) {
            if (o instanceof LabelAPI && ((LabelAPI) o).getText().startsWith("Monthly salary")) {
                return (LabelAPI) o;
            }
        }
        throw new RuntimeException("Could not find the salary label.");
    }

    /** This is the list of button objects corresponding to the skills the officer has. */
    @SuppressWarnings("unchecked")
    public List<ButtonAPI> getSkillButtons() {
        if (skillButtonsFieldName != null) {
            return (List<ButtonAPI>) Util.getField(inner, skillButtonsFieldName);
        }
        // Search through the fields to find one with type List<ButtonAPI>
        // there should only be one
        Field[] fields = inner.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (List.class.isAssignableFrom(field.getType())) {
                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType) {
                    Type argType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
                    if (argType instanceof Class && ButtonAPI.class.isAssignableFrom((Class<?>) argType)) {
                        // Found the right field
                        skillButtonsFieldName = field.getName();
                        return (List<ButtonAPI>) Util.getField(inner, field.getName());
                    }
                }
            }
        }
        throw new RuntimeException("Field for skill buttons not found");
    }

    public Button getPortrait() {
        return new Button((ButtonAPI) Util.invokeGetter(inner, "getPortrait"));
    }

    public Button getSelector() {
        return new Button((ButtonAPI) Util.invokeGetter(inner, "getSelector"));
    }

    public LabelAPI getFleetMemberLabel() {
        if (fleetMemberLabelFieldName != null) {
            return (LabelAPI) Util.getField(inner, fleetMemberLabelFieldName);
        }
        // Search through the fields to find one with the method "getText"
        // This should always say "unassigned"
        for (Field field : inner.getClass().getDeclaredFields()) {
            Object value = Util.getField(inner, field.getName());
            if (value != null) {
                try {
                    Method getText = value.getClass().getMethod("getText");
                    String text = (String) getText.invoke(value);
                    if ("Unassigned".equals(text)) {
                        fleetMemberLabelFieldName = field.getName();
                        return (LabelAPI) Util.getField(inner, field.getName());
                    }
                } catch (Exception e) {
                    // Do nothing -- move on to the next field
                }
            }
        }
        throw new RuntimeException("Couldn't find the \"Unassigned\" label object that overlays the ship icon");
    }

    public LabelAPI getStatusLabel() {
        if (statusLabelFieldName != null) {
            return (LabelAPI) Util.getField(inner, statusLabelFieldName);
        }
        // Search through the fields to find one with the method "getText"
        // This should always start with the officer's personality
        String personality = getOfficerData().getPerson().getPersonalityAPI().getDisplayName();
        for (Field field : inner.getClass().getDeclaredFields()) {
            Object value = Util.getField(inner, field.getName());
            if (value != null) {
                try {
                    Method getText = value.getClass().getMethod("getText");
                    String text = (String) getText.invoke(value);
                    if (text.startsWith(personality)) {
                        statusLabelFieldName = field.getName();
                        return (LabelAPI) Util.getField(inner, field.getName());
                    }
                } catch (Exception e) {
                    // Do nothing -- move on to the next field
                }
            }
        }
        throw new RuntimeException("Couldn't find the officer status label object");
    }

    public CaptainPickerDialog getCaptainPickerDialog() {
        if (captainPickerDialogFieldName != null) {
            return (CaptainPickerDialog) Util.getField(inner, captainPickerDialogFieldName);
        }
        // Search through the fields to find one with type CaptainPicker
        // there should only be one
        Field[] fields = inner.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (CaptainPickerDialog.class.isAssignableFrom(field.getType())) {
                captainPickerDialogFieldName = field.getName();
                return (CaptainPickerDialog) Util.getField(inner, field.getName());
            }
        }
        throw new RuntimeException("Field for parent captain selection dialog not found");
    }

    @SuppressWarnings("unchecked")
    public List<UIComponentAPI> getChildrenNonCopy() {
        return (List<UIComponentAPI>) Util.invokeGetter(inner, "getChildrenNonCopy");
    }

    /** The rules for which button is visible in the "Level up!" button slot is as follows:
     *    - If the officer can level up, the level up button is always shown.
     *    - Otherwise, if any skills are selected for removal, the forget button is shown.
     *    - Otherwise, the suspend button is shown.
     */
    public void updateButtonVisibility() {
        // Only show the "forget..." button if at least one skill is actually selected
        boolean selectedAtLeastOne = false;
        for (SkillButton button : getWrappedSkillButtons()) {
            if (button.isSelected()) {
                selectedAtLeastOne = true;
                break;
            }
        }

        // Also, level up takes precedence; if an officer is able to level up he won't be able to forget skills
        // Show forget button, hide level up button
        if (getOfficerData().canLevelUp()) {
            getLevelUpButton().setOpacity(1f);
            getForgetSkillsButton().setOpacity(0f);
            getSuspendButton().setOpacity(0f);
            getReinstateButton().setOpacity(0f);
        }
        else if (selectedAtLeastOne) {
            getLevelUpButton().setOpacity(0f);
            getForgetSkillsButton().setOpacity(1f);
            getSuspendButton().setOpacity(0f);
            getReinstateButton().setOpacity(0f);
        }
        else if (Util.isSuspended(getOfficerData())) {
            getLevelUpButton().setOpacity(0f);
            getForgetSkillsButton().setOpacity(0f);
            getSuspendButton().setOpacity(0f);
            getReinstateButton().setOpacity(1f);
            // reinstate button is enabled iff the reinstating the officer would not make the player go past the limit
            CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
            getReinstateButton().setEnabled(
                    Misc.getMaxOfficers(playerFleet) > Misc.getNumNonMercOfficers(playerFleet));
        }
        else {
            getLevelUpButton().setOpacity(0f);
            getForgetSkillsButton().setOpacity(0f);
            getSuspendButton().setOpacity(1f);
            getReinstateButton().setOpacity(0f);
        }
    }

    /** Refreshes this officer's panel. Creates new instances of each button, so will need to inject again
     *  whenever the recreate method is called, here or in the obfuscated game code. */
    public void recreate() {
        try {
            inner.getClass().getMethod("recreate").invoke(inner);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
