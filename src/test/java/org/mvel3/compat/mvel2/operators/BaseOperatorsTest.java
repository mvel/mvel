package org.mvel3.compat.mvel2.operators;

import java.beans.Introspector;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.mvel3.compat.mvel2.AbstractTest;

public class BaseOperatorsTest extends AbstractTest {

    public static final Class<?>[] TYPES = new Class<?>[]{
            Integer.class, Long.class, Byte.class, Character.class, Short.class, Float.class, Double.class, BigInteger.class, BigDecimal.class
    };
    public static final boolean[] NULL_PROPERTY_ON_LEFT = new boolean[]{true, false};
    public static final String[] EQUALITY_COMPARISON_OPERATORS = new String[]{"==", "!="};
    public static final String[] RELATIONAL_COMPARISON_OPERATORS = new String[]{">", ">=", "<", "<="};

    protected Class<?> type;
    protected String operator;
    protected boolean nullPropertyOnLeft;

    public BaseOperatorsTest() {
    }

    public BaseOperatorsTest(Class<?> type, String operator, boolean nullPropertyOnLeft) {
        this.type = type;
        this.operator = operator;
        this.nullPropertyOnLeft = nullPropertyOnLeft;
    }

    public static String getPropertyName(Class<?> clazz) {
        return Introspector.decapitalize(clazz.getSimpleName()) + "Value";
    }

    public static String getInstanceValueString(Class<?> clazz) {
        if (clazz.equals(Character.class)) {
            return "BaseOperatorTest.constantCharacterValue()";
        } else {
            return "new " + clazz.getSimpleName() + "(\"0\")";
        }
    }

    public static String getInstanceValueString(Class<?> clazz, String value) {
        if (clazz.equals(Character.class)) {
            return "BaseOperatorTest.characterValue(\"" + value + "\")";
        } else {
            return "new " + clazz.getSimpleName() + "(\"" + value + "\")";
        }
    }

    public static Character constantCharacterValue() {
        return Character.valueOf('a');
    }

    public static Character characterValue(String value) {
        return Character.valueOf(value.charAt(0));
    }
}
