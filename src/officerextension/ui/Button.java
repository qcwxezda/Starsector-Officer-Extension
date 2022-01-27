package officerextension.ui;

import com.fs.starfarer.api.ui.ButtonAPI;
import officerextension.ClassRefs;
import officerextension.listeners.ActionListener;

import java.lang.reflect.Method;

public class Button extends RenderableUIElement {

    /** [o] should be an instance of the underlying Button object */
    public Button(ButtonAPI o) {
        super(o);
    }

    public ButtonAPI getInstance() {
        return (ButtonAPI) inner;
    }

    public void setEnabled(boolean enabled) {
        try {
            Method setEnabled = inner.getClass().getMethod("setEnabled", boolean.class);
            setEnabled.invoke(inner, enabled);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setListener(ActionListener listener) {
        try {
            Method setListener = inner.getClass().getMethod("setListener", ClassRefs.actionListenerInterface);
            setListener.invoke(inner, listener.getProxy());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setButtonPressedSound(String soundId) {
        try {
            Method setSound = inner.getClass().getMethod("setButtonPressedSound", String.class);
            setSound.invoke(inner, soundId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setActive(boolean active) {
        try {
            Method setActive = inner.getClass().getMethod("setActive", boolean.class);
            setActive.invoke(inner, active);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
