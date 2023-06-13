package org.mvel2.tests.core;

import org.mvel2.tests.core.res.Cheese;
import org.mvel2.tests.core.res.Foo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Mike Brock .
 */
public class ArraysTests extends AbstractTest {
  public void testArrayConstructionSupport1() {
    assertTrue(test("new String[5]") instanceof String[]);
  }

  public void testArrayConstructionSupport2() {
    assertTrue((Boolean) test("var xStr = new String[5]; return xStr.length == 5;"));
  }

  public void testArrayConstructionSupport3() {
    String exp = "var xStr = new String[5][5]; xStr[4][0] = \"foo\"; xStr[4][0];";

    assertEquals("foo", eval(exp));

    assertEquals("foo",
        test(exp));
  }

  public void testArrayConstructionSupport4() {
    assertEquals(10,
        test("var xStr = new String[5][10]; xStr[4][0] = \"foo\"; xStr[4].length;"));
  }

  public void testArrayDefinitionWithInitializer() {
    String[] compareTo = new String[]{"foo", "bar"};
    String[] results = (String[]) eval("new String[] { \"foo\", \"bar\" }");

    for (int i = 0; i < compareTo.length; i++) {
      if (!compareTo[i].equals(results[i])) throw new AssertionError("arrays do not match.");
    }
  }

  public void testArrayDefinitionWithCoercion() {
    String expr = "new double[] { 1,2,3,4 }";
    double[] d = (double[]) eval(expr);
    assertEquals(2d, d[1]);
    assertEquals(2d, ((double[]) eval(expr))[1]);
  }

  public void testArrayDefinitionWithCoercion2() {
    String expr = "new float[] { 1,2,3,4 }";
    float[] d = (float[]) eval( expr );

    assertEquals(2f, d[1]);
    assertEquals(2f, ((float[]) eval(expr))[1]);
  }

  public void testArrayDefinitionWithCoercionBoolean() {
      String expr = "new boolean[] { false, true, false }";
      assertFalse(((boolean[]) eval(expr))[0]);
      assertTrue(((boolean[]) eval(expr))[1]);
  }

  public void testArrayDefinitionWithAutoBoxing() {
      String expr = "new Boolean[] { !true, true, !!false }";
      assertFalse(((Boolean[]) eval(expr))[0]);
      assertTrue(((Boolean[]) eval(expr))[1]);
      assertFalse(((Boolean[]) eval(expr))[2]);
  }

  public void testArrayDefinitionWithCoercionInt() {
      String expr = "new int[] { 0, 1, 2 }";
      assertEquals(0, ((int[]) eval(expr))[0]);
      assertEquals(1, ((int[]) eval(expr))[1]);
  }
  
  public void testArrayDefinitionWithCoercionShort() {
      String expr = "new short[] { 0, 1, 2 }";
      assertEquals(0, ((short[]) eval(expr))[0]);
      assertEquals(1, ((short[]) eval(expr))[1]);
  }

  public void testArrayCreation() {
    String[][] s = (String[][]) test("new String[][]{{\"2008-04-01\", \"2008-05-10\"}, {\"2007-03-01\", \"2007-02-12\"}}");
    assertEquals("2007-03-01", s[1][0]);
  }

  public void testArrayCoercion1() {
    Foo foo = new Foo();

    eval("bar.intarray[0] = \"12\"", foo, null);

    assertEquals(12,
        foo.getBar().getIntarray()[0].intValue());

    eval("bar.intarray[0] = \"13\"", foo, null);

    assertEquals(13,
        foo.getBar().getIntarray()[0].intValue());
  }

  public void testArrayLength() {
    String[] x = new String[] {"11111", "2222"};
    Map<String, Object> vars = new HashMap<>();
    vars.put("x", x);

    assertEquals(2, eval("x.length;", null, vars));
  }

  public void testMultiDimensionalArrayType() {
    String str = "$c#Column#.cheeses[0][0] = new Cheese(\"brie\", 15);";

    Set<String> imports = new HashSet<>();
    imports.add(Cheese.class.getCanonicalName());
    imports.add(Column.class.getCanonicalName());

    Map<String,Object> vars = new HashMap<String, Object>();
    Column c = new Column("x", 1);
    c.setCheeses( new Cheese[5][5] );
    vars.put( "$c", c );

    eval(str, null, vars, imports);
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
    Map vars = new HashMap() {{ put("array", new Double[2][2]); }};
    assertEquals(42.0, eval("array[1][1] = 42.0;\narray[1][1];", null, vars));
  }
}
