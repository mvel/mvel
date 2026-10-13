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
 * Imported from org.mvel2.tests.core.operators.EqualityComparisonTest (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged. Failures are intentionally visible; known behavioural differences
 * are annotated with {@link KnownCompatibility}.
 */
@Tag("mvel2-compatibility")
public class EqualityComparisonTest extends BaseOperatorsTest {

    private static final Class<?>[] PRIMITIVE_WRAPPER_TYPES = new Class<?>[]{
            Integer.class, Long.class, Byte.class, Short.class, Float.class, Double.class
    };

    private static final Class<?>[] BIG_NUMBER_TYPES = new Class<?>[]{
            BigInteger.class, BigDecimal.class
    };

    public static Collection<Object[]> primitiveWrapperParams() {
        List<Object[]> parameterData = new ArrayList<>();
        for (Class<?> type : PRIMITIVE_WRAPPER_TYPES) {
            for (String operator : EQUALITY_COMPARISON_OPERATORS) {
                for (boolean nullPropertyOnLeft : NULL_PROPERTY_ON_LEFT) {
                    parameterData.add(new Object[]{type, operator, nullPropertyOnLeft});
                }
            }
        }
        return parameterData;
    }

    public static Collection<Object[]> characterParams() {
        List<Object[]> parameterData = new ArrayList<>();
        for (String operator : EQUALITY_COMPARISON_OPERATORS) {
            for (boolean nullPropertyOnLeft : NULL_PROPERTY_ON_LEFT) {
                parameterData.add(new Object[]{Character.class, operator, nullPropertyOnLeft});
            }
        }
        return parameterData;
    }

    public static Collection<Object[]> bigNumberParams() {
        List<Object[]> parameterData = new ArrayList<>();
        for (Class<?> type : BIG_NUMBER_TYPES) {
            for (String operator : EQUALITY_COMPARISON_OPERATORS) {
                for (boolean nullPropertyOnLeft : NULL_PROPERTY_ON_LEFT) {
                    parameterData.add(new Object[]{type, operator, nullPropertyOnLeft});
                }
            }
        }
        return parameterData;
    }

    @ParameterizedTest(name = "{0} {1} nullPropertyOnLeft={2}")
    @MethodSource("primitiveWrapperParams")
    public void compareWithNullPropertyPrimitiveWrappers(Class<?> type, String operator, boolean nullPropertyOnLeft) throws Exception {
        executeNullComparison(type, operator, nullPropertyOnLeft);
    }

    @ParameterizedTest(name = "{0} {1} nullPropertyOnLeft={2}")
    @MethodSource("characterParams")
    @KnownCompatibility("inline-import")
    public void compareWithNullPropertyCharacter(Class<?> type, String operator, boolean nullPropertyOnLeft) throws Exception {
        executeNullComparison(type, operator, nullPropertyOnLeft);
    }

    @ParameterizedTest(name = "{0} {1} nullPropertyOnLeft={2}")
    @MethodSource("bigNumberParams")
    @KnownCompatibility("big-number-null-comparison")
    public void compareWithNullPropertyBigNumbers(Class<?> type, String operator, boolean nullPropertyOnLeft) throws Exception {
        executeNullComparison(type, operator, nullPropertyOnLeft);
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
        if (operator.equals("==") && result.equals(false) || operator.equals("!=") && result.equals(true)) {
            assertTrue(true);
        } else {
            fail("Wrong result: " + expression + " -> " + result);
        }
    }
}
