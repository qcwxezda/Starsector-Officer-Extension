package officerextension;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.characters.PersonAPI;

public class FleetListener extends BaseCampaignEventListener {

    public FleetListener(boolean permaRegister) {
        super(permaRegister);
    }

    @Override
    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
        SectorEntityToken target = dialog.getInteractionTarget();
        if (!(target instanceof CampaignFleetAPI)) {
            return;
        }

        CampaignFleetAPI fleet = (CampaignFleetAPI) target;
        PersonAPI commander = fleet.getCommander();

        if (commander == null || commander.isPlayer()) {
            return;
        }

        TextPanelAPI textPanel = dialog.getTextPanel();
        textPanel.setFontSmallInsignia();
        if (commander.getStats().getLevel() >= 1) {
            textPanel.addPara("The fleet's commander, "
                            + commander.getNameString() +
                            " (level " +
                            commander.getStats().getLevel() +
                            "), possesses the following combat skills: ");
            textPanel.addSkillPanel(commander, false);
        }
        textPanel.setFontInsignia();
    }
}
