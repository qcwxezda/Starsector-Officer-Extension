package officerextension.listeners;

import com.fs.starfarer.api.ui.TextFieldAPI;
import officerextension.Util;
import officerextension.ui.OfficerUIElement;

import java.util.Arrays;
import java.util.Locale;
import java.util.TreeSet;

public class ConfirmEditTags extends DialogDismissedListener {

    private final OfficerUIElement uiElement;
    private final TextFieldAPI textField;

    public ConfirmEditTags(OfficerUIElement elem, TextFieldAPI textField) {
        uiElement = elem;
        this.textField = textField;
    }

    @Override
    public void trigger(Object... args) {
        // The second argument is 0 if confirmed, 1 if canceled
        int option = (int) args[1];
        if (option == 1) {
            return;
        }
        String tagsStr = textField.getText();
        String[] tags = tagsStr.trim().toLowerCase(Locale.ROOT).split("\\s*,\\s*");
        Util.updateOfficerTags(uiElement.getOfficerData(), new TreeSet<>(Arrays.asList(tags)));
    }
}
