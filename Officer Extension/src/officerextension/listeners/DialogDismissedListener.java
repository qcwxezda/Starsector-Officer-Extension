package officerextension.listeners;

import officerextension.ClassRefs;

public abstract class DialogDismissedListener extends ProxyTrigger {
    public DialogDismissedListener() {
        super(ClassRefs.dialogDismissedInterface, "dialogDismissed");
    }
}
