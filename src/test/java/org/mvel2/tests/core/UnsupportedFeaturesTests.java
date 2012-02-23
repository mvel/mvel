package org.mvel2.tests.core;

import junit.framework.TestCase;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.optimizers.OptimizerFactory;

/**
 * @author Mike Brock .
 */
public class UnsupportedFeaturesTests extends TestCase {
  public void testJavaStyleClassLiterals() {
    MVEL.COMPILER_OPT_SUPPORT_JAVA_STYLE_CLASS_LITERALS = true;

    OptimizerFactory.setDefaultOptimizer("ASM");
    assertEquals(String.class, MVEL.executeExpression(MVEL.compileExpression("String.class")));

    OptimizerFactory.setDefaultOptimizer("reflective");
    assertEquals(String.class, MVEL.executeExpression(MVEL.compileExpression("String.class")));

    OptimizerFactory.setDefaultOptimizer(OptimizerFactory.DYNAMIC);

    assertEquals(String.class, MVEL.eval("String"));
    assertEquals(String.class, MVEL.eval("java.lang.String"));
    assertEquals(java.util.ArrayList.class, MVEL.eval("java.util.ArrayList"));

    assertEquals(String.class, MVEL.eval("(String)"));
    assertEquals(String.class, MVEL.eval("(java.lang.String)"));
    assertEquals(java.util.ArrayList.class, MVEL.eval("(java.util.ArrayList)"));

    assertEquals(String.class, MVEL.eval("(String.class)"));
    assertEquals(String.class, MVEL.eval("(java.lang.String.class)"));
    assertEquals(java.util.ArrayList.class, MVEL.eval("(java.util.ArrayList.class)"));

    assertEquals(String.class, MVEL.eval("String.class"));
    assertEquals(String.class, MVEL.eval("java.lang.String.class"));
    assertEquals(java.util.ArrayList.class, MVEL.eval("java.util.ArrayList.class"));

    assertEquals(Class.class, MVEL.analyze("String", ParserContext.create()));
    assertEquals(Class.class, MVEL.analyze("String.class", ParserContext.create()));
    assertEquals(Class.class, MVEL.analyze("java.lang.String.class", ParserContext.create()));
    assertEquals(Class.class, MVEL.analyze("java.util.ArrayList.class", ParserContext.create()));

    MVEL.COMPILER_OPT_SUPPORT_JAVA_STYLE_CLASS_LITERALS = false;
  }

}
