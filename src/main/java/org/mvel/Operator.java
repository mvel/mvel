package org.mvel;

public interface Operator {
    public static final int ADD = 0;
    public static final int SUB = 1;
    public static final int MULT = 2;
    public static final int DIV = 3;
    public static final int MOD = 4;
    public static final int EQUAL = 5;
    public static final int NEQUAL = 6;
    public static final int LTHAN = 7;
    public static final int GTHAN = 8;
    public static final int LETHAN = 9;
    public static final int GETHAN = 10;

    public static final int AND = 11;
    public static final int OR = 12;
    public static final int CHOR = 13;
    public static final int REGEX = 14;
    public static final int INSTANCEOF = 15;
    public static final int CONTAINS = 16;
    public static final int STR_APPEND = 17;
    public static final int SOUNDEX = 18;
    public static final int SIMILARITY = 19;
    public static final int BW_AND = 20;
    public static final int BW_OR = 21;
    public static final int BW_XOR = 22;
    public static final int BW_SHIFT_RIGHT = 23;
    public static final int BW_SHIFT_LEFT = 24;
    public static final int BW_USHIFT_RIGHT = 25;
    public static final int BW_USHIFT_LEFT = 26;
    public static final int TERNARY = 27;
    public static final int TERNARY_ELSE = 28;
    public static final int ASSIGN = 29;
    public static final int INC_ASSIGN = 30;
    public static final int DEC_ASSIGN = 31;
    public static final int NEW = 32;
    public static final int PROJECTION = 33;
    public static final int CONVERTABLE_TO = 34;
    public static final int END_OF_STMT = 35;
}
