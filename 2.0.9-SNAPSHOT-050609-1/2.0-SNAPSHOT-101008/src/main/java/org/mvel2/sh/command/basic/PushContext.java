package org.mvel2.sh.command.basic;

import org.mvel2.MVEL;
import org.mvel2.sh.Command;
import org.mvel2.sh.ShellSession;

public class PushContext implements Command {
    public Object execute(ShellSession session, String[] args) {
        session.setCtxObject(MVEL.eval(args[0], session.getCtxObject(), session.getVariables()));
        return "Changed Context";
    }

    public String getDescription() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getHelp() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
