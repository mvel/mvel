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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mvel3.MVEL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Porting of CompileArithmeticIdentifierTest from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>Regression tests for issue #422: expressions with constant multiplication folding
 * and identifier resolution (e.g. {@code 1 + 2 * 3 * D}).
 */
public class CompileArithmeticIdentifierAdaptedTest {

    private static final Set<String> IMPORTS = Set.of(Bag.class.getCanonicalName());

    private static Object eval(final String expression) {
        return new MVEL().executeExpression(expression);
    }

    private static Object eval(final String expression, final Map<String, Object> vars) {
        return new MVEL().executeExpression(expression, IMPORTS, vars);
    }

    @Test
    public void compilesLiteralPlusTwoLiteralMulsThenIdentifier() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("D", 10);
        assertEquals(11, eval("1 + 1 * 1 * D", vars));
    }

    @Test
    public void compilesLiteralPlusTwoNonOneLiteralMulsThenIdentifier() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("D", 10);
        assertEquals(61, eval("1 + 2 * 3 * D", vars));
    }

    @Test
    public void compilesLiteralPlusThreeLiteralMulsThenIdentifier() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("D", 10);
        assertEquals(11, eval("1 + 1 * 1 * 1 * D", vars));
    }

    @Test
    public void compilesPreviouslyWorkingVariants() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("D", 10);
        assertEquals(10, eval("1 * 1 * D", vars));
        assertEquals(11, eval("1 + 1 * D", vars));
        assertEquals(11, eval("1 + D * 1 * 1", vars));
        assertEquals(12, eval("1 + 1 + 1 * D", vars));
    }

    @Test
    public void executesWithIdentifierBound() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("D", 10);

        assertEquals(11, eval("1 + 1 * 1 * D", vars));
        assertEquals(61, eval("1 + 2 * 3 * D", vars));
        assertEquals(11, eval("1 + 1 * D", vars));
        assertEquals(10, eval("1 * 1 * D", vars));
    }

    public static class Bag {
        public int bar = 7;
    }

    @Test
    public void continuesAfterDeferredIdentifierWithLowerPrecedenceOp() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("D", 10);
        assertEquals(65, eval("1 + 2 * 3 * D + 4", vars));
    }

    @Test
    public void continuesAfterDeferredIdentifierWithSamePrecedenceOp() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("D", 10);
        assertEquals(241, eval("1 + 2 * 3 * D * 4", vars));
    }

    @Test
    public void deferredNonLiteralMayBeAPropertyAccessor() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("foo", new Bag());
        assertEquals(43, eval("1 + 2 * 3 * foo.bar", vars));
    }

    @Test
    public void compileSucceedsButRuntimeStillFailsWhenIdentifierUnbound() {
        assertThrows(Exception.class, () -> eval("1 + 1 * 1 * D", Collections.emptyMap()));
    }

    @Test
    public void literalOnlyConstantFoldingStillWorks() {
        assertEquals(2, eval("1 + 1 * 1 * 1"));
        assertEquals(7, eval("1 + 2 * 3"));
        assertEquals(13, eval("1 + 2 * 2 * 3"));
    }
}
