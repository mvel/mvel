package org.mvel3.compat.mvel2;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mvel3.MVEL;
import org.mvel3.Type;
import org.mvel3.compat.mvel2.res.Bar;
import org.mvel3.compat.mvel2.res.Cheese;
import org.mvel3.compat.mvel2.res.Foo;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.eval;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;

/**
 * Imported from org.mvel2.tests.core.ArraysTests (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged. Failures are intentionally visible; known behavioural differences
 * are annotated with {@link KnownCompatibility}.
 *
 * @author Mike Brock .
 */
@Tag("mvel2-compatibility")
public class ArraysTests extends AbstractTest {
  @Test
  public void testArrayConstructionSupport1() {
    assertTrue(test("new String[5]") instanceof String[]);
  }

  // Category not isolated: fails with a parse error (missing ';' before '}') because the block has no
  // trailing `return` (block-syntax) and `xStr` is an undeclared assignment. With `var` and `return` added,
  // `xStr.size()` on an array still fails ("Method 'size' cannot be resolved"); arrays have no size() in MVEL3.
  @Test
  @KnownCompatibility("block-syntax")
  public void testArrayConstructionSupport2() {
    assertTrue((Boolean) test("xStr = new String[5]; xStr.size() == 5"));
  }

  // Category not isolated: fails with "A reference was unexpectedly null". The expression combines a
  // single-quoted string and an undeclared assignment without `return` (block-syntax); both were seen to fail
  // separately, and which one triggers the first error was not verified.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testArrayConstructionSupport3() {
    String exp = "xStr = new String[5][5]; xStr[4][0] = 'foo'; xStr[4][0]";
    Serializable s = compileExpression(exp);

    assertEquals("foo", executeExpression(s, new HashMap()));

