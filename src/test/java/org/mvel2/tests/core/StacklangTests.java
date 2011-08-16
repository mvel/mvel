package org.mvel2.tests.core;

import junit.framework.TestCase;
import org.mvel2.MVEL;
import org.mvel2.optimizers.OptimizerFactory;

import java.util.HashMap;

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

  public void testSimple3() {
    MVEL.eval("stacklang {" +
        "push 10;\n" +
        "store i;" +
        "" +
        "label loop;\n" +
        "load i;\n" +
        "push 1;\n" +
        "push 1; " +
        "reduce;\n" +
        "store i;\n" +
        "" +
        "push 0;\n" +
        "push 18;\n" +
        "reduce;" +
        "jumpif endloop;\t\n" +
        "" +
        "ldtype java.lang.System;\n" +
        "dup;\n" +
        "getfield out;\n" +
        "ldtype java.io.PrintStream;\n" +
        "load i;\n" +
        "invoke println;\n" +
        "" +
        "jump loop;\n" +
        "" +
        "label endloop;\n" +
        "}", new HashMap());
  }
}
