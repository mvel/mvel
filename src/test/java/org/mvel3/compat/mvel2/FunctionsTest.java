package org.mvel3.compat.mvel2;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.eval;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;

/**
 * Imported from org.mvel2.tests.core.FunctionsTest (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged. Failures are intentionally visible; known behavioural differences
 * are annotated with {@link KnownCompatibility}.
 *
 * <p>Not imported (they exercise MVEL2-only APIs that have no MVEL3 counterpart):
 * <ul>
 *   <li>{@code testFunctionDefAndCall2}: {@code CompilerTools.extractAllDeclaredFunctions}
 *       and the reflective optimizer.</li>
 *   <li>{@code testFunctionReuse}: sharing declared functions between evaluations through a
 *       chained {@code VariableResolverFactory}.</li>
 *   <li>{@code testFunctionReuseMultiThread}: the same function sharing through
 *       {@code SharedFuncLib}, run on four threads.</li>
 * </ul>
 */
@Tag("mvel2-compatibility")
public class FunctionsTest extends AbstractTest {

  // Category not isolated: the parse error is reported at `def fun(`. The body also uses a projection
  // `($ in [..] if ..)` (projection-operator); that was not tested separately.
  @Test
  @KnownCompatibility("function-declaration")
  public final void testThatFunctionsCloseOverArguments() {
    final Object o = eval(
        "def fun(x) { ($ in [1, 2, 3] if $ > x) }" +
            "" +
            "fun(0)",
        new HashMap<String, Object>()
    );

    assertTrue(o instanceof List);
    assertEquals(Arrays.asList(1, 2, 3), o);
  }

  // Category not isolated: the parse error is reported at the function declaration. The expression also uses
  // single-quoted strings (single-quote-string); that was not tested separately.
  @Test
  @KnownCompatibility("function-declaration")
  public void testFunctionDefAndCall() {
    assertEquals("FoobarFoobar",
        test("function heyFoo() { return 'Foobar'; };\n" +
            "return heyFoo() + heyFoo();"));
  }

  // Category not isolated: the parse error is reported at the function declaration. The expression also uses
  // single-quoted strings (single-quote-string); that was not tested separately.
  @Test
  @KnownCompatibility("function-declaration")
  public void testFunctionDefAndCall1() {
    assertEquals("FoobarFoobar", eval("function heyFoo() { return 'Foobar'; };\n"
        + "return heyFoo() + heyFoo();", new HashMap()));
  }

  // Category not isolated: fails with "A reference was unexpectedly null" (JavaParser AssignExpr), not a parse error.
  // The expression has a function declaration, undeclared assignments (block-syntax) and single-quoted strings
  // (single-quote-string); which one triggers it was not verified.
  @Test
  @KnownCompatibility("function-declaration")
  public void testFunctionDefAndCall3() {
    assertEquals("FOOBAR", test("function testFunction() { a = 'foo'; b = 'bar'; a + b; }; testFunction().toUpperCase();  "));
  }

  // Category not isolated: the parse error is reported at the function declaration. The expression also uses
  // single-quoted strings (single-quote-string); that was not tested separately.
  @Test
  @KnownCompatibility("function-declaration")
  public void testFunctionDefAndCall4() {
    assertEquals("barfoo", test("function testFunction(input) { return input; }; testFunction('barfoo');"));
  }

  @Test
  @KnownCompatibility("function-declaration")
  public void testFunctionDefAndCall5() {
    assertEquals(10, test("function testFunction(x, y) { return x + y; }; testFunction(7, 3);"));
  }

  // Category not isolated: the parse error is reported at the function declaration. The expression also uses
  // single-quoted strings (single-quote-string); that was not tested separately.
  @Test
  @KnownCompatibility("function-declaration")
  public void testFunctionDefAndCall6() {
    assertEquals("foo", eval("def fooFunction(x) x; fooFunction('foo')", new HashMap()));
  }

  // Category not isolated: fails with a null parse tree. The expression has an anonymous function and a
  // single-quoted string (single-quote-string); with a double-quoted string the error is
  // "A string was unexpectedly empty". Which one triggers it was not verified further.
  @Test
  @KnownCompatibility("function-declaration")
  public void testAnonymousFunction() {
    assertEquals("foobar", test("a = function { 'foobar' }; a();"));
  }

