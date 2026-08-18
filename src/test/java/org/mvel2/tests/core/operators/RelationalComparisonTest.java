package org.mvel2.tests.core.operators;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class RelationalComparisonTest extends BaseOperatorsTest {

    public RelationalComparisonTest(Class type, String operator, boolean nullPropertyOnLeft) {
        super(type, operator, nullPropertyOnLeft);
    }

    @Parameters
    public static Collection<Object[]> ruleParams() {
        List<Object[]> parameterData = new ArrayList<Object[]>();
        for (Class type : TYPES) {
            for (String operator : RELATIONAL_COMPARISON_OPERATORS) {
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
        assertEquals("Comparison with null property should be false: " + expression, false, result);
    }
}