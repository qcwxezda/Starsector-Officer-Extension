package officerextension.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.SleeperPodsSpecial;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

public class LocateExceptionalOfficers implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            for (SectorEntityToken entity : system.getAllEntities()) {
                if (getExceptionalOfficerId(entity) == null) continue;
                Console.showMessage(system.getName() + ": " + entity.getFullName() + ", " + entity.getLocation());
            }
        }
        return CommandResult.SUCCESS;
    }

    public static String getExceptionalOfficerId(SectorEntityToken entity) {
        MemoryAPI memory = entity.getMemoryWithoutUpdate();
        if (memory == null || !memory.contains(MemFlags.SALVAGE_SPECIAL_DATA)) {
            return null;
        }
        Object o = memory.get(MemFlags.SALVAGE_SPECIAL_DATA);
        if (!(o instanceof SleeperPodsSpecial.SleeperPodsSpecialData data)) return null;
        if (data.officer == null) return null;
        MemoryAPI officerMemory = data.officer.getMemoryWithoutUpdate();
        return officerMemory != null && officerMemory.getBoolean(MemFlags.EXCEPTIONAL_SLEEPER_POD_OFFICER) ? data.officer.getId() : null;
    }
}
