package org.mvel.debug;


public interface Debugger {
    public static int CONTINUE = 0;
    public static int STEP = 1;
    public static int STEP_OVER = STEP;


    /**
     * When a breakpoint is recached, 
     * @param frame
     * @return continuation command
     */
    public int onBreak(Frame frame);
}