package org.mvel.sh.command.file;

import org.mvel.sh.Command;
import org.mvel.sh.ShellSession;

/**
 * Created by IntelliJ IDEA.
 * User: christopherbrock
 * Date: Oct 9, 2007
 * Time: 1:16:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class PrintWorkingDirectory implements Command {

    public Object execute(ShellSession session, String[] args) {
        System.out.println(session.getEnv().get("$CWD"));
        return null;
    }


    public String getDescription() {
        return "prints the current working directory";
    }

    public String getHelp() {
        return "no help yet.";
    }
}
