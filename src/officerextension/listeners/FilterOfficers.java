package officerextension.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.CharacterStats;
import com.fs.starfarer.coreui.CaptainPickerDialog;
import com.fs.starfarer.loading.SkillSpec;
import com.fs.starfarer.ui.impl.StandardTooltipV2;
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable;
import officerextension.CoreScript;
import officerextension.Settings;
import officerextension.Util;
import officerextension.UtilReflection;
import officerextension.filter.*;
import officerextension.ui.Button;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

public class FilterOfficers extends ActionListener {

    private final CaptainPickerDialog dialog;
    private final CoreScript injector;
    private static final Set<String> validSkillIds = new HashSet<>();
    static {
        validSkillIds.add(Skills.HELMSMANSHIP);
        validSkillIds.add(Skills.COMBAT_ENDURANCE);
        validSkillIds.add(Skills.IMPACT_MITIGATION);
        validSkillIds.add(Skills.DAMAGE_CONTROL);
        validSkillIds.add(Skills.FIELD_MODULATION);
        validSkillIds.add(Skills.POINT_DEFENSE);
        validSkillIds.add(Skills.TARGET_ANALYSIS);
        validSkillIds.add(Skills.BALLISTIC_MASTERY);
        validSkillIds.add(Skills.SYSTEMS_EXPERTISE);
        validSkillIds.add(Skills.MISSILE_SPECIALIZATION);
        validSkillIds.add(Skills.GUNNERY_IMPLANTS);
        validSkillIds.add(Skills.ENERGY_WEAPON_MASTERY);
        validSkillIds.add(Skills.ORDNANCE_EXPERTISE);
        validSkillIds.add(Skills.POLARIZED_ARMOR);
    }

    public FilterOfficers(CaptainPickerDialog dialog, CoreScript injector) {
        this.dialog = dialog;
        this.injector = injector;
    }

