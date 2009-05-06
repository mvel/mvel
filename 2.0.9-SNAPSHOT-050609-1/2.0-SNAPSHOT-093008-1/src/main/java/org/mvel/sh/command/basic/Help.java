package org.mvel.sh.command.basic;

import org.mvel.sh.Command;
import org.mvel.sh.ShellSession;
import static org.mvel.sh.text.TextUtil.pad;

public class Help implements Command {
    public Object execute(ShellSession session, String[] args) {
        for (String command : session.getCommands().keySet()) {
            System.out.println(command + pad(command.length(), 25) + "- " + session.getCommands().get(command).getDescription());
        }

        return null;
    }


    public String getDescription() {
        return "displays help for available shell commands";
    }

    public String getHelp() {
        return "No help yet";
    }
}
