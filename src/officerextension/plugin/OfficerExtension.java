package officerextension.plugin;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.SkillsChangeOfficerEffect;
import officerextension.*;
import officerextension.campaign.ModifiedSkillsChangeOfficerEffect;
import org.apache.log4j.Logger;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.List;

@SuppressWarnings("unused")
public class OfficerExtension extends BaseModPlugin {

    private static final Logger logger = Global.getLogger(OfficerExtension.class);

    private static final String[] reflectionWhitelist = new String[] {
            "officerextension.CoreScript",
            "officerextension.FleetListener",
            "officerextension.ClassRefs",
            "officerextension.UtilReflection",
            "officerextension.ui",
            "officerextension.FleetPanelInjector",
            "officerextension.listeners"
    };

    @Override
    public void onApplicationLoad() {
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            LunaLibSettingsListener.init();
        }
        else {
            Settings.load();
        }
    }

    @Override
    public void onGameLoad(boolean newGame) {
        URL url;
        try {
            url = getClass().getProtectionDomain().getCodeSource().getLocation();
        }
        catch (SecurityException e) {
            try {
                url = Paths.get("../mods/Officer Extension/jars/Officer Extension.jar").toUri().toURL();
            } catch (Exception ex) {
                logger.error("Could not convert jar path to URL; exiting", ex);
                return;
            }
        }

        ListenerManagerAPI listeners = Global.getSector().getListenerManager();

        DialogHandler dialogHandler = new DialogHandler();
        listeners.addListener(dialogHandler, true);
        Global.getSector().addTransientListener(dialogHandler);
        Global.getSector().addTransientScript(dialogHandler);

        @SuppressWarnings("resource") ClassLoader cl = new ReflectionEnabledClassLoader(url, getClass().getClassLoader());
        try {
            Global.getSector().addTransientScript(
                    (EveryFrameScript) cl.loadClass("officerextension.CoreScript").newInstance());
        } catch (Exception e) {
            logger.error("Failure to load core script class; exiting", e);
            return;
        }

        Global.getSector().addTransientListener(new EconomyListener(false));
        Global.getSector().addTransientListener(new FleetListener());
        Global.getSector().addTransientListener(new ExceptionalOfficerChecker());

        listeners.removeListenerOfClass(SkillsChangeOfficerEffect.class);
        listeners.addListener(new ModifiedSkillsChangeOfficerEffect(), true);

        // If somehow the player managed to save while the game was paused, add suspended officers back into the player's fleet
        @SuppressWarnings("unchecked")
        List<OfficerDataAPI> suspendedOfficers = (List<OfficerDataAPI>) Global.getSector().getPersistentData().get(Settings.SUSPENDED_OFFICERS_DATA_KEY);
        if (suspendedOfficers != null) {
            for (OfficerDataAPI officer : suspendedOfficers) {
                Global.getSector().getPlayerFleet().getFleetData().addOfficer(officer);
            }
            Global.getSector().getPersistentData().remove(Settings.SUSPENDED_OFFICERS_DATA_KEY);
        }
    }

    public static class ReflectionEnabledClassLoader extends URLClassLoader {

        public ReflectionEnabledClassLoader(URL url, ClassLoader parent) {
            super(new URL[] {url}, parent);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.startsWith("java.lang.reflect")) {
                return ClassLoader.getSystemClassLoader().loadClass(name);
            }
            return super.loadClass(name);
        }

        @Override
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                return c;
            }
            // Be the defining classloader for all classes in the reflection whitelist
            // For classes defined by this loader, classes in java.lang.reflect will be loaded directly
            // by the system classloader, without the intermediate delegations.
            for (String str : reflectionWhitelist) {
                if (name.startsWith(str)) {
                    return findClass(name);
                }
            }
            return super.loadClass(name, resolve);
        }
    }
}
