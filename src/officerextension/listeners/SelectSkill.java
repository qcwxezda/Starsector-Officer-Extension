package officerextension.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Misc;
import officerextension.Settings;
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
        // If officer can level up, force leveling up to maximum before demoting
        if (uiElement.getOfficerData().canLevelUp() && !button.isSelected()) {
            Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                    "Officer must be leveled up before demoting", Misc.getNegativeHighlightColor());
            return;
        }
        // If the skill is permanent, cancel the selection
        if (button.getSkillSpec().hasTag(Settings.SKILL_TAG_UNREMOVABLE) && !button.isSelected()) {
            Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                    "Skill \"" + button.getSkillSpec().getName() + "\" cannot be unlearned",
                    Misc.getNegativeHighlightColor());
            return;
        }
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
            return;
        }
        button.toggleSelect();
        uiElement.updateButtonVisibility();
    }
}
