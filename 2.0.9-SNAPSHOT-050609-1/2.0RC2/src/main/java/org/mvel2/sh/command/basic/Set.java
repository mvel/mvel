package org.mvel2.sh.command.basic;

import org.mvel2.sh.Command;
import org.mvel2.sh.CommandException;
import org.mvel2.sh.ShellSession;

import java.util.Map;

public class Set implements Command {
    public Object execute(ShellSession session, String[] args) {

        Map<String, String> env = session.getEnv();

        if (args.length == 0) {
            for (String var : env.keySet()) {
                System.out.println(var + " = " + env.get(var));
            }
        }
        else if (args.length == 1) {
            throw new CommandException("incorrect number of parameters");
        }
        else {
            StringBuffer sbuf = new StringBuffer();
            for (int i = 1; i < args.length; i++) {
                sbuf.append(args[i]);
                if (i < args.length) sbuf.append(" ");
            }

            env.put(args[0], sbuf.toString().trim());
        }

        return null;
    }


    public String getDescription() {
        return "sets an environment variable";
    }

    public String getHelp() {
        return null;
    }
}
