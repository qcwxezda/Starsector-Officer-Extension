package officerextension.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.CharacterStats;
import com.fs.starfarer.coreui.CaptainPickerDialog;
import com.fs.starfarer.loading.SkillSpec;
import com.fs.starfarer.ui.impl.StandardTooltipV2;
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable;
import officerextension.CoreScript;
import officerextension.Util;
import officerextension.UtilReflection;
import officerextension.filter.OfficerFilter;
import officerextension.filter.SkillFilter;
import officerextension.filter.TagFilter;
import officerextension.ui.Button;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;

public class FilterOfficers extends ActionListener {

    private final CaptainPickerDialog dialog;
    private final CoreScript injector;

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
        StandardTooltipV2Expandable tooltipMaker = (StandardTooltipV2Expandable) customPanel.createUIElement(150f, 480f, true);
        Set<OfficerFilter> activeFilters = injector.getActiveFilters();
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
            tooltipMaker.addImage(spec.getSpriteName(), -53f);
            UIComponentAPI image = tooltipMaker.getPrev();
            wrapped.setListener(new CustomAreaCheckBoxListener(wrapped, image));
            image.getPosition().setSize(50f, 50f).setXAlignOffset(3f);
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
            SkillFilter filter = new SkillFilter(spec);
            if (activeFilters.contains(filter)) {
                setComponentOpacity(image, 1f);
                button.setChecked(true);
            }
            else {
                setComponentOpacity(image, 0.25f);
                button.setChecked(false);
            }
            buttonMap.put(button, filter);
        }
        setTooltipHeight(tooltipMaker, maxHeight);

        Set<String> allTags = Util.getAllTags();
        TooltipMakerAPI tagMaker = customPanel.createUIElement(280f, 480f, true);
        for (String str : allTags) {
            ButtonAPI button = tagMaker.addAreaCheckbox(
                    str,
                    null,
                    Misc.getBasePlayerColor(),
                    Misc.getDarkPlayerColor(),
                    Misc.getBrightPlayerColor(),
                    260f,
                    30f,
                    10f);
            TagFilter filter = new TagFilter(str);
            button.setChecked(activeFilters.contains(filter));
            buttonMap.put(button, filter);
        }

        ConfirmFilterOfficers confirmListener = new ConfirmFilterOfficers(dialog, buttonMap, injector);
        UtilReflection.ConfirmDialogData data = UtilReflection.showConfirmationDialog(
                "Select filters: ",
                "Confirm",
                "Cancel",
                allTags.isEmpty() ? 350f : 450f,
                600f,
                confirmListener);
        if (data == null) {
            return;
        }
        if (!allTags.isEmpty()) {
            customPanel.addUIElement(tooltipMaker).inLMid(10f);
            customPanel.addUIElement(tagMaker).inLMid(140f);
        }
        else {
            customPanel.addUIElement(tooltipMaker).inLMid(95f);
        }
        data.panel.addComponent(customPanel).inTL(10f, 50f);
    }
}
