package officerextension.listeners;

import com.fs.starfarer.api.ui.UIComponentAPI;
import officerextension.filter.SkillFilter;
import officerextension.ui.Button;

import java.lang.reflect.Method;

public class SkillFilterButtonListener extends ActionListener {

    private final Button checkBox;
    private final UIComponentAPI attachedImage;
    private final UIComponentAPI eliteIcon;
    private final SkillFilter filter;

    public SkillFilterButtonListener(Button checkBox, UIComponentAPI attachedImage, UIComponentAPI eliteIcon, SkillFilter filter) {
        this.checkBox = checkBox;
        this.attachedImage = attachedImage;
        this.eliteIcon = eliteIcon;
        this.filter = filter;
    }


    @Override
    public void trigger(Object... args) {
        try {
            Method setOpacity = attachedImage.getClass().getMethod("setOpacity", float.class);
            Method getOpacity = attachedImage.getClass().getMethod("getOpacity");
            float eliteIconOpacity = (float) getOpacity.invoke(eliteIcon);
            // Insert the additional state "on: elite" in between on and off
            if (eliteIconOpacity == 0f && !checkBox.getInstance().isChecked()) {
                checkBox.getInstance().setChecked(true);
                setOpacity.invoke(eliteIcon, 1f);
                filter.setElite(true);
            }
            else {
                setOpacity.invoke(eliteIcon, 0f);
                filter.setElite(false);
            }
            setOpacity.invoke(attachedImage, checkBox.getInstance().isChecked() ? 1.0f : 0.25f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
