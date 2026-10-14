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
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mvel3.MVEL;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Porting of PrimitiveTypesTest from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>Adaptations:
 * <ul>
 *   <li><b>this-reference</b>: In MVEL2, {@code pctx.addInput("this", FactWithFloat.class)}
 *       allowed implicit resolution of {@code floatValue} on the root object. In MVEL3,
 *       the context object is accessed explicitly (e.g. {@code fact.floatValue}).</li>
 *   <li>Float comparisons use float literals ({@code 15.1f}) to follow Java IEEE 754
 *       floating-point semantics.</li>
 * </ul>
 */
public class PrimitiveTypesAdaptedTest {

    public static class FactWithFloat {

        private final float floatValue;
        private final Float floatObjectValue;

        public FactWithFloat(final float floatValue) {
            this.floatValue = floatValue;
            this.floatObjectValue = floatValue;
        }

        public float getFloatValue() {
            return floatValue;
        }

        public Float getFloatObjectValue() {
            return floatObjectValue;
        }
    }

    private static final Set<String> IMPORTS = Set.of(FactWithFloat.class.getCanonicalName());

    @Test
    public void testFloatPrimitive() {
        Map<String, Object> vars = Collections.singletonMap("fact", new FactWithFloat(15.1f));
        boolean result = (Boolean) new MVEL().executeExpression("fact.floatValue == 15.1f", IMPORTS, vars);
        assertTrue(result);
    }

    @Test
    public void testFloat() {
        Map<String, Object> vars = Collections.singletonMap("fact", new FactWithFloat(15.1f));
        boolean result = (Boolean) new MVEL().executeExpression("fact.floatObjectValue == 15.1f", IMPORTS, vars);
        assertTrue(result);
    }
}
