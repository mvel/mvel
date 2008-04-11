/**
 * MVEL (The MVFLEX Expression Language)
 *
 * Copyright (C) 2007 Christopher Brock, MVFLEX/Valhalla Project and the Codehaus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.mvel;

public interface Operator {
    public static final int ADD = 0;
    public static final int SUB = 1;
    public static final int MULT = 2;
    public static final int DIV = 3;
    public static final int MOD = 4;
    public static final int POWER = 5;
    public static final int LTHAN = 6;
    public static final int GTHAN = 7;
    public static final int LETHAN = 8;
    public static final int GETHAN = 9;

    public static final int EQUAL = 10;
    public static final int NEQUAL = 11;

    public static final int AND = 12;
    public static final int OR = 13;
    public static final int CHOR = 14;
    public static final int REGEX = 15;
    public static final int INSTANCEOF = 16;
    public static final int CONTAINS = 17;
    public static final int STR_APPEND = 18;
    public static final int SOUNDEX = 19;
    public static final int SIMILARITY = 20;
    public static final int BW_AND = 21;
    public static final int BW_OR = 22;
    public static final int BW_XOR = 23;
    public static final int BW_SHIFT_RIGHT = 24;
    public static final int BW_SHIFT_LEFT = 25;
    public static final int BW_USHIFT_RIGHT = 26;
    public static final int BW_USHIFT_LEFT = 27;
    public static final int TERNARY = 28;
    public static final int TERNARY_ELSE = 29;
    public static final int ASSIGN = 30;
    public static final int INC_ASSIGN = 31;
    public static final int DEC_ASSIGN = 32;
    public static final int NEW = 33;
    public static final int PROJECTION = 34;
    public static final int CONVERTABLE_TO = 35;
    public static final int END_OF_STMT = 36;

    public static final int FOREACH = 37;
    public static final int IF = 38;
    public static final int ELSE = 39;
    public static final int WHILE = 40;
    public static final int FOR = 41;
    public static final int SWITCH = 42;
    public static final int DO = 43;
    public static final int WITH = 44;

    public static final int INC = 50;
    public static final int DEC = 51;
    public static final int ASSIGN_ADD = 52;
    public static final int ASSIGN_SUB = 53;
    public static final int ASSIGN_STR_APPEND = 54;


    public static final int IMPORT_STATIC = 95;
    public static final int IMPORT = 96;
    public static final int ASSERT = 97;
    public static final int UNTYPED_VAR = 98;
    public static final int RETURN = 99;

    public static final int FUNCTION = 100;

}
