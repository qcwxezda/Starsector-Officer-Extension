package officerextension.listeners;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.TextFieldAPI;
import officerextension.Util;
import officerextension.ui.OfficerUIElement;

import java.util.*;

public class ConfirmEditTags extends DialogDismissedListener {

    private final OfficerUIElement uiElement;
    private final Map<String, ButtonAPI> buttonMap;

    public ConfirmEditTags(OfficerUIElement elem, Map<String, ButtonAPI> buttonMap) {
        uiElement = elem;
        this.buttonMap = buttonMap;
    }

    @Override
    public void trigger(Object... args) {
        // The second argument is 0 if confirmed, 1 if canceled
        int option = (int) args[1];
        if (option == 1) {
            return;
        }

        Set<String> tags = new TreeSet<>();
        for (Map.Entry<String, ButtonAPI> entry : buttonMap.entrySet()) {
          if (entry.getValue().isChecked()) {
              tags.add(entry.getKey());
          }
        }
        Util.updateOfficerTags(uiElement.getOfficerData(), tags);
    }
}
