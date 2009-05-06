package org.mvel.sh.command.file;

import org.mvel.sh.Command;
import org.mvel.sh.CommandException;
import org.mvel.sh.ShellSession;

import java.util.Map;
import java.io.File;

public class ChangeWorkingDir implements Command {

    public Object execute(ShellSession session, String[] args) {
        File cwd = new File(session.getEnv().get("$CWD"));

        if (args.length == 0 || ".".equals(args[0])) return null;
        else if ("..".equals(args[0])) {
            if (cwd.getParentFile() != null) {
                cwd = cwd.getParentFile();
            }
            else {       
                throw new CommandException("already at top-level directory");
            }
        }
        else if (args[0].charAt(0) == '/') {
            cwd = new File(args[0]);
            if (!cwd.exists()) {
                throw new CommandException("no such directory: " + args[0]);
            }
        }
        else {
            cwd = new File(cwd.getAbsolutePath() + "/" + args[0]);
            if (!cwd.exists()) {
                throw new CommandException("no such directory: " + args[0]);
            }
        }

        session.getEnv().put("$CWD", cwd.getAbsolutePath());

        return null;

    }


    public String getDescription() {
        return "changes the working directory";
    }

    public String getHelp() {
        return "no help yet";
    }
}
