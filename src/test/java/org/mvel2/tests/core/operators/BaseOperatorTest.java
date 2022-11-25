package org.mvel2.tests.core.operators;

import java.beans.Introspector;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BaseOperatorTest {

    protected static final Class[] TYPES = new Class[]{Integer.class, Long.class, Byte.class, Character.class, Short.class, Float.class, Double.class, BigInteger.class, BigDecimal.class};
    protected static final boolean[] PROPERTY_ON_LEFT = new boolean[]{true, false};

    protected Class type;

    protected String operator;

    protected boolean propertyOnLeft;

    protected String toStringTestParams() {
        return "[" + type + ", " + operator + ", " + propertyOnLeft + "]";
    }

    public BaseOperatorTest(Class type, String operator, boolean propertyOnLeft) {
        this.type = type;
        this.operator = operator;
        this.propertyOnLeft = propertyOnLeft;
    }

    @Test
    public void operatorsWithNull() throws Exception {
        String propertyName = getPropertyName(type);
        String instanceValueString = getInstanceValueString(type);
        String expression = "";
        if (propertyOnLeft) {
            expression += propertyName + " " + operator + " " + instanceValueString;
        } else {
            expression += instanceValueString + " " + operator + " " + propertyName;
        }

        System.out.println(expression);

        Map<String, Object> imports = new HashMap<String, Object>();
        imports.put(type.getSimpleName(), type);
        ParserContext pctx = new ParserContext(imports, null, "testfile");
        pctx.setStrictTypeEnforcement(true);
        pctx.setStrongTyping(true);
        pctx.addInput(propertyName, type);
        pctx.addImport("BaseOperatorTest", BaseOperatorTest.class);

        Serializable compiledExpr = MVEL.compileExpression(expression, pctx);

        VariableResolverFactory factory = new MapVariableResolverFactory(new HashMap<String, Object>());
        factory.createVariable(propertyName, null);

        try {
            Object result = MVEL.executeExpression(compiledExpr, null, factory);
            System.out.println("  => result = " + result + " : " + toStringTestParams());
            fail("Should throw NPE : " + toStringTestParams());
        } catch (NullPointerException e) {
            System.out.println("  => NPE is thrown");
            assertTrue(true);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected Exception", e);
        }
    }

    protected static String getPropertyName(Class clazz) {
        // returns a property name of ValueHolder class
        return Introspector.decapitalize(clazz.getSimpleName()) + "Value";
    }

    protected static String getInstanceValueString(Class clazz) {
        if (clazz.equals(Character.class)) {
            return "BaseOperatorTest.constantCharacterValue()"; //// Mvel converts char to String so we cannot express Character constructor
        } else {
            return "new " + clazz.getSimpleName() + "(\"0\")";
        }
    }

    protected static Throwable getRootCause(Throwable th) {
        while (th.getCause() != null) {
            th = th.getCause();
        }
        return th;
    }

    public static Character constantCharacterValue() {
        return Character.valueOf('a');
    }
}
