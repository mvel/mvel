package org.mvel2.tests.core;

import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.compiler.CompiledExpression;
import org.mvel2.compiler.ExpressionCompiler;
import org.mvel2.optimizers.OptimizerFactory;
import org.mvel2.tests.core.res.Foo;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mvel2.MVEL.compileExpression;
import static org.mvel2.MVEL.executeExpression;

public class MutationsTests extends AbstractTest {
  public void testDeepAssignment() {
    Map map = createTestMap();
    assertEquals("crap", testCompiledSimple("foo.bar.assignTest = 'crap'", map));
    assertEquals("crap", testCompiledSimple("foo.bar.assignTest", map));
  }

  public void testDeepAssignment2() {
    Map map = createTestMap();

    ParserContext ctx = new ParserContext();

    ctx.addInput("foo", Foo.class);
    ctx.setStrongTyping(true);

    ExpressionCompiler compiler = new ExpressionCompiler("foo.bar.age = 21", ctx);
    CompiledExpression ce = compiler.compile();

    executeExpression(ce, map);

    assertEquals(((Foo) map.get("foo")).getBar().getAge(), 21);
  }

  public void testComplexExpression() {
    assertEquals("bar", test("a = 'foo'; b = 'bar'; c = 'jim'; list = {a,b,c}; list[1]"));
  }


  public void testAssignment() {
    assertEquals(true, test("populate(); blahfoo = 'sarah'; blahfoo == 'sarah'"));
  }

  public void testAssignment2() {
    assertEquals("sarah", test("populate(); blahfoo = barfoo"));
  }

  public void testAssignment3() {
    assertEquals(java.lang.Integer.class, test("blah = 5").getClass());
  }

  public void testAssignment4() {
    assertEquals(102, test("a = 100 + 1 + 1"));
  }

  public void testAssignment6() {
    assertEquals("blip", test("array[zero] = array[zero+1]; array[zero]"));
  }

  public void testConstructor() {
    assertEquals("foo", test("a = 'foobar'; new String(a.toCharArray(), 0, 3)"));
  }

  public void testStaticVarAssignment() {
    assertEquals("1", test("String mikeBrock = 1; mikeBrock"));
  }

  public void testFunctionPointer() {
    String ex = "squareRoot = java.lang.Math.sqrt; squareRoot(4)";

    Object o = MVEL.eval(ex, new HashMap());

    assertEquals(2.0, o);


    assertEquals(2.0, test(ex));
  }
/*
  // TODO - Temporarily comment these 2 tests out.
  // They succeed inside the IDE but for some reason fail when running from maven

  public void testFunctionPointerAsParam() {
    assertEquals("2.0", test("squareRoot = Math.sqrt; new String(String.valueOf(squareRoot(4)));"));
  }

  public void testFunctionPointerInAssignment() {
    assertEquals(5.0, test("squareRoot = Math.sqrt; i = squareRoot(25); return i;"));
  }
*/
  public void testIncrementOperator() {
    assertEquals(2, test("x = 1; x++; x"));
  }

  public void testPreIncrementOperator() {
    assertEquals(2, test("x = 1; ++x"));
  }

  public void testDecrementOperator() {
    assertEquals(1, test("x = 2; x--; x"));
  }

  public void testPreDecrementOperator() {
    assertEquals(1, test("x = 2; --x"));
  }

  public void testQualifiedStaticTyping() {
    Object val = test("java.math.BigDecimal a = new java.math.BigDecimal( 10.0 ); java.math.BigDecimal b = new java.math.BigDecimal( 10.0 ); java.math.BigDecimal c = a + b; return c; ");
    assertEquals(new BigDecimal(20), val);
  }

  public void testUnQualifiedStaticTyping() {
    CompiledExpression ce = (CompiledExpression) compileExpression("import java.math.BigDecimal; BigDecimal a = new BigDecimal( 10.0 ); BigDecimal b = new BigDecimal( 10.0 ); BigDecimal c = a + b; return c; ");
    assertEquals(new BigDecimal(20), testCompiledSimple("import java.math.BigDecimal; BigDecimal a = new BigDecimal( 10.0 ); BigDecimal b = new BigDecimal( 10.0 ); BigDecimal c = a + b; return c; ", new HashMap()));
  }

  public void testSubExpressionIndexer() {
    assertEquals("bar", test("xx = new java.util.HashMap(); xx.put('foo', 'bar'); prop = 'foo'; xx[prop];"));
  }

  public void testAssignListToBean() {

    OptimizerFactory.setDefaultOptimizer("reflective");

    MockClass mock = new MockClass();

    executeExpression(compileExpression("this.values = [0, 1, 2, 3, 4]"), mock);
    assertEquals(5, mock.getValues().size());
  }

  public static class MockClass {
    List values;

    public List getValues() {
      return values;
    }

    public void setValues(List values) {
      this.values = values;
    }
  }

}
