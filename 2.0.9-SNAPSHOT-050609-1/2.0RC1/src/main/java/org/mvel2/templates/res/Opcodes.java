package org.mvel2.templates.res;

public interface Opcodes {
    public static int IF = 1;
    public static int ELSE = 2;
    public static int FOREACH = 3;
    public static int END = 10;

    public static int INCLUDE_FILE = 50;
    public static int INCLUDE_NAMED = 51;
    public static int COMMENT = 52;
    public static int CODE = 53;

    public static int DECLARE = 54;

    public static int STOP = 70;
}
