package org.mvel.sh;

import java.util.Map;

public interface Command {
    public Object execute(ShellSession session, String[] args);
    public String getDescription();
    public String getHelp();
}
