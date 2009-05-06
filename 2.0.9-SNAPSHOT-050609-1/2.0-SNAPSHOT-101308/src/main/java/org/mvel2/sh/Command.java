package org.mvel2.sh;

public interface Command {
    public Object execute(ShellSession session, String[] args);

    public String getDescription();

    public String getHelp();
}
