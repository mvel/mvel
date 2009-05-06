package org.mvel.sh;

public interface Command {
    public Object execute(ShellSession session, String[] args);

    public String getDescription();

    public String getHelp();
}
