package org.mvel.sh.command.file;

import org.mvel.sh.Command;
import org.mvel.sh.CommandException;
import org.mvel.sh.ShellSession;

import java.io.File;


public class DirList implements Command {
    public Object execute(ShellSession session, String[] args) {
        File current = new File(session.getEnv().get("$CWD"));

        if (!current.isDirectory())
            throw new CommandException("cannot list directory : " + session.getEnv().get("$CWD") + " is not a directory");

        File[] files = current.listFiles();

        if (files.length == 0)  return null;
        else {
            System.out.append("Total ").append(String.valueOf(files.length)).append("\n");
        }

        for (File file : current.listFiles()) {
            if (file.isDirectory()) {
                System.out.append(file.getName()).append("/");
            }
            else {
                System.out.append(file.getName());
            }
            System.out.append("\n");
        }
        System.out.flush();

        return null;
    }


    public String getDescription() {
        return "performs a list of files and directories in the current working dir.";
    }

    public String getHelp() {
        return "no help yet";
    }
}
