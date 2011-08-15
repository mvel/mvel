package org.mvel2.tests.core;

import junit.framework.TestCase;
import org.mvel2.MVEL;
import org.mvel2.optimizers.OptimizerFactory;

/**
 * @author Mike Brock <cbrock@redhat.com>
 */
public class StacklangTests extends TestCase {

  public void testSimple1() {
    assertEquals("foobar", MVEL.executeExpression(MVEL.compileExpression("stacklang{push 'foo';push 'bar';push 0}")));
    assertEquals(50, MVEL.eval("stacklang{push 10;push 5;push 2}"));
  }

  public void testSimple2() {
    OptimizerFactory.setDefaultOptimizer("ASM");
    MVEL.executeExpression(MVEL.compileExpression("java.util.Collections.emptySet instanceof java.util.Set"));
  }
}
