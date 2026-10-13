/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel3.compat.mvel3;

import org.junit.jupiter.api.Test;
import org.mvel3.MVEL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Porting of ScopeTests from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>Adaptations:
 * <ul>
 *   <li><b>block-syntax</b>: {@code var i = 0} gets a trailing {@code ;}, and the
 *       final {@code i} becomes {@code return i;}. This ensures the failure is
 *       caused by {@code i} being out of scope, not by a syntax error.</li>
 *   <li>MVEL2's interpreted/compiled (strongly typed) distinction does not exist in
 *       MVEL3, so both original tests collapse to the same check.</li>
 * </ul>
 */
public class ScopeAdaptedTest {

    private static Object eval(final String expression) {
        return new MVEL().executeExpression(expression);
    }

    @Test
    public void testNoScopeLeakage() {
        String ex = "if (true) { var i = 0; } return i;";
        Exception e = assertThrows(Exception.class, () -> eval(ex));
        assertTrue(e.getMessage().contains("cannot find symbol"), e.getMessage());
    }

    @Test
    public void testVariableVisibleInsideScope() {
        // Sanity check: the same expression is valid when i is used inside the block.
        String ex = "if (true) { var i = 7; return i; } return 0;";
        assertEquals(7, eval(ex));
    }
}
