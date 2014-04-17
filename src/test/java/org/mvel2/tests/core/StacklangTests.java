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
    assertEquals("foobar",
        MVEL.executeExpression(MVEL.compileExpression("stacklang{push 'foo';push 'bar';push 0}")));
    assertEquals(50, MVEL.eval("stacklang{push 10;push 5;push 2}"));
  }

  public void testSimple2() {
    OptimizerFactory.setDefaultOptimizer("ASM");
    MVEL.executeExpression(
        MVEL.compileExpression("java.util.Collections.emptySet instanceof java.util.Set"));
  }

  public void testSimple3() {
    String str;
    MVEL.eval(str = "stacklang {" +
        "push 10;" +
        "store i;" +
        "label loop;" +
        "load i;" +
        "push 1;" +
        "push 1; " +
        "reduce;" +
        "store i;" +
        "push 0;" +
        "push 18;" +
        "reduce;" +
        "jumpif endloop;" +
        "ldtype java.lang.System;" +
        "dup;" +
        "getfield out;" +
        "ldtype java.io.PrintStream;" +
        "load i;" +
        "invoke println;" +
        "jump loop;" +
        "" +
        "label endloop;" +
        "}", new HashMap<String, Object>());

    System.out.println(str);
  }

  public void testFoo() {

    Object eval = MVEL.eval("foo.bar", new Outer() {
      {
        foo = new TwilightZone() {

        };
      }
    });
    System.out.println(eval);
  }

  public static class Outer {
    public TwilightZone foo;
  }

  public static class TwilightZone {
    public String bar = "shit";

  }
}
