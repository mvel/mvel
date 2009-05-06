package org.mvel.sh.command.basic;

import org.mvel.sh.Command;
import org.mvel.sh.ShellSession;

import java.util.Map;

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
