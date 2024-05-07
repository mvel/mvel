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
public class EqualityComparisonTest extends BaseOperatorsTest {

    public EqualityComparisonTest(Class type, String operator, boolean nullPropertyOnLeft) {
        super(type, operator, nullPropertyOnLeft);
    }

    @Parameters
    public static Collection<Object[]> ruleParams() {
        List<Object[]> parameterData = new ArrayList<Object[]>();
        for (Class type : TYPES) {
            for (String operator : EQUALITY_COMPARISON_OPERATORS) {
                for (boolean nullPropertyOnLeft : NULL_PROPERTY_ON_LEFT)
                    parameterData.add(new Object[]{type, operator, nullPropertyOnLeft});
            }
        }

        return parameterData;
    }

    @Test
    public void compareWithNullProperty() throws Exception {
        String propertyName = getPropertyName(type);
        String instanceValueString = getInstanceValueString(type);
        String expression = "";
        if (nullPropertyOnLeft) {
            expression += propertyName + " " + operator + " " + instanceValueString;
        } else {
            expression += instanceValueString + " " + operator + " " + propertyName;
        }

        Map<String, Object> imports = new HashMap<String, Object>();
        imports.put(type.getSimpleName(), type);
        ParserContext pctx = new ParserContext(imports, null, "testfile");
        pctx.setStrictTypeEnforcement(true);
        pctx.setStrongTyping(true);
        pctx.addInput(propertyName, type);
        pctx.addImport("BaseOperatorTest", BaseOperatorsTest.class);

        Serializable compiledExpr = MVEL.compileExpression(expression, pctx);

        VariableResolverFactory factory = new MapVariableResolverFactory(new HashMap<String, Object>());
        factory.createVariable(propertyName, null);

        Object result = MVEL.executeExpression(compiledExpr, null, factory);
        if (operator.equals("==") && result.equals(false) || operator.equals("!=") && result.equals(true)) {
            assertTrue(true);
        } else {
            fail("Wrong result");
        }
    }
}
