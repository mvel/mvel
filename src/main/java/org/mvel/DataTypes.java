package org.mvel;

/**
 * @author Christopher Brock
 */
public interface DataTypes {
    public static final int NULL = -1;

    public static final int OBJECT = 0;
    public static final int STRING = 1;
    public static final int SHORT = 100;
    public static final int INTEGER = 101;
    public static final int LONG = 102;
    public static final int DOUBLE = 103;
    public static final int FLOAT = 104;
    public static final int BOOLEAN = 7;
    public static final int CHAR = 8;
    public static final int BYTE = 9;

    public static final int W_SHORT = 105;
    public static final int W_INTEGER = 106;
    public static final int W_LONG = 107;
    public static final int W_DOUBLE = 108;
    public static final int W_FLOAT = 109;
    public static final int W_CHAR = 112;

    public static final int W_BOOLEAN = 15;
    public static final int W_BYTE = 17;

    public static final int BIG_DECIMAL = 110;
    public static final int BIG_INTEGER = 111;

    public static final int EMPTY = 200;

    public static final int UNIT = 300;
}
