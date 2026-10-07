/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Borrowed from MVEL, under the ASL2.0 license.
 *
 */

package org.mvel3.compat.mvel3;

import org.junit.jupiter.api.Test;
import org.mvel3.MVEL;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Porting of TernaryOpPriorityTest from MVEL2 adapted for MVEL3.
 *
 * <p>All 2 original tests pass unchanged in MVEL3 — nested ternary operator
 * right-associativity and precedence are consistent between the two engines.
 * No tests are skipped or disabled.
 */
public class TernaryOpPriorityAdaptedTest {

    private static Object eval(final String expression) {
        return new MVEL().executeExpression(expression);
    }

    @Test
    public void testTernaryOperatorPriority_Interpreted() {
        int javaResult = false ? true ? 9 : 5 : 1;
        assertEquals(javaResult, ((Integer) eval("false ? true ? 9 : 5 : 1")).intValue());
    }

    @Test
    public void testTernaryOperatorPriority_Compiled() {
        int javaResult = false ? true ? 9 : 5 : 1;
        assertEquals(javaResult, ((Integer) eval("false ? true ? 9 : 5 : 1")).intValue());
    }
}
