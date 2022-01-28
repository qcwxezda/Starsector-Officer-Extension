package officerextension.ui;

import com.fs.starfarer.api.ui.ButtonAPI;
import officerextension.ClassRefs;
import officerextension.Util;
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

    private static String graphicsObjectGetterName;
    private static String textGetterName;

    public String getText() {
        try {
            Object renderer = Util.invokeGetter(inner, "getRenderer");
            Object graphicsObject = null;
            if (graphicsObjectGetterName != null) {
                graphicsObject = Util.invokeGetter(renderer, graphicsObjectGetterName);
            }
            else {
                for (Method method : renderer.getClass().getDeclaredMethods()) {
                    Package pack = method.getReturnType().getPackage();
                    if (pack != null && pack.getName().startsWith("com.fs.graphics")) {
                        graphicsObjectGetterName = method.getName();
                        graphicsObject = method.invoke(renderer);
                    }
                }
            }
            if (graphicsObject == null) {
                throw new RuntimeException("Renderer's graphics object not found");
            }

            if (textGetterName != null) {
                return (String) Util.invokeGetter(graphicsObject, textGetterName);
            }
            for (Method method : graphicsObject.getClass().getDeclaredMethods()) {
                if (String.class.isAssignableFrom(method.getReturnType())) {
                    textGetterName = method.getName();
                    return (String) method.invoke(graphicsObject);
                }
            }
            throw new RuntimeException("Text label for button object not found");
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
