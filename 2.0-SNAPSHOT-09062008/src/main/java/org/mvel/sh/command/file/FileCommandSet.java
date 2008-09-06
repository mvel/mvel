package org.mvel.sh.command.file;

import org.mvel.sh.CommandSet;
import org.mvel.sh.Command;

import java.util.Map;
import java.util.HashMap;

public class FileCommandSet implements CommandSet {

    public Map<String, Command> load() {
        Map<String, Command> cmd = new HashMap<String, Command>();

        cmd.put("ls", new DirList());
        cmd.put("cd", new ChangeWorkingDir());
        cmd.put("pwd", new PrintWorkingDirectory());

        return cmd;
    }
}
