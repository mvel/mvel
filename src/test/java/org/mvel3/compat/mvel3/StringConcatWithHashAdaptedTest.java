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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mvel3.MVEL;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Porting of StringConcatWithHashTest from MVEL2 adapted for MVEL3.
 *
 * <p>MVEL2 provides a {@code #} binary operator that coerces both operands to
 * {@code String} and concatenates them. In MVEL3 {@code #} begins a line
 * comment (Java semantics), so the {@code #} operator is unavailable.
 *
 * <p>The original test is disabled. A replacement test demonstrates the MVEL3
 * idiom for the same operation: use {@code "" + a + b} or
 * {@code String.valueOf(a) + String.valueOf(b)}.
 */
public class StringConcatWithHashAdaptedTest {

    private static Object eval(final String expression, final Map<String, Object> vars) {
        Set<String> imports = new HashSet<>();
        return new MVEL().executeExpression(expression, imports, vars);
    }

    // -------------------------------------------------------------------------
    // hash-concat-operator: MVEL2's # string-coercion concat operator is not
    // available in MVEL3 — # starts a line comment.
    // -------------------------------------------------------------------------

    @Test
    @Disabled("hash-concat-operator: '#' is a line-comment in MVEL3; no equivalent infix coercing-concat operator exists")
    public void testConcatWithHash() {
        // Original MVEL2 expressions:
        //   props['res'] = props['number1'] # props['number2']  → "01"
        //   props['res'] = props['number1'] # props['foo']      → "0bar"
        //   props['res'] = 'bar' # props['foo']                 → "barbar"
    }

    // -------------------------------------------------------------------------
    // MVEL3 replacement: use "" + a + b for coercing string concatenation.
    // -------------------------------------------------------------------------

    @Test
    public void testConcatWithStringCoercion() {
        Map<String, Object> props = new HashMap<>();
        props.put("number1", Integer.valueOf(0));
        props.put("number2", Integer.valueOf(1));
        props.put("foo", "bar");
        Map<String, Object> vars = new HashMap<>();
        vars.put("props", props);

        // "" + a + b coerces both operands to String, matching # semantics.
        assertEquals("01",    eval("\"\" + props[\"number1\"] + props[\"number2\"]", vars));
        assertEquals("0bar",  eval("\"\" + props[\"number1\"] + props[\"foo\"]", vars));
        assertEquals("barbar",eval("\"bar\" + props[\"foo\"]", vars));
    }
}
