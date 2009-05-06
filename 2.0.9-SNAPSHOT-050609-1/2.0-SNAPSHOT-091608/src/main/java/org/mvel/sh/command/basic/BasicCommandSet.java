package org.mvel.sh.command.basic;

import org.mvel.sh.Command;
import org.mvel.sh.CommandSet;

import java.util.LinkedHashMap;
import java.util.Map;

public class BasicCommandSet implements CommandSet {

    public Map<String, Command> load() {
        Map<String, Command> cmds = new LinkedHashMap<String, Command>();

        cmds.put("set", new Set());
        cmds.put("help", new Help());
        cmds.put("showvars", new ShowVars());
        cmds.put("inspect", new ObjectInspector());
        cmds.put("exit", new Exit());

        return cmds;
    }
}

