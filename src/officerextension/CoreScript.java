package officerextension;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.plugins.OfficerLevelupPlugin;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.coreui.CaptainPickerDialog;
import officerextension.filter.OfficerFilter;
import officerextension.ui.*;
import officerextension.listeners.*;
import officerextension.ui.Button;
import officerextension.ui.Label;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

public class CoreScript implements EveryFrameScript {

    /** Keep track of the initial assignments for undoing purposes */
    private final Map<FleetMemberAPI, PersonAPI> initialOfficerMap = new HashMap<>();

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
    private CaptainPickerDialog cpdRef = null;

    /** The injected "number of officers in fleet" label */
    private Label numOfficersLabel = null;
    private boolean isFirstFrame = true;
    private boolean injectedCurrentDialog = false;
    private final DialogHandler dialogHandler;

    private final FleetPanelInjector fleetPanelInjector;

    /** Really a set, but we map filters to themselves in order to enable "get" */
    private final Map<OfficerFilter, OfficerFilter> activeFilters = new HashMap<>();

    public CoreScript() {
        fleetPanelInjector = new FleetPanelInjector();
        List<DialogHandler> handlers = Global.getSector().getListenerManager().getListeners(DialogHandler.class);
        dialogHandler = handlers.get(0);
    }

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

        fleetPanelInjector.advance();

        CaptainPickerDialog cpd = findCaptainPickerDialog();
        if (cpd == null || cpd != cpdRef) {
            if (injectedCurrentDialog) {
                dialogHandler.tempRemoveSuspendedOfficers();
            }
            // The existing dialog was closed,
            // and we have to inject every panel again
            // Also clear any existing filters
            officerPanelFirstChild.clear();
            injectedCurrentDialog = false;
            cpdRef = cpd;
            activeFilters.clear();
            return;
        }

