package org.mvel2.tests.core;

import org.mvel2.MVEL;
import org.mvel2.ParserConfiguration;
import org.mvel2.ParserContext;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.optimizers.OptimizerFactory;
import org.mvel2.tests.core.res.Bar;
import org.mvel2.tests.core.res.Cheese;
import org.mvel2.tests.core.res.Foo;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.mvel2.MVEL.*;

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
    double[] d = (double[]) MVEL.executeExpression(MVEL.compileExpression("new double[] { 1,2,3,4 }"));
    assertEquals(2d,
        d[1]);
  }

  public void testArrayDefinitionWithCoercion2() {
    float[] d = (float[]) MVEL.executeExpression( MVEL.compileExpression( "new float[] { 1,2,3,4 }" ) );
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

  public void testMultiDimensionalArrayType() {
    String str = "$c.cheeses[0][0] = new Cheese('brie', 15)";

    ParserConfiguration pconf = new ParserConfiguration();
    pconf.addImport(Cheese.class);

    ParserContext pctx = new ParserContext(pconf);
    pctx.addInput( "$c", Column.class );
    pctx.setStrongTyping(true);

    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);
    Map<String,Object> vars = new HashMap<String, Object>();
    Column c = new Column("x", 1);
    c.setCheeses( new Cheese[5][5] );
    vars.put( "$c", c );
    MVEL.executeExpression(stmt, null, vars);
    assertEquals( new Cheese("brie", 15), c.getCheeses()[0][0]);
  }

  public class Column {
    private String name;
    private int length;

    private Cheese[][] cheeses;

    public Cheese[][] getCheeses() {
      return cheeses;
    }

    public void setCheeses(Cheese[][] cheeses) {
      this.cheeses = cheeses;
    }

    public Column(String name, int length) {
      this.name = name;
      this.length = length;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public int getLength() {
      return length;
    }

    public void setLength(int length) {
      this.length = length;
    }
  }

  public void testAssignmentOnTwoDimensionalArrayUsingIndexedInput() {
    ParserConfiguration conf = new ParserConfiguration();
    ParserContext pctx = new ParserContext( conf );
    pctx.setIndexAllocation( true );
    pctx.setStrictTypeEnforcement(true);
    pctx.setStrongTyping(true);
    pctx.addInput("array", Double[][].class);
    pctx.addIndexedInput("array");
    Map vars = new HashMap() {{ put("array", new Double[2][2]); }};
    assertEquals(42.0, MVEL.executeExpression(MVEL.compileExpression("array[1][1] = 42.0;\narray[1][1]", pctx), vars));
  }
}
