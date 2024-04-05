package officerextension.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import officerextension.ExceptionalOfficerChecker;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

public class LocateExceptionalOfficers implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            for (SectorEntityToken entity : system.getAllEntities()) {
                if (ExceptionalOfficerChecker.getExceptionalOfficerId(entity) == null) continue;
                Console.showMessage(system.getName() + ": " + entity.getFullName() + ", " + entity.getLocation());
            }
        }
        return CommandResult.SUCCESS;
    }
}
