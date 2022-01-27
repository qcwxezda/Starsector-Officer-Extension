package officerextension.listeners;

import officerextension.ClassRefs;

public abstract class ActionListener extends ProxyTrigger {
    public ActionListener() {
        super(ClassRefs.actionListenerInterface, "actionPerformed");
    }
}
