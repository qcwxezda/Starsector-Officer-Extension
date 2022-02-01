package officerextension.listeners;

import com.fs.starfarer.api.ui.UIComponentAPI;
import officerextension.ui.Button;

import java.lang.reflect.Method;

public class CustomAreaCheckBoxListener extends ActionListener {

    private final Button checkBox;
    private final UIComponentAPI attachedImage;

    public CustomAreaCheckBoxListener(Button checkBox, UIComponentAPI attachedImage) {
        this.checkBox = checkBox;
        this.attachedImage = attachedImage;
    }


    @Override
    public void trigger(Object... args) {
        try {
            Method setOpacity = attachedImage.getClass().getMethod("setOpacity", float.class);
            setOpacity.invoke(attachedImage, checkBox.getInstance().isChecked() ? 1.0f : 0.25f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
