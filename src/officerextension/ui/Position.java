package officerextension.ui;

import com.fs.starfarer.api.ui.PositionAPI;

import java.lang.reflect.Method;

public class Position {

    private final PositionAPI inner;
    private static Method setMethod;

    public Position(PositionAPI o) {
        inner = o;
        if (setMethod == null) {
            try {
                setMethod = inner.getClass().getMethod("set", inner.getClass());
            }
            catch (Exception e) {
                throw new RuntimeException("PositionAPI's set method not found");
            }
        }
    }

    public void set(PositionAPI copy) {
        try {
            setMethod.invoke(inner, copy);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PositionAPI getInstance() {
        return inner;
    }
}
