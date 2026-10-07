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
 * Porting of TrailingBlanksTests from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>Two adaptations are required for each expression:
 * <ul>
 *   <li><b>single-quote-string</b>: {@code 'ok'} / {@code 'ko'} replaced with
 *       double-quoted {@code "ok"} / {@code "ko"}.</li>
 *   <li><b>block-syntax</b>: each branch body uses {@code return} so that MVEL3
 *       can determine the block's return value.</li>
 * </ul>
 * All three tests pass; the trailing newline / blank / absence-of-blank
 * variations behave identically in MVEL3 as in MVEL2.
 */
public class TrailingBlanksAdaptedTest {

    private static Object eval(final String expression) {
        return new MVEL().executeExpression(expression);
    }

    @Test
    public void testWithTrailingNewLine() {
        String expression = "if (true) { return \"ok\"; } else if (false) { return \"ko\"; } else { return \"ko\"; }\n";
        assertEquals("ok", eval(expression));
    }

    @Test
    public void testWithTrailingBlank() {
        String expression = "if (true) { return \"ok\"; } else if (false) { return \"ko\"; } else { return \"ko\"; } ";
        assertEquals("ok", eval(expression));
    }

    @Test
    public void testWithoutTrailingBlank() {
        String expression = "if (true) { return \"ok\"; } else if (false) { return \"ko\"; } else { return \"ko\"; }";
        assertEquals("ok", eval(expression));
    }
}
