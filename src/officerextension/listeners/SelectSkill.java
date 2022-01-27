package officerextension.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Misc;
import officerextension.ui.OfficerUIElement;
import officerextension.ui.SkillButton;

public class SelectSkill extends ActionListener {

    private final SkillButton button;
    private final OfficerUIElement uiElement;

    public SelectSkill(SkillButton button, OfficerUIElement uiElement) {
        this.button = button;
        this.uiElement = uiElement;
    }

    @Override
    public void trigger(Object... args)  {
        // If every button is selected, cancel the selection -- officer cannot forget
        // his last skill
        boolean selectedEvery = true;
        for (SkillButton otherButton : uiElement.getWrappedSkillButtons()) {
            if (!otherButton.isSelected() && button != otherButton) {
                selectedEvery = false;
                break;
            }
        }
        if (selectedEvery) {
            Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                    "Officers cannot be demoted below level 1",
                    Misc.getNegativeHighlightColor());
        } else {
            button.toggleSelect();
        }
        uiElement.updateButtonVisibility();
    }
}
