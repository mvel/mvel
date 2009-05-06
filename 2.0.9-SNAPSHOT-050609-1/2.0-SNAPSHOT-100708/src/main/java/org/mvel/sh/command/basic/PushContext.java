package org.mvel.sh.command.basic;

import org.mvel.MVEL;
import org.mvel.sh.Command;
import org.mvel.sh.ShellSession;

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
