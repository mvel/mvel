package org.mvel3.compat.mvel2.operators;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mvel3.compat.mvel2.KnownCompatibility;
import org.mvel3.compat.mvel2.TestCompilerContext;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;

/**
 * Imported from org.mvel2.tests.core.operators.RelationalComparisonTest (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged. Failures are intentionally visible; known behavioural differences
 * are annotated with {@link KnownCompatibility}.
 */
@Tag("mvel2-compatibility")
public class RelationalComparisonTest extends BaseOperatorsTest {

    private static final Class<?>[] PRIMITIVE_WRAPPER_TYPES = new Class<?>[]{
            Integer.class, Long.class, Byte.class, Short.class, Float.class, Double.class
    };

    private static final Class<?>[] BIG_NUMBER_TYPES = new Class<?>[]{
            BigInteger.class, BigDecimal.class
    };

    public static Collection<Object[]> primitiveWrapperParams() {
        List<Object[]> parameterData = new ArrayList<>();
        for (Class<?> type : PRIMITIVE_WRAPPER_TYPES) {
            for (String operator : RELATIONAL_COMPARISON_OPERATORS) {
                for (boolean nullPropertyOnLeft : NULL_PROPERTY_ON_LEFT) {
                    parameterData.add(new Object[]{type, operator, nullPropertyOnLeft});
                }
            }
        }
        return parameterData;
    }

    public static Collection<Object[]> characterParams() {
        List<Object[]> parameterData = new ArrayList<>();
        for (String operator : RELATIONAL_COMPARISON_OPERATORS) {
            for (boolean nullPropertyOnLeft : NULL_PROPERTY_ON_LEFT) {
                parameterData.add(new Object[]{Character.class, operator, nullPropertyOnLeft});
            }
        }
        return parameterData;
    }

    public static Collection<Object[]> bigNumberParams() {
        List<Object[]> parameterData = new ArrayList<>();
        for (Class<?> type : BIG_NUMBER_TYPES) {
            for (String operator : RELATIONAL_COMPARISON_OPERATORS) {
                for (boolean nullPropertyOnLeft : NULL_PROPERTY_ON_LEFT) {
                    parameterData.add(new Object[]{type, operator, nullPropertyOnLeft});
                }
            }
        }
        return parameterData;
    }

    public static Collection<Object[]> nonNullPrimitiveParams() {
        List<Object[]> parameterData = new ArrayList<>();
        for (Class<?> type : PRIMITIVE_WRAPPER_TYPES) {
            for (String operator : RELATIONAL_COMPARISON_OPERATORS) {
                parameterData.add(new Object[]{type, operator});
            }
        }
        return parameterData;
    }

    public static Collection<Object[]> nonNullBigNumberParams() {
        List<Object[]> parameterData = new ArrayList<>();
        for (Class<?> type : BIG_NUMBER_TYPES) {
            for (String operator : RELATIONAL_COMPARISON_OPERATORS) {
                parameterData.add(new Object[]{type, operator});
            }
        }
        return parameterData;
    }

    public static Collection<Object[]> nonNullCharacterParams() {
        List<Object[]> parameterData = new ArrayList<>();
        for (String operator : RELATIONAL_COMPARISON_OPERATORS) {
            parameterData.add(new Object[]{Character.class, operator});
        }
        return parameterData;
    }

    @ParameterizedTest(name = "{0} {1} nullPropertyOnLeft={2}")
    @MethodSource("primitiveWrapperParams")
    @KnownCompatibility("null-relational-comparison")
    public void compareWithNullPropertyPrimitiveWrappers(Class<?> type, String operator, boolean nullPropertyOnLeft) throws Exception {
        executeNullComparison(type, operator, nullPropertyOnLeft);
    }

    @ParameterizedTest(name = "{0} {1} nullPropertyOnLeft={2}")
    @MethodSource("characterParams")
    @KnownCompatibility("null-relational-comparison")
    public void compareWithNullPropertyCharacter(Class<?> type, String operator, boolean nullPropertyOnLeft) throws Exception {
        executeNullComparison(type, operator, nullPropertyOnLeft);
    }

    @ParameterizedTest(name = "{0} {1} nullPropertyOnLeft={2}")
    @MethodSource("bigNumberParams")
    @KnownCompatibility("null-relational-comparison")
    public void compareWithNullPropertyBigNumbers(Class<?> type, String operator, boolean nullPropertyOnLeft) throws Exception {
        executeNullComparison(type, operator, nullPropertyOnLeft);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("nonNullPrimitiveParams")
    public void compareNonNullValuesPrimitives(Class<?> type, String operator) throws Exception {
        executeNonNullComparison(type, operator);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("nonNullBigNumberParams")
    public void compareNonNullValuesBigNumbers(Class<?> type, String operator) throws Exception {
        executeNonNullComparison(type, operator);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("nonNullCharacterParams")
    @KnownCompatibility("inline-import")
    public void compareNonNullValuesCharacter(Class<?> type, String operator) throws Exception {
        executeNonNullComparison(type, operator);
    }

    private void executeNullComparison(Class<?> type, String operator, boolean nullPropertyOnLeft) {
        String propertyName = getPropertyName(type);
        String instanceValueString = getInstanceValueString(type);
        String expression = "";
        if (nullPropertyOnLeft) {
            expression += propertyName + " " + operator + " " + instanceValueString;
        } else {
            expression += instanceValueString + " " + operator + " " + propertyName;
        }

        TestCompilerContext pctx = new TestCompilerContext();
        pctx.addInput(propertyName, type);
        pctx.addImport(type);
        pctx.addImport(BaseOperatorsTest.class);

        Serializable compiledExpr = compileExpression(expression, pctx);

        Map<String, Object> vars = new HashMap<>();
        vars.put(propertyName, null);

        Object result = executeExpression(compiledExpr, vars);
        assertEquals("Comparison with null property should be false: " + expression, false, result);
    }

    private void executeNonNullComparison(Class<?> type, String operator) {
        String lowValue = getInstanceValueString(type, "1");
        String highValue = getInstanceValueString(type, "2");
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

        assertRelationalResult(expectedLowVsHigh, lowValue, highValue, type, operator);
        assertRelationalResult(expectedHighVsLow, highValue, lowValue, type, operator);
        assertRelationalResult(expectedAtEquality, lowValue, lowValue, type, operator);
    }

    private void assertRelationalResult(boolean expected, String leftValue, String rightValue, Class<?> type, String operator) {
        String expression = leftValue + " " + operator + " " + rightValue;

        TestCompilerContext pctx = new TestCompilerContext();
        pctx.addImport(type);
        pctx.addImport(BaseOperatorsTest.class);

        Serializable compiledExpr = compileExpression(expression, pctx);
        Object result = executeExpression(compiledExpr);
        assertEquals(expression, expected, result);
    }
}
