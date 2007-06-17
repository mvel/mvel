package org.mvel.debug;


public interface Debugger {
    public int onBreak();
    public boolean breakOn(int lineNumber);
}