package officerextension;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.ui.*;
import officerextension.ui.Button;
import officerextension.listeners.ActionListener;
import officerextension.listeners.DialogDismissedListener;
import sun.reflect.misc.MethodUtil;

import java.util.ArrayList;
import java.util.List;

import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Util {

    @SuppressWarnings("unchecked")
    public static void addSuspendedOfficer(OfficerDataAPI officer) {
        List<OfficerDataAPI> suspendedOfficers = (List<OfficerDataAPI>) Global.getSector().getPersistentData().get(Settings.SUSPENDED_OFFICERS_DATA_KEY);
        if (suspendedOfficers == null) {
            suspendedOfficers = new ArrayList<>();
            Global.getSector().getPersistentData().put(Settings.SUSPENDED_OFFICERS_DATA_KEY, suspendedOfficers);
        }
        suspendedOfficers.add(officer);
    }

    @SuppressWarnings("unchecked")
    public static void removeSuspendedOfficer(OfficerDataAPI officer) {
        List<OfficerDataAPI> suspendedOfficers = (List<OfficerDataAPI>) Global.getSector().getPersistentData().get(Settings.SUSPENDED_OFFICERS_DATA_KEY);
        if (suspendedOfficers == null) {
            return;
        }
        suspendedOfficers.remove(officer);
        if (suspendedOfficers.isEmpty()) {
            Global.getSector().getPersistentData().remove(Settings.SUSPENDED_OFFICERS_DATA_KEY);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<OfficerDataAPI> getSuspendedOfficers() {
        if (!Global.getSector().getPersistentData().containsKey(Settings.SUSPENDED_OFFICERS_DATA_KEY)) {
            return new ArrayList<>();
        }
        return (List<OfficerDataAPI>) Global.getSector().getPersistentData().get(Settings.SUSPENDED_OFFICERS_DATA_KEY);
    }

    public static boolean isSuspended(OfficerDataAPI officer) {
        if (!Global.getSector().getPersistentData().containsKey(Settings.SUSPENDED_OFFICERS_DATA_KEY)) {
            return false;
        }
        return ((List<?>) Global.getSector().getPersistentData().get(Settings.SUSPENDED_OFFICERS_DATA_KEY)).contains(officer);
    }

    public static Button makeButton(String text, ActionListener handler, Color base, Color bg, float width, float height) {
        CustomPanelAPI dummyPanel = Global.getSettings().createCustom(0f, 0f, null);
        TooltipMakerAPI dummyTooltipMaker = dummyPanel.createUIElement(0f, 0f, false);
        Button button = new Button(dummyTooltipMaker.addButton(text, null, base, bg, width, height, 0f));
        button.setListener(handler);
        return button;
    }

    public static class ConfirmDialogData {
        public LabelAPI textLabel;
        public Button confirmButton;
        public Button cancelButton;

        public ConfirmDialogData(LabelAPI label, Button yes, Button no) {
            textLabel = label;
            confirmButton = yes;
            cancelButton = no;
        }
    }

    /** Returns the LabelAPI for the text inside the confirmation dialog */
    public static ConfirmDialogData showConfirmationDialog(
            String text,
            String confirmText,
            String cancelText,
            float width,
            float height,
            DialogDismissedListener dialogListener) {
        try {
            Constructor<?> cons = ClassRefs.confirmDialogClass
                    .getConstructor(
                            float.class,
                            float.class,
                            ClassRefs.uiPanelClass,
                            ClassRefs.dialogDismissedInterface,
                            String.class,
                            String[].class);
            Object confirmDialog = cons.newInstance(
                    width,
                    height,
                    getField(Global.getSector().getCampaignUI(), "screenPanel"),
                    dialogListener.getProxy(),
                    text,
                    new String[]{confirmText, cancelText}
            );
            Method show = confirmDialog.getClass().getMethod("show", float.class, float.class);
            show.invoke(confirmDialog, 0.25f, 0.25f);
            LabelAPI label = (LabelAPI) invokeGetter(confirmDialog, "getLabel");
            Button yes = new Button((ButtonAPI) invokeGetter(confirmDialog, "getButton", 0));
            Button no = new Button((ButtonAPI) invokeGetter(confirmDialog, "getButton", 1));
            return new ConfirmDialogData(label, yes, no);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object getField(Object o, String fieldName) {
        if (o == null) return null;
        try {
            Field field = o.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(o);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object invokeGetter(Object o, String methodName, Object... args) {
        if (o == null) return null;
        try {
            Class<?>[] argClasses = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                argClasses[i] = args[i].getClass();
                // unbox
                if (argClasses[i] == Integer.class) {
                    argClasses[i] = int.class;
                }
                else if (argClasses[i] == Boolean.class) {
                    argClasses[i] = boolean.class;
                }
                else if (argClasses[i] == Float.class) {
                    argClasses[i] = float.class;
                }
            }
            Method method = o.getClass().getMethod(methodName, argClasses);
            return method.invoke(o, args);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void printClassLoaderHierarchy(ClassLoader classLoader) {
        while (classLoader != null) {
            System.out.println(classLoader);
            classLoader = classLoader.getParent();
        }
    }
}