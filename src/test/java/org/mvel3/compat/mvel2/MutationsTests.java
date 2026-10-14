package org.mvel3.compat.mvel2;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mvel3.compat.mvel2.Mvel3TestSupport.CompiledExpression;
import org.mvel3.compat.mvel2.Mvel3TestSupport.ExpressionCompiler;
import org.mvel3.compat.mvel2.res.Foo;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;

/**
 * Imported from org.mvel2.tests.core.MutationsTests (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged. Failures are intentionally visible; known behavioural differences
 * are annotated with {@link KnownCompatibility}.
 */
@Tag("mvel2-compatibility")
public class MutationsTests extends AbstractTest {
  // Category not isolated: the first expression `foo.bar.assignTest = 'crap'` has two suspected causes:
  // single-quote-string ('crap' is parsed as a char literal; the error is "Unsolved symbol in ap") and
  // property-assign-void-setter (a property assignment used as the expression value). Not tested separately.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testDeepAssignment() {
    Map map = createTestMap();
    assertEquals("crap", testCompiledSimple("foo.bar.assignTest = 'crap'", map));
    assertEquals("crap", testCompiledSimple("foo.bar.assignTest", map));
  }

  @Test
  @KnownCompatibility("property-assign-void-setter")
  public void testDeepAssignment2() {
    Map map = createTestMap();

    TestCompilerContext ctx = new TestCompilerContext();

    ctx.addInput("foo", Foo.class);

    ExpressionCompiler compiler = new ExpressionCompiler("foo.bar.age = 21", ctx);
    CompiledExpression ce = compiler.compile();

    executeExpression(ce, map);

    assertEquals(((Foo) map.get("foo")).getBar().getAge(), 21);
  }

  // Category not isolated: fails with "A reference was unexpectedly null". The expression combines
  // single-quoted strings, MVEL2 `{a,b,c}` list syntax (MVEL3 uses `[a, b, c]`), undeclared assignments,
  // no `return`, and redeclares the existing map inputs `a`, `b`, `c` (input-variable-redeclaration).
  // Which of these is the actual trigger was not verified.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testComplexExpression() {
    assertEquals("bar", test("a = 'foo'; b = 'bar'; c = 'jim'; list = {a,b,c}; list[1]"));
  }

  // Category not isolated: fails with "A reference was unexpectedly null". Besides single-quote-string,
  // the expression has an undeclared assignment (block-syntax) and calls `populate()` on the MVEL2
  // root object (`this`, see this-reference). Which of these is the actual trigger was not verified.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testAssignment() {
    assertEquals(true, test("populate(); blahfoo = 'sarah'; blahfoo == 'sarah'"));
  }

  // Category not isolated: the observed error is a parse error at the missing trailing `return`
  // (block-syntax), but the expression also calls `populate()` on the MVEL2 root object (`this`,
  // see this-reference), which would likely fail after the syntax is fixed.
  @Test
  @KnownCompatibility("block-syntax")
  public void testAssignment2() {
    assertEquals("sarah", test("populate(); blahfoo = barfoo"));
  }

  @Test
  @KnownCompatibility("block-syntax")
  public void testAssignment3() {
    assertEquals(java.lang.Integer.class, test("blah = 5").getClass());
  }

  @Test
  public void testAssignment4() {
    assertEquals(102, test("a = 100 + 1 + 1"));
  }

  @Test
  @KnownCompatibility("block-syntax")
  public void testAssignment6() {
    assertEquals("blip", test("array[zero] = array[zero+1]; array[zero]"));
  }

  // Category not isolated: block-syntax is the observed parse error. The expression also uses
  // single-quoted 'foobar' (single-quote-string); this was not tested separately.
  @Test
  @KnownCompatibility("block-syntax")
  public void testConstructor() {
    assertEquals("foo", test("a = 'foobar'; new String(a.toCharArray(), 0, 3)"));
  }

  // Category not isolated: block-syntax is the observed parse error (no `return`). The expression
  // also assigns an int to a String (`String mikeBrock = 1`), which Java rejects (see string-coercion);
  // this was not tested separately.
  @Test
  @KnownCompatibility("block-syntax")
  public void testStaticVarAssignment() {
    assertEquals("1", test("String mikeBrock = 1; mikeBrock"));
  }

  // Category not isolated: block-syntax is the observed parse error. The MVEL2 function pointer
  // (`squareRoot = java.lang.Math.sqrt; squareRoot(4)`) has no known MVEL3 equivalent, so it may
  // still fail after the syntax is fixed. Not tested separately.
  @Test
  @KnownCompatibility("block-syntax")
  public void testFunctionPointer() {
    String ex = "squareRoot = java.lang.Math.sqrt; squareRoot(4)";

    Object o = Mvel3TestSupport.eval(ex, new HashMap());

    assertEquals(2.0, o);


    assertEquals(2.0, test(ex));
  }

  @Test
  @KnownCompatibility("block-syntax")
  public void testIncrementOperator() {
    assertEquals(2, test("x = 1; x++; x"));
  }

  @Test
  @KnownCompatibility("block-syntax")
  public void testPreIncrementOperator() {
    assertEquals(2, test("x = 1; ++x"));
  }

  @Test
  @KnownCompatibility("block-syntax")
  public void testDecrementOperator() {
    assertEquals(1, test("x = 2; x--; x"));
  }

  @Test
  @KnownCompatibility("block-syntax")
  public void testPreDecrementOperator() {
    assertEquals(1, test("x = 2; --x"));
  }

  @Test
  @KnownCompatibility("input-variable-redeclaration")
  public void testQualifiedStaticTyping() {
    Object val = test("java.math.BigDecimal a = new java.math.BigDecimal( 10.0 ); java.math.BigDecimal b = new java.math.BigDecimal( 10.0 ); java.math.BigDecimal c = a + b; return c; ");
    assertEquals(new BigDecimal(20), val);
  }

  // Category not isolated: inline-import is the first (parse) error and stops compilation, so any later
  // error was not observed. The adapted test passes once the import moves to the API imports set.
  @Test
  @KnownCompatibility("inline-import")
  public void testUnQualifiedStaticTyping() {
    CompiledExpression ce = (CompiledExpression) compileExpression("import java.math.BigDecimal; BigDecimal a = new BigDecimal( 10.0 ); BigDecimal b = new BigDecimal( 10.0 ); BigDecimal c = a + b; return c; ");
    assertEquals(new BigDecimal(20), testCompiledSimple("import java.math.BigDecimal; BigDecimal a = new BigDecimal( 10.0 ); BigDecimal b = new BigDecimal( 10.0 ); BigDecimal c = a + b; return c; ", new HashMap()));
  }

  // Category not isolated: fails with a JavaParser null-node NPE ("current" is null). The expression has
  // single-quoted strings, a raw `new java.util.HashMap()`, undeclared assignments, no `return`, and
  // `xx[prop]` indexing. The adapted test passes after changing several of these at once; the actual
  // trigger was not isolated.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testSubExpressionIndexer() {
    assertEquals("bar", test("xx = new java.util.HashMap(); xx.put('foo', 'bar'); prop = 'foo'; xx[prop];"));
  }

  @Test
  @KnownCompatibility("this-reference")
  public void testAssignListToBean() {

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
