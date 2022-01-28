package officerextension.ui;

import com.fs.starfarer.api.ui.LabelAPI;

import java.lang.reflect.Method;

public class Label {

    private final LabelAPI inner;

    public Label(LabelAPI inner) {
        this.inner = inner;
    }

    public LabelAPI create(String text) {
        try {
            Method createMethod = inner.getClass().getMethod("create", String.class);
            return (LabelAPI) createMethod.invoke(null, text);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
