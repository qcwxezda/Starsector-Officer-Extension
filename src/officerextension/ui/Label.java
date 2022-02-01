package officerextension.ui;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Label implements Renderable {

    private final LabelAPI inner;
    private static Method createMethod;
    private static Method createSmallInsigniaMethod;
    private static Method setHighlightOnMouseoverMethod;
    private static Field tooltipField;
    private static Method setTooltipMethod;
    private static Method autoSizeMethod;

    public Label(LabelAPI o) {
        inner = o;
        if (createMethod == null) {
            try {
                createMethod = inner.getClass().getMethod("create", String.class);
            }
            catch (Exception e) {
                throw new RuntimeException("LabelAPI's create method not found");
            }
        }
        if (createSmallInsigniaMethod == null) {
            try {
                createSmallInsigniaMethod = inner.getClass().getMethod("createSmallInsigniaLabel", String.class, Alignment.class);
            }
            catch (Exception e) {
                throw new RuntimeException("LabelAPI's createSmallInsigniaLabel method not found");
            }
        }
        if (setHighlightOnMouseoverMethod == null) {
            try {
                setHighlightOnMouseoverMethod = inner.getClass().getMethod("setHighlightOnMouseover", boolean.class);
            }
            catch (Exception e) {
                throw new RuntimeException("LabelAPI's setHighlightOnMouseover method not found");
            }
        }
        if (tooltipField == null) {
            try {
                for (Field field : inner.getClass().getDeclaredFields()) {
                    // Look for an interface with the "notifyShown" method
                    if (field.getType().isInterface()) {
                        try {
                            field.getType().getMethod("notifyShown");
                            // Past here, we know we found the field we're looking for
                            tooltipField = field;
                        }
                        catch (NoSuchMethodException e) {
                            // continue
                        }
                    }
                }
                if (tooltipField == null) {
                    throw new RuntimeException("LabelAPI's tooltip field not found");
                }
            }
            catch (Exception e) {
                throw new RuntimeException("LabelAPI's tooltip field not found");
            }
        }
        if (setTooltipMethod == null) {
            try {
                setTooltipMethod = inner.getClass().getMethod("setTooltip", float.class, tooltipField.getType());
            }
            catch (Exception e) {
                throw new RuntimeException("LabelAPI's setTooltip method not found");
            }
        }
        if (autoSizeMethod == null) {
            try {
                autoSizeMethod = inner.getClass().getMethod("autoSize");
            }
            catch (Exception e) {
                throw new RuntimeException("LabelAPI's autoSize method not found");
            }
        }
    }

    public Label create(String text) {
        try {
            return new Label((LabelAPI) createMethod.invoke(null, text));
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Label createSmallInsigniaLabel(String text, Alignment alignment) {
        try {
            return new Label((LabelAPI) createSmallInsigniaMethod.invoke(null, text, alignment));
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void autoSize() {
        try {
            autoSizeMethod.invoke(inner);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeTooltip() {
        try {
            setTooltipMethod.invoke(inner, 0f, null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setHighlightOnMouseover(boolean value) {
        try {
            setHighlightOnMouseoverMethod.invoke(inner, value);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public LabelAPI getInstance() {
        return inner;
    }
}
