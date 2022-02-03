package officerextension.listeners;

import com.fs.starfarer.api.ui.ButtonAPI;

import java.util.List;

public class PersonalityFilterButtonListener extends ActionListener {

    private final List<ButtonAPI> allButtons;

    public PersonalityFilterButtonListener(List<ButtonAPI> allButtons) {
        this.allButtons = allButtons;
    }

    @Override
    public void trigger(Object... args) {
        // Only allow one personality button to be selected at a time,
        // since officers can only have one personality

        ButtonAPI thisButton = (ButtonAPI) args[1];

        if (!thisButton.isChecked()) {
            return;
        }

        for (ButtonAPI button : allButtons) {
            button.setChecked(thisButton == button);
        }
    }
}
