package officerextension.ui;

import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import officerextension.UtilReflection;

import java.lang.reflect.Field;

public class SkillButton extends Button {

    private SkillSpecAPI skillSpec;
    private boolean selected = false;

    /** Belongs to the tooltip; i.e. getTooltip().[skillSpecField]*/
    private static String skillSpecField;

    /** [o] should be an instance of the underlying obfuscated skill button */
    public SkillButton(ButtonAPI o) {
        super(o);
    }

    public boolean isSelected() {
        return selected;
    }

    public void toggleSelect() {
        selected = !selected;
        setOpacity(selected ? 0.2f : 1f);
    }

    public SkillSpecAPI getSkillSpec() {
        Object tooltip = UtilReflection.invokeGetter(inner, "getTooltip");
        // Already know the field that retrieves the skill spec
        if (skillSpecField != null) {
            skillSpec = (SkillSpecAPI) UtilReflection.getField(tooltip, skillSpecField);
        }
        // Have to search through all fields
        else {
            for (Field field : tooltip.getClass().getDeclaredFields()) {
                if (SkillSpecAPI.class.isAssignableFrom(field.getType())) {
                    skillSpecField = field.getName();
                    skillSpec = (SkillSpecAPI) UtilReflection.getField(tooltip, field.getName());
                }
            }
        }
        // Not found
        if (skillSpec == null) {
            throw new RuntimeException("Skill spec for skill button not found!");
        }
        return skillSpec;
    }
}
