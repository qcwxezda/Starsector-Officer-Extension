package officerextension;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.fleet.CampaignFleet;
import com.fs.starfarer.campaign.fleet.FleetMember;
import com.fs.starfarer.coreui.CaptainPickerDialog;
import com.fs.starfarer.rpg.OfficerData;
import officerextension.ui.Button;
import officerextension.ui.CaptainPicker;
import officerextension.ui.OfficerUIElement;
import officerextension.ui.SkillButton;
import officerextension.ui.Label;
import officerextension.listeners.*;

import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

public class CoreScript implements EveryFrameScript {

    /** Maps each officer's panel to its first child object. Can check when recreate() has
     *  been called by checking when the first child object changes. */
    private final Map<OfficerUIElement, Object> officerPanelFirstChild = new HashMap<>();

    /** Keep track of the last known index of the captain picker dialog
     * in core.getChildren (all the UI elements that are children of the core UI element);
     * this is the first index that will be checked each frame for
     * faster access. */
    private int lastCaptainDialogIndex = -1;

    /** Keep a reference of the last known officer list of the CaptainPicker; if this
     *  changes, that means the CaptainPicker has been recreated, and we need to re-inject
     *  every panel. */
    private UIPanelAPI officerListRef = null;

    private boolean isFirstFrame = true;

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {

        if (isFirstFrame) {
            // Since the core UI's "screenPanel" isn't created on the first frame, trying to do anything with the UI
            // on the first frame will cause an NPE. Therefore, we will initialize the screenPanel before trying
            // to call findAllClasses, if it hasn't been initialized already.
            try {
                CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
                Field field = campaignUI.getClass().getDeclaredField("screenPanel");
                field.setAccessible(true);
                if (field.get(campaignUI) == null) {
                    field.set(campaignUI,
                            field.getType()
                                    .getConstructor(float.class, float.class)
                                    .newInstance(
                                            Global.getSettings().getScreenWidth(),
                                            Global.getSettings().getScreenHeight()));
                    ClassRefs.findAllClasses();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            isFirstFrame = false;
            return;
        }

        if (!ClassRefs.foundAllClasses()) {
            ClassRefs.findAllClasses();
        }

        CaptainPickerDialog cpd = findCaptainPickerDialog();
        if (cpd == null) {
            // The existing dialog was closed,
            // and we have to inject every panel again
            officerPanelFirstChild.clear();
            return;
        }

        // Haven't grabbed the panels yet; do that
        // and the initial injection
        if (officerPanelFirstChild.isEmpty()) {
            officerListRef = cpd.getListOfficers();
            insertSuspendedOfficers(cpd);
            injectAll(cpd);
        }
        else {
            // If the officer list has changed, we need to re-inject every panel
            if (officerListRef != cpd.getListOfficers()) {
                officerListRef = cpd.getListOfficers();
                officerPanelFirstChild.clear();
                insertSuspendedOfficers(cpd);
                injectAll(cpd);
            }
            // Check each panel to see if we need to re-inject
            // due to that panel being recreated
            for (Map.Entry<OfficerUIElement, Object> entry : officerPanelFirstChild.entrySet()) {
                OfficerUIElement elem = entry.getKey();
                List<UIComponentAPI> children = elem.getChildrenNonCopy();
                if (children.get(0) != entry.getValue()) {
                    inject(elem);
                }
            }
        }
    }

    /** Injects the custom behavior into all officer UI elements in the captain picker dialog list. */
    private void injectAll(CaptainPickerDialog cpd) {
        List<?> officerUIList = cpd.getListOfficers().getItems();
        if (officerUIList != null) {
            for (Object o : officerUIList) {
                inject(new OfficerUIElement(o));
            }
        }
    }

    /** Injects the custom behavior into the specified officer UI element in the
     *  captain picker dialog list. */
    private void inject(OfficerUIElement elem) {
        // Mark this panel as injected by remembering its first child
        officerPanelFirstChild.put(elem, elem.getChildrenNonCopy().get(0));

        OfficerDataAPI data = elem.getOfficerData();
        // Player can't forget skills or be suspended
        if (data.getPerson().equals(Global.getSector().getPlayerPerson())) {
            return;
        }
        // AI cores can't forget skills or be suspended
        if (data.getPerson().isAICore()) {
            return;
        }
        // Mercenaries can't forget skills or be suspended
        if (Misc.isMercenary(data.getPerson())) {
            return;
        }

        if (Util.isSuspended(data)) {
            // Update the salary label
            LabelAPI label = elem.getSalaryLabel();
            String oldSalaryText = Misc.getDGSCredits(Misc.getOfficerSalary(data.getPerson()));
            String salaryText = Misc.getDGSCredits(Settings.SUSPENDED_SALARY_FRACTION * Misc.getOfficerSalary(data.getPerson()));
            label.setText("Monthly salary: " + salaryText);
            label.setHighlight(salaryText);
            // Update the position if the new salary text is shorter than the old one
            float xDiff = label.computeTextWidth(oldSalaryText) - label.computeTextWidth(salaryText);
            // getXAlignOffset is not exposed in the API
            float xAlignOffset = (float) Util.invokeGetter(label.getPosition(), "getXAlignOffset");
            label.getPosition().setXAlignOffset(xAlignOffset + xDiff);

            // Disable the selectors
            elem.getSelector().setActive(false);
            elem.getPortrait().setActive(false);

            // Disable the existing "Unassigned" and "personality, status" labels
            elem.getFleetMemberLabel().setText("");
            elem.getStatusLabel().setOpacity(0f);

            // Add our own
            String personality = elem.getOfficerData().getPerson().getPersonalityAPI().getDisplayName();
            Label temp = new Label(elem.getStatusLabel());
            LabelAPI suspendedLabel = temp.create("Suspended");
            LabelAPI newStatusLabel = temp.create(personality + ", suspended");
            suspendedLabel.setAlignment(Alignment.MID);
            suspendedLabel.setHighlight("Suspended");
            suspendedLabel.setHighlightColor(Misc.getNegativeHighlightColor());
            newStatusLabel.setHighlight(personality + ",", "suspended");
            newStatusLabel.setHighlightColors(Misc.getGrayColor(), Misc.getNegativeHighlightColor());

            try {
                Method addMethod = elem.getInstance().getClass().getMethod("add", ClassRefs.renderableUIElementInterface);
                PositionAPI suspendedPosition = (PositionAPI) addMethod.invoke(elem.getInstance(), suspendedLabel);
                PositionAPI statusPosition = (PositionAPI) addMethod.invoke(elem.getInstance(), newStatusLabel);
                Method setMethod = statusPosition.getClass().getMethod("set", statusPosition.getClass());
                setMethod.invoke(suspendedPosition, elem.getFleetMemberLabel().getPosition());
                setMethod.invoke(statusPosition, elem.getStatusLabel().getPosition());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        injectSkillButtons(elem);
        insertForgetButton(elem);
        insertSuspendButton(elem);
        insertReinstateButton(elem);

        // Reroute the dismiss button to our own listener that recreates the entire
        // captain dialog when confirmed,
        // both for consistency and so that the "reinstate button" for suspended officers
        // can be correctly made visible when dismissing a different officer reduces the number of officers to
        // below the maximum.
        elem.getDismissButton().setListener(new DismissOfficer(elem));

        elem.updateButtonVisibility();
    }

    /** Adds a custom listener to each skill button, as well as making them emit a sound when pressed.
     * Sets the [wrappedSkillButtons] field of the corresponding [OfficerUIElement]. */
    private void injectSkillButtons(OfficerUIElement elem) {
        List<ButtonAPI> skillButtons = elem.getSkillButtons();
        List<SkillButton> wrappedSkillButtons = new ArrayList<>(skillButtons.size());
        for (ButtonAPI button : skillButtons) {
            SkillButton skillButton = new SkillButton(button);
            SelectSkill buttonListener = new SelectSkill(skillButton, elem);
            wrappedSkillButtons.add(skillButton);
            skillButton.setListener(buttonListener);
            skillButton.setButtonPressedSound("ui_button_pressed");
        }
        elem.setWrappedSkillButtons(wrappedSkillButtons);
    }

    /** Adds and overlays the forget ("demote") button on top of the "Level up!" button. */
    private void insertForgetButton(OfficerUIElement elem) {
        elem.setForgetSkillsButton(
                insertButtonOnTopOfLevelUp(
                        elem,
                        "Demote",
                        Misc.getStoryOptionColor(),
                        Misc.getStoryDarkColor(),
                        new ForgetSkills(elem)
                )
        );
    }

    /** Adds and overlays the "Suspend" button on top of the "Level up!" button. */
    private void insertSuspendButton(OfficerUIElement elem) {
        elem.setSuspendButton(
                insertButtonOnTopOfLevelUp(
                        elem,
                        "Suspend",
                        Misc.getBasePlayerColor(),
                        Misc.getDarkPlayerColor(),
                        new SuspendOfficer(elem)
                )
        );
    }

    /** Adds and overlays the "Reinstate" button on top of the "Level up!" button. */
    private void insertReinstateButton(OfficerUIElement elem) {
        elem.setReinstateButton(
                insertButtonOnTopOfLevelUp(
                        elem,
                        "Reinstate",
                        Misc.getBasePlayerColor(),
                        Misc.getDarkPlayerColor(),
                        new ReinstateOfficer(elem)
                )
        );
    }

    /** Adds the panels for suspended officers to the given captain picker dialog */
    private void insertSuspendedOfficers(CaptainPickerDialog cpd) {
        List<?> officerUIList = cpd.getListOfficers().getItems();
        // Don't inject if the ship is automated and can't take human officers
        if (Misc.isAutomated(CaptainPicker.getFleetMember(cpd))) {
            return;
        }
        // Need one element in the UI list in order to get its class and constructor
        if (officerUIList == null || officerUIList.isEmpty()) {
            return;
        }
        int showBackgroundOffset = officerUIList.size() % 2 == 0 ? 1 : 0;
        try {
            Class<?> officerUIClass = officerUIList.get(0).getClass();
            Constructor<?> cons = officerUIClass.getDeclaredConstructor(
                    CaptainPickerDialog.class,
                    CampaignFleet.class,
                    FleetMember.class,
                    OfficerData.class,
                    boolean.class,
                    boolean.class
            );
            List<OfficerDataAPI> suspendedOfficers = Util.getSuspendedOfficers();
            for (int i = 0; i < suspendedOfficers.size(); i++) {
                OfficerDataAPI officer = suspendedOfficers.get(i);
                // Constructor info: captain picker dialog, fleet, fleet member, officer, gray (true) or black (false) background, is AI
                //noinspection JavaReflectionInvocation
                Object panel = cons.newInstance(
                        cpd,
                        Global.getSector().getPlayerFleet(),
                        null,
                        officer,
                        i % 2 != showBackgroundOffset,
                        false);
                Method addItem = cpd.getListOfficers()
                        .getClass()
                        .getMethod("addItem", ClassRefs.renderableUIElementInterface, Object.class);
                addItem.invoke(cpd.getListOfficers(), panel, officer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Overlays the specified button on top of the "Level up!" button. */
    private Button insertButtonOnTopOfLevelUp(OfficerUIElement elem, String text, Color base, Color bg, ActionListener listener) {
        try {
            Method addMethod = elem.getInstance().getClass().getMethod("add", ClassRefs.renderableUIElementInterface);
            Button button = Util.makeButton(
                    text,
                    listener,
                    base,
                    bg,
                    100f,
                    20f);
            ((PositionAPI) addMethod.invoke(elem.getInstance(), button.getInstance())).inTR(110f, 7f);
            return button;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private CaptainPickerDialog findCaptainPickerDialog() {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();

        Object encounterDialog = Util.getField(campaignUI, "encounterDialog");

        // If no encounter dialog, then search the core (opened fleet menu in space)
        if (encounterDialog == null) {
            CoreUIAPI core = (CoreUIAPI) Util.getField(campaignUI, "core");
            List<?> children = (List<?>) Util.invokeGetter(core, "getChildrenNonCopy");
            return findCaptainPickerDialogInList(children);
        }
        // Otherwise, search through the last item in the encounter dialog
        else {
            List<?> children = (List<?>) Util.invokeGetter(encounterDialog, "getChildrenNonCopy");
            Object lastChild = children.get(children.size() - 1);
            List<?> subChildren = (List<?>) Util.invokeGetter(lastChild, "getChildrenNonCopy");
            return findCaptainPickerDialogInList(subChildren);
        }
    }

    private CaptainPickerDialog findCaptainPickerDialogInList(List<?> items) {
        if (items != null && !items.isEmpty()) {
            // Try the last known index first
            if (lastCaptainDialogIndex >= 0 &&
                    lastCaptainDialogIndex < items.size() &&
                    items.get(lastCaptainDialogIndex) instanceof CaptainPickerDialog) {
                return (CaptainPickerDialog) items.get(lastCaptainDialogIndex);
            }
            // Not found; have to search through the entire list
            for (int i = 0; i < items.size(); i++) {
                Object component = items.get(i);
                if (component instanceof CaptainPickerDialog) {
                    lastCaptainDialogIndex = i;
                    return (CaptainPickerDialog) component;
                }
            }
        }
        return null;
    }
}
