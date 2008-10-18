package org.mvel2.sh.command.basic;

import org.mvel2.sh.Command;
import org.mvel2.sh.ShellSession;
import static org.mvel2.sh.text.TextUtil.pad;

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
