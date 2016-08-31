package org.mvel2.tests.core.osgi;

import junit.framework.TestCase;
import org.mvel2.MVEL;
import org.mvel2.ParserConfiguration;
import org.mvel2.ParserContext;
import org.mvel2.optimizers.dynamic.DynamicOptimizer;

import java.io.Serializable;

public class AsmOptimizerOsgiTest extends TestCase {

    private static ClassLoader NO_MVEL_CL = new NoMvelClassLoader();

    private static class NoMvelClassLoader extends ClassLoader {
        public NoMvelClassLoader() {
            super(null);
        }
    };

    public void testCollectionAccessWithInvalidThreadClassLoader() {
        String expression = "['A', 'B', 'C'] contains 'B'";

        ParserConfiguration parserConfiguration = new ParserConfiguration();
        parserConfiguration.setClassLoader(MVEL.class.getClassLoader());
        Serializable compiledExpression = MVEL.compileExpression(expression, new ParserContext(parserConfiguration));

        ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(NO_MVEL_CL);

            for (int i = 0; i <= DynamicOptimizer.tenuringThreshold; i++) {
                Object result = MVEL.executeExpression(compiledExpression);
                assertEquals(Boolean.TRUE, result);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(currentCl);
        }
    }
}
