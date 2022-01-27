package officerextension.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.util.Misc;
import officerextension.Settings;
import officerextension.Util;
import officerextension.ui.Button;
import officerextension.ui.OfficerUIElement;
import officerextension.ui.SkillButton;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ForgetSkills extends ActionListener {

    private final OfficerUIElement uiElement;

    public ForgetSkills(OfficerUIElement uiElement) {
        this.uiElement = uiElement;
    }

    @Override
    public void trigger(Object... args) {
        PersonAPI officerPerson = uiElement.getOfficerData().getPerson();
        int numForgetting = 0;
        for (SkillButton button : uiElement.getWrappedSkillButtons()) {
            if (button.isSelected()) {
                numForgetting++;
            }
        }
        StringBuilder confirmSB = new StringBuilder();
        List<String> highlights = new ArrayList<>();
        List<Color> colors = new ArrayList<>();
        confirmSB.append("Are you sure? ")
                .append(officerPerson.getNameString())
                .append(" (level ")
                .append(officerPerson.getStats().getLevel())
                .append(") will be demoted to level ")
                .append(officerPerson.getStats().getLevel() - numForgetting)
                .append(" and will permanently forget the following skills: \n\n");
        for (SkillButton button : uiElement.getWrappedSkillButtons()) {
            if (button.isSelected()) {
                SkillSpecAPI spec = button.getSkillSpec();
                confirmSB.append("        - ")
                        .append(spec.getName());
                highlights.add(spec.getName());
                colors.add(spec.getGoverningAptitudeColor());
                if (officerPerson.getStats().getSkillLevel(button.getSkillSpec().getId()) > 1) {
                    confirmSB.append(" (elite)");
                    highlights.add("(elite)");
                    colors.add(Misc.getStoryOptionColor());
                }
                confirmSB.append("\n");
            }
        }
        String bonusXPPercent = (int) (100f * Settings.DEMOTE_BONUS_XP_FRACTION) + "%";
        int numStoryPoints = Global.getSector().getPlayerStats().getStoryPoints();
        String storyPointOrPoints = numStoryPoints == 1 ? "story point" : "story points";
        confirmSB.append("\n")
                .append("Demoting an officer requires a story point and grants ")
                .append(bonusXPPercent)
                .append(" bonus experience.\n\n")
                .append("You have ")
                .append(numStoryPoints)
                .append(" ")
                .append(storyPointOrPoints)
                .append(".");
        highlights.add("story point");
        colors.add(Misc.getStoryOptionColor());
        highlights.add(bonusXPPercent);
        colors.add(Misc.getStoryOptionColor());
        highlights.add("" + numStoryPoints);
        colors.add(numStoryPoints >= 1 ? Misc.getStoryOptionColor() : Misc.getNegativeHighlightColor());
        ConfirmForgetSkills confirmListener = new ConfirmForgetSkills(uiElement);
        Util.ConfirmDialogData data = Util.showConfirmationDialog(
                confirmSB.toString(),
                "Forget",
                "Never mind",
                650f,
                250f + 20f * numForgetting,
                confirmListener);
        if (data == null) {
            return;
        }
        LabelAPI label = data.textLabel;
        label.setHighlight(highlights.toArray(new String[0]));
        label.setHighlightColors(colors.toArray(new Color[0]));
        Button yesButton = data.confirmButton;
        if (numStoryPoints < 1) {
            yesButton.setEnabled(false);
            yesButton.setActive(false);
        }
    }
}