  // Category not isolated: fails with a JavaParser NPE ("current" is null). The expression has function
  // declarations, a `foreach` statement (foreach-syntax / javaparser-null-node) and single-quoted strings
  // (single-quote-string); which one triggers it was not verified.
  @Test
  @KnownCompatibility("function-declaration")
  public void testJIRA207() {
    String ex = "x = 0; y = 0;" +
        "def foo() { x = 1; System.out.println('Word up'); }\n" +
        "def bar() { y = 1;  System.out.println('Peace out'); }\n" +
        "def doMany(fps) {\n" +
        "foreach(f : fps) { System.out.println(f); f(); }\n" +
        "}\n" +
        "doMany([foo,bar]);" +
        "x == 1 && y == 1;";

    Boolean bool;

    // MVEL2 optimizer selection (OptimizerFactory.setDefaultOptimizer("ASM")) has no MVEL3 equivalent.
    Serializable s = compileExpression(ex);

    bool = (Boolean) executeExpression(s, new HashMap());
    assertTrue(bool);
  }

  @Test
  @KnownCompatibility("function-declaration")
  public void testBranchesWithReturn() {
    String ex = "function max($a, $b) {\n" +
        " if ($a>$b){\n" +
        "    System.out.println($a);\n" +
        "    return $a;\n" +
        "} else {\n" +
        "   System.out.println($b);\n" +
        "   return $b;\n" +
        "};\n" +
        "}; val = max(20, 30);";

    Serializable s = compileExpression(ex);
    Map<String, Object> map = new HashMap<String, Object>();
    executeExpression(s, map);

    assertEquals(30, map.get("val"));
  }

  public static class TestClassAZZ {
    public String hey() {
      return "Heythere!";
    }
  }

  @Test
  @KnownCompatibility("function-declaration")
  public void testCallGlobalStaticFunctionFromMVELFunction() {
    TestClassAZZ azz = new TestClassAZZ();

    String expr = "def foobie12345() { hey(); } foobie12345();";

    assertEquals("Heythere!", eval(expr, azz, new HashMap<String, Object>()));
  }

  @Test
  @KnownCompatibility("function-declaration")
  public void testDeepNestedLoopsInFunction() {
    assertEquals(10,
        test("def increment(i) { i + 1 }; def ff(i) { x = 0; while (i < 1) { " + "x++; " +
            "while (i < 10) { i = increment(i); } }; if (x == 1) return i; else -1; }; i = 0; ff(i);"));
  }

  @Test
  @KnownCompatibility("function-declaration")
  public void testFunctions5() {
    String exp = "def foo(a,b) { a + b }; foo(1.5,5.25)";
    System.out.println(eval(exp,
        new HashMap()));
  }

  @Test
  @KnownCompatibility("function-declaration")
  public void testJIRA174() {
    // MVEL2 optimizer selection (OptimizerFactory.setDefaultOptimizer("ASM")) has no MVEL3 equivalent.

    Serializable s = compileExpression("def test(a1) { java.util.Collection a = a1; a.clear(); a.add(1); a.add(2); a.add(3); a.remove((Object) 2); a; }\n" +
        "a = test(new java.util.ArrayList());\n" +
        "b = test(new java.util.HashSet());");

    Map vars = new HashMap();
    executeExpression(s, vars);

    assertEquals(false, ((Collection) vars.get("a")).contains(2));
    assertEquals(2, ((Collection) vars.get("a")).size());

    assertEquals(false, ((Collection) vars.get("b")).contains(2));
    assertEquals(2, ((Collection) vars.get("b")).size());
  }

  @Test
  @KnownCompatibility("function-declaration")
  public void testMVEL225() {
    Serializable compileExpression = compileExpression(
        "def f() { int a=1;a++;return a; }; f();");
    // MapVariableResolverFactory is replaced by a plain Map of input variables.
    assertEquals(2, executeExpression(compileExpression, new HashMap<String, Object>()));
  }

  @Test
  @KnownCompatibility("function-declaration")
  public void testAnonymousFunctionDecl() {
    assertEquals(3,
        test("anonFunc = function (a,b) { return a + b; }; anonFunc(1,2)"));
  }

  // Category not isolated: the parse error is reported at the function declaration. The expression also uses
  // single-quoted strings (single-quote-string); that was not tested separately.
  @Test
  @KnownCompatibility("function-declaration")
  public void testFunctionSemantics() {
    assertEquals(true,
        test("function fooFunction(a) { return a; }; x__0 = ''; 'boob' == fooFunction(x__0 = 'boob') " +
            "&& x__0 == 'boob';"));
  }
}
