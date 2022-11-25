package org.mvel2.tests.core.operators;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class EqualityComparisonNullCheckTest extends BaseOperatorTest {

    public EqualityComparisonNullCheckTest(Class type, String operator, boolean propertyOnLeft) {
        super(type, operator, propertyOnLeft);
    }

    private static final String[] EQUALITY_COMPARISON_OPERATORS = new String[]{"==", "!="};

    @Parameters
    public static Collection<Object[]> ruleParams() {
        List<Object[]> parameterData = new ArrayList<Object[]>();
        for (Class type : TYPES) {
            for (String operator : EQUALITY_COMPARISON_OPERATORS) {
                for (boolean propertyOnLeft : PROPERTY_ON_LEFT)
                    parameterData.add(new Object[]{type, operator, propertyOnLeft});
            }
        }

        return parameterData;
    }

    @Test
    public void operatorsWithNull() throws Exception {
        String propertyName = getPropertyName(type);
        String expression = "";
        if (propertyOnLeft) {
            expression += propertyName + " " + operator + " null";
        } else {
            expression += "null " + operator + " " + propertyName;
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
            if (operator.equals("==") && result.equals(true) || operator.equals("!=") && result.equals(false)) {
                assertTrue(true);
            } else {
                fail("Wrong result");
            }
        } catch (Exception e) {
            throw new RuntimeException("Unexpected Exception", e);
        }
    }
}
