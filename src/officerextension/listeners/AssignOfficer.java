package officerextension.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import officerextension.Util;
import officerextension.ui.OfficerUIElement;

import java.lang.reflect.Method;

public class AssignOfficer extends ActionListener {

    private final OfficerUIElement uiElement;
    private final Object originalListener;

    /** Orig should implement the obfuscated "ActionListener" class that has the [actionPerformed] method */
    public AssignOfficer(OfficerUIElement elem, Object orig) {
        uiElement = elem;
        originalListener = orig;
    }

    @Override
    public void trigger(Object... args) {

        // Cancel the action if the officer is suspended
        if (Util.isSuspended(uiElement.getOfficerData())) {
            Global.getSector().getCampaignUI().getMessageDisplay().addMessage("Cannot assign suspended officer", Misc.getNegativeHighlightColor());
            return;
        }

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        FleetMemberAPI originalFlagship = playerFleet.getFlagship();
        PersonAPI originalOfficer = uiElement.getFleetMember().getCaptain();
        try {
            Method origActionPerformed = originalListener.getClass().getMethod("actionPerformed", Object.class, Object.class);
            origActionPerformed.invoke(originalListener, args[0], args[1]);

            // If the assignment caused us to go past our officer limit, then undo it
            int maxOfficers = Util.getMaxPlayerOfficers();
            if (Util.countAssignedNonMercOfficers(playerFleet) > maxOfficers) {
                // If the original officer was a mercenary, set it back to the mercenary (mercenaries don't count against limit)
                // Otherwise unset the officer
                uiElement.getFleetMember().setCaptain(
                        Misc.isMercenary(originalOfficer) ? originalOfficer : Global.getSettings().createPerson());
                // Set the player back to their original flagship (the game will set a flagship if it sees that
                // the player doesn't have one, we need to undo that too)
                playerFleet.getFleetData().setFlagship(originalFlagship);
                Global.getSector().getCampaignUI().getMessageDisplay().addMessage("Officer limit reached", Misc.getNegativeHighlightColor());
            }

            uiElement.getInjector().updateNumOfficersLabel();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
