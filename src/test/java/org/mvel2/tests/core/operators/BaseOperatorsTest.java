package org.mvel2.tests.core.operators;

import java.beans.Introspector;
import java.math.BigDecimal;
import java.math.BigInteger;

public class BaseOperatorsTest {

    protected static final Class[] TYPES = new Class[]{Integer.class, Long.class, Byte.class, Character.class, Short.class, Float.class, Double.class, BigInteger.class, BigDecimal.class};
    protected static final boolean[] NULL_PROPERTY_ON_LEFT = new boolean[]{true, false};
    protected static final String[] EQUALITY_COMPARISON_OPERATORS = new String[]{"==", "!="};

    protected Class type;

    protected String operator;

    protected boolean nullPropertyOnLeft;

    public BaseOperatorsTest(Class type, String operator, boolean nullPropertyOnLeft) {
        this.type = type;
        this.operator = operator;
        this.nullPropertyOnLeft = nullPropertyOnLeft;
    }

    protected static String getPropertyName(Class clazz) {
        return Introspector.decapitalize(clazz.getSimpleName()) + "Value";
    }

    protected static String getInstanceValueString(Class clazz) {
        if (clazz.equals(Character.class)) {
            return "BaseOperatorTest.constantCharacterValue()"; //// Mvel converts char to String so we cannot express Character constructor
        } else {
            return "new " + clazz.getSimpleName() + "(\"0\")";
        }
    }

    public static Character constantCharacterValue() {
        return Character.valueOf('a');
    }
}
