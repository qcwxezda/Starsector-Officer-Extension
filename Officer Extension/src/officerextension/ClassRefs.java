package officerextension;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** Stores references to class objects in the obfuscated game files */
public class ClassRefs {
    /** The class that CampaignUIAPI.showConfirmDialog instantiates. We need this because showConfirmDialog doesn't work
     *  if any core UI is open. */
    public static Class<?> confirmDialogClass;
    /** Interface that contains a single method: actionPerformed */
    public static Class<?> actionListenerInterface;
    /** Interface that contains a single method: dialogDismissed */
    public static Class<?> dialogDismissedInterface;
    /** Interface for renderable UI elements */
    public static Class<?> renderableUIElementInterface;
    /** Obfuscated UI panel class */
    public static Class<?> uiPanelClass;

    private static boolean foundAllClasses = false;

    public static void findConfirmDialogClass() {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        // If we don't know the confirmation dialog class, try to create a confirmation dialog in order to access it
        try {
            if (confirmDialogClass == null && campaignUI.showConfirmDialog("", "", "", null, null)) {
                boolean isPaused = Global.getSector().isPaused();
                Object screenPanel = UtilReflection.getField(campaignUI, "screenPanel");
                List<?> children = (List<?>) UtilReflection.invokeGetter(screenPanel, "getChildrenNonCopy");
                // the confirm dialog will be the last child
                Object panel = children.get(children.size() - 1);
                confirmDialogClass = panel.getClass();
                // we have the class, dismiss the dialog
                Method dismiss = confirmDialogClass.getMethod("dismiss", int.class);
                dismiss.invoke(panel, 0);
                Global.getSector().setPaused(isPaused);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void findUIPanelClass() {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        try {
            Field field = campaignUI.getClass().getDeclaredField("screenPanel");
            uiPanelClass = field.getType();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** [witness] needs to implement the renderable UI element interface */
    public static void findRenderableUIElementInterface(Object witness) {
        for (Class<?> cls : witness.getClass().getInterfaces()) {
            // Look for an interface that has the "render" method
            for (Method method : cls.getDeclaredMethods()) {
                if (method.getName().equals("render")) {
                    renderableUIElementInterface = cls;
                    return;
                }
            }
        }

        if (renderableUIElementInterface == null) {
            throw new RuntimeException("``Renderable'' interface not found; perhaps invalid witness used?");
        }
    }

    /** [witness] needs to implement the action listener interface */
    public static void findActionListenerInterface(Object witness) {
        actionListenerInterface = findInterfaceByMethod(witness.getClass().getInterfaces(), "actionPerformed");
    }

    /** [witness] needs to implement the dialog dismissed interface */
    public static void findDialogDismissedInterface(Object witness) {
        dialogDismissedInterface = findInterfaceByMethod(witness.getClass().getInterfaces(), "dialogDismissed");
    }

    public static void findAllClasses() {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        if (confirmDialogClass == null) {
            findConfirmDialogClass();
        }
        if (dialogDismissedInterface == null) {
            findDialogDismissedInterface(campaignUI);
        }
        if (actionListenerInterface == null) {
            findActionListenerInterface(campaignUI);
        }
        if (uiPanelClass == null) {
            findUIPanelClass();
        }
        if (renderableUIElementInterface == null) {
            findRenderableUIElementInterface(UtilReflection.getField(campaignUI, "screenPanel"));
        }

        if (confirmDialogClass != null
                && dialogDismissedInterface != null
                && actionListenerInterface != null
                && uiPanelClass != null
                && renderableUIElementInterface != null) {
            foundAllClasses = true;
        }
    }

    public static boolean foundAllClasses(){
        return foundAllClasses;
    }

    /** Tries to find an interface among [interfaces] that has [methodName] as its only method. */
    private static Class<?> findInterfaceByMethod(Class<?>[] interfaces, String methodName) {
        for (Class<?> cls : interfaces) {
            Method[] methods = cls.getDeclaredMethods();
            if (methods.length != 1) {
                continue;
            }
            Method method = methods[0];
            if (method.getName().equals(methodName)) {
                return cls;
            }
        }

        throw new RuntimeException("Interface with only method " + methodName + " not found; perhaps invalid witness used?");
    }
}
