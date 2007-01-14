package org.mvel;

public interface NodeType {
    public static final int IF = 0;
    public static final int FOREACH = 1;
    public static final int ELSEIF = 2;
    public static final int ELSE = 3;
    public static final int END = 4;
    public static final int PROPERTY_EX = 5;
    public static final int LITERAL = 6;
    public static final int TERMINUS = 7;
    public static final int GOTO = 8;
    public static final int OPERATOR = 9;
}
