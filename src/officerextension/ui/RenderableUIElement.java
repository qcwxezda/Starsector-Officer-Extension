package officerextension.ui;

import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import java.lang.reflect.Method;

public class RenderableUIElement {

    protected final Object inner;

    /** [o] should be a renderable UI element, in particular it should implement UIComponentAPI
     *  and have the setOpacity method. */
    public RenderableUIElement(Object o) {
        inner = o;
    }

    public void setOpacity(float amount) {
        try {
            Method setOpacity = inner.getClass().getMethod("setOpacity", float.class);
            setOpacity.invoke(inner, amount);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PositionAPI getPosition() {
        return ((UIComponentAPI) inner).getPosition();
    }
}
