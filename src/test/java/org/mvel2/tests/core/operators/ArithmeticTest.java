package org.mvel2.tests.core.operators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ArithmeticTest extends BaseOperatorTest {

    public ArithmeticTest(Class type, String operator, boolean propertyOnLeft) {
        super(type, operator, propertyOnLeft);
    }

    private static final String[] ARITHMETIC_OPERATORS = new String[]{"+", "-", "*", "/"};

    @Parameters
    public static Collection<Object[]> ruleParams() {
        List<Object[]> parameterData = new ArrayList<Object[]>();
        for (Class type : TYPES) {
            for (String operator : ARITHMETIC_OPERATORS) {
                for (boolean propertyOnLeft : PROPERTY_ON_LEFT)
                    parameterData.add(new Object[]{type, operator, propertyOnLeft});
            }
        }

        return parameterData;
    }

}
