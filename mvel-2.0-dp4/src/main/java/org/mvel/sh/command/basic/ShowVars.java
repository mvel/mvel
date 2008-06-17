package org.mvel.sh.command.basic;

import org.mvel.sh.Command;
import org.mvel.sh.CommandException;
import org.mvel.sh.ShellSession;

import java.util.Map;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: Mike Brock
 * Date: 6-Feb-2007
 * Time: 10:41:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class ShowVars implements Command {


    public Object execute(ShellSession session, String[] args) {
        boolean values = false;

        Map<String, Object> vars = session.getVariables();

        for (int i = 0; i < args.length; i++) {
            if ("-values".equals(args[i])) values = true;
            else throw new CommandException("unknown argument: " + args[i]);
        }

        System.out.println("Printing Variables ...");
        if (values) {
            for (String key : vars.keySet()) {
                System.out.println(key + " => " + String.valueOf(vars.get(key)));
            }
        }
        else {
            for (String key : vars.keySet()) {
                System.out.println(key);
            }
        }

        System.out.println(" ** " + vars.size() + " variables total.");

        return null;
    }


    public String getDescription() {
        return "shows current variables";
    }

    public String getHelp() {
        return "no help yet";
    }
}
