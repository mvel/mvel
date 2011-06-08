package org.mvel2.tests.core;

import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.optimizers.OptimizerFactory;
import org.mvel2.tests.core.res.Bar;
import org.mvel2.tests.core.res.Foo;
import org.mvel2.util.ParseTools;

import java.io.Serializable;
import java.util.HashMap;

import static org.mvel2.MVEL.*;
import static org.mvel2.MVEL.compileSetExpression;
import static org.mvel2.MVEL.executeSetExpression;

/**
 * @author Mike Brock .
 */
public class ArraysTests extends AbstractTest {
  public void testArrayConstructionSupport1() {
    assertTrue(test("new String[5]") instanceof String[]);
  }

  public void testArrayConstructionSupport2() {
    assertTrue((Boolean) test("xStr = new String[5]; xStr.size() == 5"));
  }

  public void testArrayConstructionSupport3() {
    String exp = "xStr = new String[5][5]; xStr[4][0] = 'foo'; xStr[4][0]";
    Serializable s = MVEL.compileExpression(exp);

    assertEquals("foo", MVEL.executeExpression(s, new HashMap()));

    assertEquals("foo",
            test(exp));
  }

  public void testArrayConstructionSupport4() {
    assertEquals(10,
            test("xStr = new String[5][10]; xStr[4][0] = 'foo'; xStr[4].length"));
  }

  public void testArrayDefinitionWithInitializer() {
    String[] compareTo = new String[]{"foo", "bar"};
    String[] results = (String[]) MVEL.eval("new String[] { 'foo', 'bar' }");

    for (int i = 0; i < compareTo.length; i++) {
      if (!compareTo[i].equals(results[i])) throw new AssertionError("arrays do not match.");
    }
  }

  public void testArrayDefinitionWithCoercion() {
    Double[] d = (Double[]) MVEL.executeExpression(MVEL.compileExpression("new double[] { 1,2,3,4 }"));
    assertEquals(2d,
            d[1]);
  }

  public void testArrayDefinitionWithCoercion2() {
    Float[] d = (Float[]) test("new float[] { 1,2,3,4 }");
    assertEquals(2f,
            d[1]);
  }

  public void testArrayCreation2() {
    String[][] s = (String[][]) test("new String[][] {{\"2008-04-01\", \"2008-05-10\"}," +
            " {\"2007-03-01\", \"2007-02-12\"}}");
    assertEquals("2007-03-01",
            s[1][0]);
  }

  public void testArrayCreation3() {
    OptimizerFactory.setDefaultOptimizer("ASM");

    Serializable ce = compileExpression("new String[][] {{\"2008-04-01\", \"2008-05-10\"}," +
            " {\"2007-03-01\", \"2007-02-12\"}}");

    String[][] s = (String[][]) executeExpression(ce);

    assertEquals("2007-03-01",
            s[1][0]);
  }

  public void testArrayCreation4() {
    String[][] s = (String[][]) test("new String[][]{{\"2008-04-01\", \"2008-05-10\"}," +
            " {\"2007-03-01\", \"2007-02-12\"}}");
    assertEquals("2007-03-01",
            s[1][0]);
  }

  public void testArrayCoercion1() {
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("bar",
            Bar.class);

    Serializable s = compileSetExpression("bar.intarray[0]",
            ctx);

    Foo foo = new Foo();

    executeSetExpression(s,
            foo,
            "12");

    assertEquals(12,
            foo.getBar().getIntarray()[0].intValue());

    foo = new Foo();

    executeSetExpression(s,
            foo,
            "13");

    assertEquals(13,
            foo.getBar().getIntarray()[0].intValue());

    OptimizerFactory.setDefaultOptimizer("ASM");

    ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addInput("bar",
            Bar.class);

    s = compileSetExpression("bar.intarray[0]",
            ctx);

    foo = new Foo();

    executeSetExpression(s,
            foo,
            "12");

    assertEquals(12,
            foo.getBar().getIntarray()[0].intValue());

    executeSetExpression(s,
            foo,
            "13");

    assertEquals(13,
            foo.getBar().getIntarray()[0].intValue());
  }

  public void testArrayLength() {
    ParserContext context = new ParserContext();
    context.setStrongTyping(true);
    context.addInput("x",
            String[].class);
    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression("x.length", context);
  }

}
