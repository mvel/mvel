package org.mvel2.sh.command.basic;

import org.mvel2.sh.Command;
import org.mvel2.sh.ShellSession;

public class Exit implements Command {

    public Object execute(ShellSession session, String[] args) {
        System.exit(0);
        return null;
    }


    public String getDescription() {
        return "exits the command shell";
    }

    public String getHelp() {
        return "No help yet.";
    }
}
