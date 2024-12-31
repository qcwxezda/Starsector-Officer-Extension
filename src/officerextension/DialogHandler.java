package officerextension;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DialogHandler extends BaseCampaignEventListener implements EveryFrameScript {
    public DialogHandler() {
        super(false);
    }
    public static final String officerNumberId = "officerextension_TempOfficerCount";
    private boolean modified = false;

    @Override
    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
        // Remove all suspended officers from the player's fleet. Reason: suspended officers shouldn't affect
        // XP calculation.
        tempRemoveSuspendedOfficers();
        // Set player max officers to some high number temporarily
        Global.getSector().getPlayerPerson().getStats().getOfficerNumber().modifyFlat(officerNumberId, 999999);
        modified = true;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        if (Global.getSector().getPersistentData().containsKey(Settings.SUSPENDED_OFFICERS_DATA_KEY)) {
            addSuspendedOfficersBack();
        }
        if (modified) {
            Global.getSector().getPlayerPerson().getStats().getOfficerNumber().unmodify(officerNumberId);
            modified = false;
        }
    }

    public void addSuspendedOfficersBack() {
        //noinspection unchecked
        List<OfficerDataAPI> tempSuspendedOfficers =
                (List<OfficerDataAPI>) Global.getSector().getPersistentData().remove(Settings.SUSPENDED_OFFICERS_DATA_KEY);
        if (tempSuspendedOfficers == null) return;
        for (OfficerDataAPI data : tempSuspendedOfficers) {
            Global.getSector().getPlayerFleet().getFleetData().addOfficer(data);
        }
    }

    public void tempRemoveSuspendedOfficers() {
        //noinspection unchecked
        List<OfficerDataAPI> tempSuspendedOfficers =
                (List<OfficerDataAPI>) Global.getSector().getPersistentData().get(Settings.SUSPENDED_OFFICERS_DATA_KEY);
        if (tempSuspendedOfficers == null) tempSuspendedOfficers = new ArrayList<>();
        Set<OfficerDataAPI> existing = new HashSet<>(tempSuspendedOfficers);

        FleetDataAPI playerFleetData = Global.getSector().getPlayerFleet().getFleetData();
        for (OfficerDataAPI officer : playerFleetData.getOfficersCopy()) {
            if (officer.getPerson().hasTag(Settings.OFFICER_IS_SUSPENDED_KEY)) {
                playerFleetData.removeOfficer(officer.getPerson());
                if (!existing.contains(officer)) {
                    tempSuspendedOfficers.add(officer);
                }
            }
        }
        // Add to save file temporarily in case the player somehow manages to save while the game is paused
        Global.getSector().getPersistentData().put(Settings.SUSPENDED_OFFICERS_DATA_KEY, tempSuspendedOfficers);
    }
}
