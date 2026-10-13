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

package org.mvel3.compat.mvel3.operators;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mvel3.MVEL;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Porting of EqualityComparisonTest from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>Adaptations:
 * <ul>
 *   <li><b>big-number-null-comparison</b>: MVEL3 transpiles {@code ==} on BigInteger/BigDecimal
 *       to {@code .compareTo(...) == 0}, which throws NPE when either operand is null.
 *       In MVEL3, {@code Objects.equals(a, b)} or null-safe checks are used.</li>
 *   <li><b>inline-import</b>: Imports are passed via API imports set rather than script-level import aliases.</li>
 * </ul>
 */
public class EqualityComparisonAdaptedTest {

    private static final Class<?>[] PRIMITIVE_WRAPPER_TYPES = new Class<?>[]{
            Integer.class, Long.class, Byte.class, Short.class, Float.class, Double.class
    };

    private static final String[] EQUALITY_OPERATORS = new String[]{"==", "!="};
    private static final boolean[] NULL_ON_LEFT = new boolean[]{true, false};

    private static final Set<String> IMPORTS = Set.of(
            BigInteger.class.getCanonicalName(),
            BigDecimal.class.getCanonicalName());

    public static Collection<Object[]> ruleParams() {
        List<Object[]> parameterData = new ArrayList<>();
        for (Class<?> type : PRIMITIVE_WRAPPER_TYPES) {
            for (String operator : EQUALITY_OPERATORS) {
                for (boolean nullOnLeft : NULL_ON_LEFT) {
                    parameterData.add(new Object[]{type, operator, nullOnLeft});
                }
            }
        }
        return parameterData;
    }

    private static String getPropertyName(Class<?> clazz) {
        String name = clazz.getSimpleName();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1) + "Value";
    }

    @ParameterizedTest(name = "{0} {1} nullPropertyOnLeft={2}")
    @MethodSource("ruleParams")
    public void compareWithNullPropertyPrimitiveWrappers(Class<?> type, String operator, boolean nullPropertyOnLeft) {
        String propertyName = getPropertyName(type);
        String instanceValueString = "new " + type.getSimpleName() + "(\"0\")";
        String expression = nullPropertyOnLeft
                ? propertyName + " " + operator + " " + instanceValueString
                : instanceValueString + " " + operator + " " + propertyName;

        Map<String, Object> vars = new HashMap<>();
        vars.put(propertyName, null);

        Object result = new MVEL().executeExpression(expression, IMPORTS, vars);
        boolean expected = "!=".equals(operator);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "Character {0} nullPropertyOnLeft={1}")
    @MethodSource("characterParams")
    public void compareWithNullPropertyCharacter(String operator, boolean nullPropertyOnLeft) {
        String expression = nullPropertyOnLeft
                ? "charValue " + operator + " 'a'"
                : "'a' " + operator + " charValue";

        Map<String, Object> vars = new HashMap<>();
        vars.put("charValue", null);

        Object result = new MVEL().executeExpression(expression, Collections.emptySet(), vars);
        boolean expected = "!=".equals(operator);
        assertEquals(expected, result);
    }

    public static Collection<Object[]> characterParams() {
        List<Object[]> parameterData = new ArrayList<>();
        for (String operator : EQUALITY_OPERATORS) {
            for (boolean nullOnLeft : NULL_ON_LEFT) {
                parameterData.add(new Object[]{operator, nullOnLeft});
            }
        }
        return parameterData;
    }
}
