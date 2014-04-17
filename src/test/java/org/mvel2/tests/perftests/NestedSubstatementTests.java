package org.mvel2.tests.perftests;

import junit.framework.TestCase;

import org.mvel2.MVEL;

public class NestedSubstatementTests extends TestCase {

  public static final int MAX = 25;

  public void testBinary() {
    for (int n = 1; n < MAX; n++) {
      // long t = System.currentTimeMillis();
      String expr = "e";
      for (int i = n; i > 0; i--) {
        expr = String.format("t%d && (%s)", i, expr);
      }
      // System.out.println(expr);
      MVEL.compileExpression(expr);
      // long d = System.currentTimeMillis() - t;
      // System.out.println(String.format("n=%d, t=%dms", n, d));
    }
  }

  public void testTernary() {
    for (int n = 1; n < MAX; n++) {
      // long t = System.currentTimeMillis();
      String expr = "e";
      for (int i = n; i > 0; i--) {
        expr = String.format("t%d ? r%d : (%s)", i, i, expr);
      }
      // System.out.println(expr);
      MVEL.compileExpression(expr);
      // long d = System.currentTimeMillis() - t;
      // System.out.println(String.format("n=%d, t=%dms", n, d));
    }
  }
}