        // Haven't injected the dialog yet; do that
        if (!injectedCurrentDialog) {
            FleetDataAPI fleetData = Global.getSector().getPlayerFleet().getFleetData();
            // Populate the initial officer map for undoing purposes
            initialOfficerMap.clear();
            for (FleetMemberAPI fm : fleetData.getMembersListCopy()) {
                initialOfficerMap.put(fm, fm.getCaptain());
            }
            dialogHandler.addSuspendedOfficersBack();
            cpd.sizeChanged(0f, 0f);
            injectCaptainPickerDialog(cpd);
            injectedCurrentDialog = true;
        }
        else {
            // If the officer list has changed, we need to re-inject every panel
            if (officerListRef != UtilReflection.invokeGetter(cpd, "getListOfficers")) {
                injectCaptainPickerDialog(cpd);
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

        updateNumOfficersLabel();
    }

    public void injectCaptainPickerDialog(CaptainPickerDialog cpd) {
        officerListRef = (UIPanelAPI) UtilReflection.invokeGetter(cpd, "getListOfficers");
        officerPanelFirstChild.clear();
        insertUndoButton(cpd);
        // AI cores can't have tags
        if (!Misc.isAutomated(CaptainPicker.getFleetMember(cpd))) {
            insertFilterButton(cpd);
            insertClearFiltersButton(cpd);
            injectNumOfficersLabel(cpd);
            insertSortButton(cpd);
            applyFilters(cpd);
        }
        injectAll(cpd);
    }

    /** Modifies the officers: x/y label of the dialog so that x is the number of
     *  assigned officers rather than the total number of officers. */
    private void injectNumOfficersLabel(CaptainPickerDialog cpd) {
        LabelAPI numOfficersLabel = CaptainPicker.getNumOfficersLabel(cpd);
        numOfficersLabel.setOpacity(0f);
        Label temp = new Label(numOfficersLabel);
        temp.removeTooltip();
        Label newLabel = temp.createSmallInsigniaLabel("", Alignment.LMID);
        newLabel.setHighlightOnMouseover(true);
        this.numOfficersLabel = newLabel;
        UIPanel panel = new UIPanel(UtilReflection.invokeGetter(cpd, "getInnerPanel"));
        panel.add(newLabel).set(numOfficersLabel.getPosition());
        updateNumOfficersLabel();
    }

    public void updateNumOfficersLabel() {
        if (numOfficersLabel == null) {
            return;
        }

        int numAssigned = Util.countAssignedNonMercOfficers(Global.getSector().getPlayerFleet());
        int numMax = Util.getMaxPlayerOfficers();
        numOfficersLabel.getInstance().setText(String.format("Assigned: %s / %s", numAssigned, numMax));
        numOfficersLabel.getInstance().setHighlight("Assigned:", "" + numAssigned, "/ " + numMax);
        numOfficersLabel.getInstance().setHighlightColors(
                Misc.getGrayColor(),
                numAssigned >= numMax ? Misc.getNegativeHighlightColor() : Misc.getHighlightColor(),
                Misc.getHighlightColor());
    }

    private void insertUndoButton(CaptainPickerDialog cpd) {
        UndoAssignments undoListener = new UndoAssignments(initialOfficerMap, this);
        Button undoButton = UtilReflection.makeButton(
                "Undo assignments",
                undoListener,
                Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(),
                150f,
                25f);
        UIPanel panel = new UIPanel(UtilReflection.invokeGetter(cpd, "getInnerPanel"));
        if (Misc.isAutomated(CaptainPicker.getFleetMember(cpd))) {
            panel.add(undoButton).getInstance().inBL(10f, 10f);
        }
        else {
            panel.add(undoButton).getInstance().inBMid(10f).setXAlignOffset(-250f);
        }
    }

    private void insertFilterButton(CaptainPickerDialog cpd) {
        FilterOfficers filterListener = new FilterOfficers(cpd, this);
        Button filterButton = UtilReflection.makeButton(
                "Filter officers",
                filterListener,
                Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(),
                120f,
                25f
        );
        UIPanel panel = new UIPanel(UtilReflection.invokeGetter(cpd, "getInnerPanel"));
        panel.add(filterButton).getInstance().inBMid(10f).setXAlignOffset(-100f);
    }

    private void insertClearFiltersButton(CaptainPickerDialog cpd) {
        ClearFilters clearListener = new ClearFilters(cpd, this);
        Button clearButton = UtilReflection.makeButton(
                "Clear filters",
                clearListener,
                Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(),
                120f,
                25f
        );
        UIPanel panel = new UIPanel(UtilReflection.invokeGetter(cpd, "getInnerPanel"));
        panel.add(clearButton).getInstance().inBMid(10f).setXAlignOffset(35f);
    }

    private void insertSortButton(CaptainPickerDialog cpd) {
        SortOfficers sortListener = new SortOfficers(cpd, this);
        Button sortButton = UtilReflection.makeButton(
                "Sort officers",
                sortListener,
                Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(),
                120f,
                25f
        );
        UIPanel panel = new UIPanel(UtilReflection.invokeGetter(cpd, "getInnerPanel"));
        panel.add(sortButton).getInstance().inBMid(10f).setXAlignOffset(170f);
    }

    /** Injects the custom behavior into all officer UI elements in the captain picker dialog list. */
    private void injectAll(CaptainPickerDialog cpd) {
        List<?> officerUIList = (List<?>) UtilReflection.invokeGetter(UtilReflection.invokeGetter(cpd, "getListOfficers"), "getItems");
        if (officerUIList != null) {
            for (Object o : officerUIList) {
                inject(new OfficerUIElement(o, this));
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

        // Inject our own selection listener to the portrait and selector
        AssignOfficer listener = new AssignOfficer(elem, elem.getPortrait().getListener());
        elem.getPortrait().setListener(listener);
        elem.getSelector().setListener(listener);
        elem.getPortrait().setEnabled(true);
        elem.getSelector().setEnabled(true);

        // Set the *panel*'s (not the officer's!) local "isMercenary" tag to true
        // this bypasses the per-frame isPastMax check
        elem.setIsMercenary(true);

        // Mercenaries can't forget skills or be suspended
        if (Misc.isMercenary(data.getPerson())) {
            insertEditTagsButton(elem);
            return;
        }

        // Undo the weird behavior with exceptional cryopod officers where they only have
        // the "retrain" option regardless of their level
        PersonAPI officerPerson = elem.getOfficerData().getPerson();
        if (officerPerson.getMemoryWithoutUpdate().getBoolean(MemFlags.EXCEPTIONAL_SLEEPER_POD_OFFICER)) {
            OfficerLevelupPlugin levelUpPlugin = (OfficerLevelupPlugin) Global.getSettings().getPlugin("officerLevelUp");
            // If fewer than 5 elite skills, change the retrain button to make skills elite
            if (Misc.getNumEliteSkills(officerPerson) < levelUpPlugin.getMaxEliteSkills(officerPerson)) {
                elem.getRetrainButton().setOpacity(0f);
                elem.getMakeSkillsEliteButton().setOpacity(1f);
            }
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
            float xAlignOffset = (float) UtilReflection.invokeGetter(label.getPosition(), "getXAlignOffset");
            label.getPosition().setXAlignOffset(xAlignOffset + xDiff);

            // Disable the existing "Unassigned" and "personality, status" labels
            elem.getFleetMemberLabel().setText("");
            elem.getStatusLabel().setOpacity(0f);

            // Add our own
            String personality = elem.getOfficerData().getPerson().getPersonalityAPI().getDisplayName();
            Label temp = new Label(elem.getStatusLabel());
            Label suspendedLabel = temp.create("Suspended");
            Label newStatusLabel = temp.create(personality + ", suspended");
            suspendedLabel.getInstance().setAlignment(Alignment.MID);
            suspendedLabel.getInstance().setHighlight("Suspended");
            suspendedLabel.getInstance().setHighlightColor(Misc.getNegativeHighlightColor());
            newStatusLabel.getInstance().setHighlight(personality + ",", "suspended");
            newStatusLabel.getInstance().setHighlightColors(Misc.getGrayColor(), Misc.getNegativeHighlightColor());

            elem.add(suspendedLabel).set(elem.getFleetMemberLabel().getPosition());
            elem.add(newStatusLabel).set(elem.getStatusLabel().getPosition());
        }

        injectSkillButtons(elem);
        insertForgetButton(elem);
        insertSuspendButton(elem);
        insertReinstateButton(elem);
        insertEditTagsButton(elem);

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

    /** Adds and overlays the "Edit tags" button to the left of the XP bar. */
    private void insertEditTagsButton(OfficerUIElement elem) {
            insertButtonAtPosition(
                    elem,
                    "Edit tags",
                    Misc.getBasePlayerColor(),
                    Misc.getDarkPlayerColor(),
                    new EditTags(elem),
                    75f,
                    20f,
                    470f,
                    7f
            );
    }

    @SuppressWarnings("SameParameterValue")
    private Button insertButtonAtPosition(
            OfficerUIElement elem,
            String text,
            Color base,
            Color bg,
            ActionListener listener,
            float width,
            float height,
            float offsetX,
            float offsetY) {
        Button button = UtilReflection.makeButton(text, listener, base, bg, width, height);
        new UIPanel(elem.getInstance()).add(button).getInstance().inTR(offsetX, offsetY);
        return button;
    }

    /** Overlays the specified button on top of the "Level up!" button. */
    private Button insertButtonOnTopOfLevelUp(OfficerUIElement elem, String text, Color base, Color bg, ActionListener listener) {
        return insertButtonAtPosition(elem, text, base, bg, listener, 100f, 20f,110f, 7f);
    }

    private CaptainPickerDialog findCaptainPickerDialog() {
        UIPanelAPI core = UtilReflection.getCoreUI();
        if (core == null) {
            return null;
        }
        List<?> children = (List<?>) UtilReflection.invokeGetter(core, "getChildrenNonCopy");
        return findCaptainPickerDialogInList(children);
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

    public void applyFilters(CaptainPickerDialog cpd) {
        List<Object> filteredPanels = new ArrayList<>();
        Object officerList = UtilReflection.invokeGetter(cpd, "getListOfficers");
        List<?> items = (List<?>) UtilReflection.invokeGetter(officerList, "getItems");

        for (Object elem : items) {
            OfficerDataAPI officerData = OfficerUIElement.getOfficerData(elem);
            // Don't filter out the player ever
            if (officerData.getPerson().isPlayer()) {
                continue;
            }
            for (OfficerFilter filter : activeFilters.values()) {
                if (!filter.check(officerData)) {
                    // Filter out this UI element
                    filteredPanels.add(elem);
                    break;
                }
            }
        }

        try {
            for (Object o : filteredPanels) {
                Method removeItem = officerList.getClass().getMethod("removeItem", ClassRefs.renderableUIElementInterface);
                removeItem.invoke(officerList, o);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Method collapseEmptySlots = officerList.getClass().getMethod("collapseEmptySlots", boolean.class);
            collapseEmptySlots.invoke(officerList, true);
        }
        catch (Exception e) {
            // Do nothing
        }
    }

    public void updateActiveFilters(Set<OfficerFilter> newFilters, CaptainPickerDialog cpd) {
        if (activeFilters.isEmpty() && newFilters.isEmpty()) {
            return;
        }
        // We will re-filter if the filter list isn't empty, even if the parameters are the same
        // this is because a player might edit an officer's tags in between filters
        activeFilters.clear();
        for (OfficerFilter filter : newFilters) {
            activeFilters.put(filter, filter);
        }
        cpd.sizeChanged(0f, 0f);
        try {
            Object scroller = UtilReflection.invokeGetter(UtilReflection.invokeGetter(cpd, "getListOfficers"), "getScroller");
            Method setYOffset = scroller.getClass().getMethod("setYOffset", float.class);
            setYOffset.invoke(scroller, 0f);
        }
        catch (Exception e) {
            // Do nothing
        }
        injectCaptainPickerDialog(cpd);
    }

    public Map<OfficerFilter, OfficerFilter> getActiveFilters() {
        return activeFilters;
    }
}