    assertEquals("foo",
        test(exp));
  }

  // Category not isolated: fails with "A reference was unexpectedly null". The expression combines a
  // single-quoted string and an undeclared assignment without `return` (block-syntax); both were seen to fail
  // separately, and which one triggers the first error was not verified.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testArrayConstructionSupport4() {
    assertEquals(10,
        test("xStr = new String[5][10]; xStr[4][0] = 'foo'; xStr[4].length"));
  }

  @Test
  @KnownCompatibility("single-quote-string")
  public void testArrayDefinitionWithInitializer() {
    String[] compareTo = new String[]{"foo", "bar"};
    String[] results = (String[]) eval("new String[] { 'foo', 'bar' }");

    for (int i = 0; i < compareTo.length; i++) {
      if (!compareTo[i].equals(results[i])) throw new AssertionError("arrays do not match.");
    }
  }

  @Test
  public void testArrayDefinitionWithCoercion() {
    Serializable expr = compileExpression("new double[] { 1,2,3,4 }");
    double[] d = (double[]) executeExpression(expr);
    assertEquals(2d,
        d[1]);
    assertEquals(2d, ((double[]) executeExpression(expr))[1]);
  }

  @Test
  public void testArrayDefinitionWithCoercion2() {
    Serializable expr = compileExpression( "new float[] { 1,2,3,4 }" );
    float[] d = (float[]) executeExpression( expr );
    assertEquals(2f,
        d[1]);
    assertEquals(2f, ((float[]) executeExpression(expr))[1]);
  }

  @Test
  public void testArrayDefinitionWithCoercionBoolean() {
      Serializable expr = compileExpression( "new boolean[] { false, true, false }" );
      assertFalse(((boolean[]) executeExpression(expr))[0]);
      assertTrue(((boolean[]) executeExpression(expr))[1]);
  }

  @Test
  public void testArrayDefinitionWithAutoBoxing() {
      Serializable expr = compileExpression( "new Boolean[] { !true, true, !!false }" );
      assertFalse(((Boolean[]) executeExpression(expr))[0]);
      assertTrue(((Boolean[]) executeExpression(expr))[1]);
      assertFalse(((Boolean[]) executeExpression(expr))[2]);
  }

  @Test
  public void testArrayDefinitionWithCoercionInt() {
      Serializable expr = compileExpression( "new int[] { 0, 1, 2 }" );
      assertEquals(0, ((int[]) executeExpression(expr))[0]);
      assertEquals(1, ((int[]) executeExpression(expr))[1]);
  }

  @Test
  public void testArrayDefinitionWithCoercionShort() {
      Serializable expr = compileExpression( "new short[] { 0, 1, 2 }" );
      assertEquals(0, ((short[]) executeExpression(expr))[0]);
      assertEquals(1, ((short[]) executeExpression(expr))[1]);
  }

  @Test
  public void testArrayCreation2() {
    String[][] s = (String[][]) test("new String[][] {{\"2008-04-01\", \"2008-05-10\"}," +
        " {\"2007-03-01\", \"2007-02-12\"}}");
    assertEquals("2007-03-01",
        s[1][0]);
  }

  @Test
  public void testArrayCreation3() {
    // MVEL2 optimizer selection (OptimizerFactory.setDefaultOptimizer("ASM")) has no MVEL3 equivalent.

    Serializable ce = compileExpression("new String[][] {{\"2008-04-01\", \"2008-05-10\"}," +
        " {\"2007-03-01\", \"2007-02-12\"}}");

    String[][] s = (String[][]) executeExpression(ce);

    assertEquals("2007-03-01",
        s[1][0]);
  }

  @Test
  public void testArrayCreation4() {
    String[][] s = (String[][]) test("new String[][]{{\"2008-04-01\", \"2008-05-10\"}," +
        " {\"2007-03-01\", \"2007-02-12\"}}");
    assertEquals("2007-03-01",
        s[1][0]);
  }

  // Category not isolated: MVEL3 has no compileSetExpression/executeSetExpression. The test adapter emulates
  // them by compiling `<expr> = setValue` against a POJO root; this fails with an NPE in JavaParser
  // ("method" is null). Whether the cause is the adapter emulation, POJO-root property resolution or MVEL3 itself
  // was not isolated. The equivalent map-based assignment passes, and the String "12" is coerced to Integer
  // (see ArraysAdaptedTest).
  @Test
  @KnownCompatibility("set-expression-api")
  public void testArrayCoercion1() {
    TestCompilerContext ctx = new TestCompilerContext();
    // ctx.setStrongTyping(true) removed: MVEL3 has no such mode.
    ctx.addInput("bar",
        Bar.class);

    Serializable s = Mvel3TestSupport.compileSetExpression("bar.intarray[0]",
        ctx);

    Foo foo = new Foo();

    Mvel3TestSupport.executeSetExpression(s,
        foo,
        "12");

    assertEquals(12,
        foo.getBar().getIntarray()[0].intValue());

    foo = new Foo();

    Mvel3TestSupport.executeSetExpression(s,
        foo,
        "13");

    assertEquals(13,
        foo.getBar().getIntarray()[0].intValue());

    // MVEL2 optimizer selection (OptimizerFactory.setDefaultOptimizer("ASM")) has no MVEL3 equivalent.

    ctx = new TestCompilerContext();
    ctx.addInput("bar",
        Bar.class);

    s = Mvel3TestSupport.compileSetExpression("bar.intarray[0]",
        ctx);

    foo = new Foo();

    Mvel3TestSupport.executeSetExpression(s,
        foo,
        "12");

    assertEquals(12,
        foo.getBar().getIntarray()[0].intValue());

    Mvel3TestSupport.executeSetExpression(s,
        foo,
        "13");

    assertEquals(13,
        foo.getBar().getIntarray()[0].intValue());
  }

  @Test
  public void testArrayLength() {
    TestCompilerContext context = new TestCompilerContext();
    context.addInput("x",
        String[].class);
    // The shared compileExpression() is lazy and would not compile anything here, so the
    // original compile-only check is made eager (see README "Migrating compile-only tests").
    new MVEL().compileMapExpression("x.length", Object.class, Set.of(),
        Map.of("x", Type.type(String[].class)));
  }

  // 'brie' is read as a char literal and the assignment silently stores null (no error); with "brie" the
  // test passes.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testMultiDimensionalArrayType() {
    String str = "$c.cheeses[0][0] = new Cheese('brie', 15)";

    TestCompilerContext pconf = new TestCompilerContext();
    pconf.addImport(Cheese.class);

    TestCompilerContext pctx = new TestCompilerContext(pconf);
    pctx.addInput( "$c", Column.class );

    Serializable stmt = compileExpression(str, pctx);
    Map<String,Object> vars = new HashMap<String, Object>();
    Column c = new Column("x", 1);
    c.setCheeses( new Cheese[5][5] );
    vars.put( "$c", c );
    executeExpression(stmt, null, vars);
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

  @Test
  @KnownCompatibility("block-syntax")
  public void testAssignmentOnTwoDimensionalArrayUsingIndexedInput() {
    TestCompilerContext pctx = new TestCompilerContext();
    // setIndexAllocation / setStrictTypeEnforcement / setStrongTyping / addIndexedInput removed: no MVEL3 equivalent.
    pctx.addInput("array", Double[][].class);
    Map vars = new HashMap() {{ put("array", new Double[2][2]); }};
    assertEquals(42.0, executeExpression(compileExpression("array[1][1] = 42.0;\narray[1][1]", pctx), vars));
  }
}
