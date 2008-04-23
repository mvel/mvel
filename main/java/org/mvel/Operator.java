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
    /**
     * The index positions of the operator precedence values
     * correspond to the actual operator itself. So ADD is PTABLE[0],
     * SUB is PTABLE[1] and so on.
     */
    public static final int[] PTABLE = {
            10,   // ADD
            10,   // SUB
            11,   // MULT
            11,   // DIV
            11,   // MOD
            12,   // POWER


            6,   // BW_AND
            4,   // BW_OR
            5,   // BW_XOR
            9,   // BW_SHIFT_RIGHT
            9,   // BW_SHIFT_LEFT
            9,   // BW_USHIFT_LEFT
            5,   // BW_NOT

            8,   // LTHAN
            8,   // GTHAN
            8,   // LETHAN
            8,   // GETHAN

            7,   // EQUAL
            7,   // NEQUAL
            3,   // AND
            2,   // OR
            2,   // CHOR
            13,   // REGEX
            8,   // INSTANCEOF
            13,   // CONTAINS
            13,   // STR_APPEND
            13,   // SOUNDEX
            13,   // SIMILARITY

            0,  // TERNARY
            0,  // TERNARY ELSE
            13,   // ASSIGN
    };

    public static final int ADD = 0;
    public static final int SUB = 1;
    public static final int MULT = 2;
    public static final int DIV = 3;
    public static final int MOD = 4;
    public static final int POWER = 5;

    public static final int BW_AND = 6;
    public static final int BW_OR = 7;
    public static final int BW_XOR = 8;
    public static final int BW_SHIFT_RIGHT = 9;
    public static final int BW_SHIFT_LEFT = 10;
    public static final int BW_USHIFT_RIGHT = 11;
    public static final int BW_USHIFT_LEFT = 12;
    public static final int BW_NOT = 13;

    public static final int LTHAN = 14;
    public static final int GTHAN = 15;
    public static final int LETHAN = 16;
    public static final int GETHAN = 17;

    public static final int EQUAL = 18;
    public static final int NEQUAL = 19;

    public static final int AND = 20;
    public static final int OR = 21;
    public static final int CHOR = 22;
    public static final int REGEX = 23;
    public static final int INSTANCEOF = 24;
    public static final int CONTAINS = 25;
    public static final int STR_APPEND = 26;
    public static final int SOUNDEX = 27;
    public static final int SIMILARITY = 28;

    public static final int TERNARY = 29;
    public static final int TERNARY_ELSE = 30;
    public static final int ASSIGN = 31;
    public static final int INC_ASSIGN = 32;
    public static final int DEC_ASSIGN = 33;
    public static final int NEW = 34;
    public static final int PROJECTION = 35;
    public static final int CONVERTABLE_TO = 36;
    public static final int END_OF_STMT = 37;

    public static final int FOREACH = 38;
    public static final int IF = 39;
    public static final int ELSE = 40;
    public static final int WHILE = 41;
    public static final int FOR = 42;
    public static final int SWITCH = 43;
    public static final int DO = 44;
    public static final int WITH = 45;

    public static final int INC = 50;
    public static final int DEC = 51;
    public static final int ASSIGN_ADD = 52;
    public static final int ASSIGN_SUB = 53;
    public static final int ASSIGN_STR_APPEND = 54;


    public static final int IMPORT_STATIC = 95;
    public static final int IMPORT = 96;
    public static final int ASSERT = 97;
    public static final int TYPED_VAR = 98;
    public static final int RETURN = 99;

    public static final int FUNCTION = 100;

    public static final int STK_SWAP = 200;
    public static final int STK_XSWAP = 201;
    public static final int STK_X2SWAP = 202;

}
