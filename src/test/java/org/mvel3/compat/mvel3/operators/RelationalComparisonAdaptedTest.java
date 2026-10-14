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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mvel3.MVEL;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Porting of RelationalComparisonTest from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>Adaptations:
 * <ul>
 *   <li><b>null-relational-comparison</b>: MVEL2 returned {@code false} when comparing {@code null}
 *       with relational operators. In MVEL3/Java, unboxing/compareTo on null results in NPE.
 *       Only non-null comparisons are tested here.</li>
 *   <li><b>Character literals</b>: Character comparisons use standard {@code '1'} / {@code '2'} char literals.</li>
 * </ul>
 */
public class RelationalComparisonAdaptedTest {

    private static final Class<?>[] PRIMITIVE_WRAPPER_TYPES = new Class<?>[]{
            Integer.class, Long.class, Byte.class, Short.class, Float.class, Double.class
    };

    private static final Class<?>[] BIG_NUMBER_TYPES = new Class<?>[]{
            BigInteger.class, BigDecimal.class
    };

    private static final String[] RELATIONAL_OPERATORS = new String[]{">", ">=", "<", "<="};

    private static final Set<String> IMPORTS = Set.of(
            BigInteger.class.getCanonicalName(),
            BigDecimal.class.getCanonicalName());

    public static Collection<Object[]> primitiveParams() {
        List<Object[]> parameterData = new ArrayList<>();
        for (Class<?> type : PRIMITIVE_WRAPPER_TYPES) {
            for (String operator : RELATIONAL_OPERATORS) {
                parameterData.add(new Object[]{type, operator});
            }
        }
        return parameterData;
    }

    public static Collection<Object[]> bigNumberParams() {
        List<Object[]> parameterData = new ArrayList<>();
        for (Class<?> type : BIG_NUMBER_TYPES) {
            for (String operator : RELATIONAL_OPERATORS) {
                parameterData.add(new Object[]{type, operator});
            }
        }
        return parameterData;
    }

    public static Collection<Object[]> characterParams() {
        List<Object[]> parameterData = new ArrayList<>();
        for (String operator : RELATIONAL_OPERATORS) {
            parameterData.add(new Object[]{operator});
        }
        return parameterData;
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("primitiveParams")
    public void compareNonNullPrimitives(Class<?> type, String operator) {
        String lowValue = "new " + type.getSimpleName() + "(\"1\")";
        String highValue = "new " + type.getSimpleName() + "(\"2\")";
        executeComparisons(operator, lowValue, highValue, Set.of());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("bigNumberParams")
    public void compareNonNullBigNumbers(Class<?> type, String operator) {
        String lowValue = "new " + type.getSimpleName() + "(\"1\")";
        String highValue = "new " + type.getSimpleName() + "(\"2\")";
        executeComparisons(operator, lowValue, highValue, IMPORTS);
    }

    @ParameterizedTest(name = "Character {0}")
    @MethodSource("characterParams")
    public void compareNonNullCharacter(String operator) {
        String lowValue = "'1'";
        String highValue = "'2'";
        executeComparisons(operator, lowValue, highValue, Set.of());
    }

    private void executeComparisons(String operator, String lowValue, String highValue, Set<String> imports) {
        boolean expectedLowVsHigh;
        boolean expectedHighVsLow;
        boolean expectedAtEquality;

        if (">".equals(operator)) {
            expectedLowVsHigh = false;
            expectedHighVsLow = true;
            expectedAtEquality = false;
        } else if (">=".equals(operator)) {
            expectedLowVsHigh = false;
            expectedHighVsLow = true;
            expectedAtEquality = true;
        } else if ("<".equals(operator)) {
            expectedLowVsHigh = true;
            expectedHighVsLow = false;
            expectedAtEquality = false;
        } else if ("<=".equals(operator)) {
            expectedLowVsHigh = true;
            expectedHighVsLow = false;
            expectedAtEquality = true;
        } else {
            throw new IllegalStateException("Unexpected operator: " + operator);
        }

        assertResult(expectedLowVsHigh, lowValue + " " + operator + " " + highValue, imports);
        assertResult(expectedHighVsLow, highValue + " " + operator + " " + lowValue, imports);
        assertResult(expectedAtEquality, lowValue + " " + operator + " " + lowValue, imports);
    }

    private void assertResult(boolean expected, String expression, Set<String> imports) {
        Object result = new MVEL().executeExpression(expression, imports, Map.of());
        assertEquals(expected, result, expression);
    }
}