    private void setTooltipPrev(StandardTooltipV2Expandable tooltip, UIComponentAPI prev) {
        try {
            Class<?> cls = tooltip.getClass().getEnclosingClass() == null ? tooltip.getClass() : tooltip.getClass().getEnclosingClass();
            // Anonymous class, have to use the enclosing class
            Field prevField = cls.getDeclaredField("prev");
            prevField.setAccessible(true);
            prevField.set(tooltip, prev);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Method addTooltipAboveMethod() {
        for (Method method : StandardTooltipV2Expandable.class.getMethods()) {
            if ("addTooltipAbove".equals(method.getName()) && method.getParameterTypes().length == 2) {
                return method;
            }
        }
        throw new RuntimeException("StandardTooltipV2Expandable.addTooltipAbove not found");
    }

    private void setTooltipHeight(StandardTooltipV2Expandable tooltip, float height) {
        try {
            Class<?> cls = tooltip.getClass().getEnclosingClass() == null ? tooltip.getClass() : tooltip.getClass().getEnclosingClass();
            // Anonymous class, have to use the enclosing class
            Field heightField = cls.getDeclaredField("height");
            heightField.setAccessible(true);
            heightField.set(tooltip, height);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setComponentOpacity(UIComponentAPI component, float opacity) {
        try {
            component.getClass().getMethod("setOpacity", float.class).invoke(component, opacity);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void trigger(Object... args) {
        CustomPanelAPI customPanel = Global.getSettings().createCustom(600f, 480f, null);
        StandardTooltipV2Expandable tooltipMaker = (StandardTooltipV2Expandable) customPanel.createUIElement(150f, 480f, false);
        Map<OfficerFilter, OfficerFilter> activeFilters = injector.getActiveFilters();
        Map<ButtonAPI, OfficerFilter> buttonMap = new HashMap<>();
        PersonAPI randomPerson = Global.getSettings().createPerson();

        List<String> sortedSkillIds = Global.getSettings().getSortedSkillIds();
        Method addTooltipAbove = addTooltipAboveMethod();
        int rows = 7;
        int i = 0;
        float maxHeight = 0f;
        for (String id : sortedSkillIds) {
            SkillSpecAPI spec = Global.getSettings().getSkillSpec(id);
            if (spec.hasTag("deprecated")) {
                continue;
            }
            if (!spec.isCombatOfficerSkill()) {
                continue;
            }
            if (!validSkillIds.contains(id)) {
                continue;
            }
            i++;
            @SuppressWarnings("IntegerDivisionInFloatingPointContext")
            float offset = (i - 1) % rows == 0 ? 60f * (i / rows) : 0f;
            ButtonAPI button = tooltipMaker.addAreaCheckbox(
                    "",
                    null,
                    Misc.getBasePlayerColor(),
                    spec.getGoverningAptitudeColor(),
                    Misc.getStoryOptionColor(),
                    56f,
                    56f,
                    10f);
            button.getPosition().setXAlignOffset(offset);
            Button wrapped = new Button(button);
            UIComponentAPI oldPrev = tooltipMaker.getPrev();
            tooltipMaker.addImage(spec.getSpriteName(), 50f, 50f, -53f);
            UIComponentAPI image = tooltipMaker.getPrev();
            image.getPosition().setXAlignOffset(3f);
            tooltipMaker.addImage(Util.eliteIcons.get(spec.getGoverningAptitudeId()),56f, 56f, -53f);
            UIComponentAPI eliteIcon = tooltipMaker.getPrev();
            eliteIcon.getPosition().setXAlignOffset(-3f);
            SkillFilter filter = new SkillFilter(spec);
            wrapped.setListener(new SkillFilterButtonListener(wrapped, image, eliteIcon, filter));
            if (i % rows == 0) {
                setTooltipPrev(tooltipMaker, null);
                setTooltipHeight(tooltipMaker, 0f);
            } else {
                setTooltipPrev(tooltipMaker, oldPrev);
                maxHeight = Math.max(tooltipMaker.getHeightSoFar(), maxHeight);
            }
            try {
                addTooltipAbove.invoke(
                        null,
                        button,
                        StandardTooltipV2.createSkillTooltip(
                                (SkillSpec) spec,
                                (CharacterStats) randomPerson.getStats(),
                                800f,
                                300f,
                                true,
                                false,
                                0,
                                null));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            if (activeFilters.containsValue(filter)) {
                SkillFilter active = (SkillFilter) activeFilters.get(filter);
                if (active.isElite()) {
                    filter.setElite(true);
                }
                setComponentOpacity(image, 1f);
                setComponentOpacity(eliteIcon, active.isElite() ? 1f : 0f);
                button.setChecked(true);
            }
            else {
                setComponentOpacity(image, 0.25f);
                setComponentOpacity(eliteIcon, 0f);
                button.setChecked(false);
            }
            buttonMap.put(button, filter);
        }
        setTooltipHeight(tooltipMaker, maxHeight);

        TooltipMakerAPI tagMaker = customPanel.createUIElement(280f, 462f, true);

        // Pre-defined tags: personality
        // No personality enum so have to do this manually
        List<ButtonAPI> personalityButtons = new ArrayList<>();
        for (PersonalityFilter pFilter : Util.personalityFilters) {
            ButtonAPI button = tagMaker.addAreaCheckbox(
                    pFilter.getDisplayName(),
                    null,
                    Misc.getBasePlayerColor(),
                    Misc.getDarkPlayerColor(),
                    Misc.getHighlightColor(),
                    260f,
                    30f,
                    10f);
            button.setChecked(activeFilters.containsValue(pFilter));
            personalityButtons.add(button);
        }

        for (ButtonAPI button : personalityButtons) {
            new Button(button).setListener(new PersonalityFilterButtonListener(personalityButtons));
        }

        for (int j = 0; j < Util.personalityFilters.size(); j++) {
            buttonMap.put(personalityButtons.get(j), Util.personalityFilters.get(j));
        }

        // Pre-defined-tags: innate tags
        InnateTagFilter lvl7CryopodFilter = new InnateTagFilter("$exceptionalSleeperPodOfficer");
        ButtonAPI podButton = tagMaker.addAreaCheckbox(
                "Exceptional pod officer",
                null,
                Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(),
                Misc.getHighlightColor(),
                260f,
                30f,
                10f);
        podButton.setChecked(activeFilters.containsValue(lvl7CryopodFilter));
        buttonMap.put(podButton, lvl7CryopodFilter);

        // Custom tags
        Set<String> allTags = Util.getAllTags();
        for (String str : allTags) {
            Color textColor = Settings.PERSISTENT_OFFICER_TAGS.contains(str)
                    ? Color.WHITE
                    : Misc.getBrightPlayerColor();
            ButtonAPI button = tagMaker.addAreaCheckbox(
                    str,
                    null,
                    Misc.getBasePlayerColor(),
                    Misc.getDarkPlayerColor(),
                    textColor,
                    260f,
                    30f,
                    10f);
            CustomTagFilter filter = new CustomTagFilter(str);
            button.setChecked(activeFilters.containsValue(filter));
            buttonMap.put(button, filter);
        }

        ConfirmFilterOfficers confirmListener = new ConfirmFilterOfficers(dialog, buttonMap, injector);
        UtilReflection.ConfirmDialogData data = UtilReflection.showConfirmationDialog(
                "Select filters: ",
                "Confirm",
                "Cancel",
                450f,
                600f,
                confirmListener);
        if (data == null) {
            return;
        }

        customPanel.addUIElement(tooltipMaker).inTL(10f, 0f);
        customPanel.addUIElement(tagMaker).inTL(140f, 0f);
        data.panel.addComponent(customPanel).inTL(10f, 50f);
    }
}
