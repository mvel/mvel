package org.mvel2.sh.command.file;

import org.mvel2.sh.Command;
import org.mvel2.sh.CommandSet;

import java.util.HashMap;
import java.util.Map;

public class FileCommandSet implements CommandSet {

    public Map<String, Command> load() {
        Map<String, Command> cmd = new HashMap<String, Command>();

        cmd.put("ls", new DirList());
        cmd.put("cd", new ChangeWorkingDir());
        cmd.put("pwd", new PrintWorkingDirectory());

        return cmd;
    }
}
