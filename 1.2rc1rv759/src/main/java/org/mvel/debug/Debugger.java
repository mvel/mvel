package org.mvel.debug;


public interface Debugger {
    /**
     * When a breakpoint is recached, 
     * @param frame
     * @return continuation command
     */
    public int onBreak(Frame frame);
}