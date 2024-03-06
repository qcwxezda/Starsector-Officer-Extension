package officerextension.ui;

import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import java.lang.reflect.Method;

public class UIComponent implements Renderable {

    protected final Object inner;
    private static Method setOpacityMethod;

    /** [o] should be a renderable UI element, in particular it should implement UIComponentAPI
     *  and have the setOpacity method. */
    public UIComponent(Object o) {
        inner = o;
        if (setOpacityMethod == null) {
            try {
                setOpacityMethod = inner.getClass().getMethod("setOpacity", float.class);
            } catch (Exception e) {
                throw new RuntimeException("Renderable UI element's setOpacity method not found");
            }
        }
    }

    public void setOpacity(float amount) {
        try {
            setOpacityMethod.invoke(inner, amount);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PositionAPI getPosition() {
        return ((UIComponentAPI) inner).getPosition();
    }

    @Override
    public Object getInstance() {
        return inner;
    }
}
