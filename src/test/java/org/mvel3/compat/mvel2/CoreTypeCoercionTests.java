package org.mvel3.compat.mvel2;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mvel3.compat.mvel2.res.Bar;
import org.mvel3.compat.mvel2.res.Foo;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.ExpressionCompiler;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileSetExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.eval;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeSetExpression;

/**
 * Imported from the type-coercion and conversion tests of org.mvel2.tests.core.CoreConfidenceTests (mvel2 branch):
 * type casting, primitive/boxed number conversions, BigInteger/BigDecimal arithmetic, array creation and type declarations.
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged where MVEL3 has an equivalent. Failures are intentionally visible;
 * known behavioural differences are annotated with {@link KnownCompatibility}.
 *
 * <p>Not imported, because they use MVEL2-only compiler analysis or internal APIs:
 * {@code testTypeRegression} ({@code getVarOrInputType}); {@code testStrictTypingCompilation}
 * (strict type enforcement with invalid properties); {@code testBooleanModeOnly2} ({@code DataConversion.convert});
 * {@code testUntypedClone}, {@code testAnalyzeMathAbs}, {@code testGenericsWithOr}, {@code testAnalyzeTernary},
 * {@code testAnalyzeWrongTernary}, {@code testUppercaseField}, {@code testExpressionReturnType},
 * {@code testConstantOnLeftExpression}, {@code testExpressionReturnTypeWithGenerics}, {@code testModuloReturnType},
 * {@code testWrongExpressions} ({@code MVEL.analyze}); {@code testLooseTypeConversion}
 * ({@code MVEL.eval(expr, Class)}).
 *
 * <p>Adaptations: {@code ParserContext}/{@code ParserConfiguration} become {@link TestCompilerContext};
 * optimizer selection and strong typing flags are removed; {@code compileAndExecuteWithStrongTyping} is replaced
 * by {@code eval} on an empty variable map.
 */
@Tag("mvel2-compatibility")
public class CoreTypeCoercionTests extends AbstractTest {

  protected static Object runSingleTest(final String ex) {
    return eval(ex, new HashMap<>());
  }

  protected static Object compileAndExecuteWithStrongTyping(final String ex) {
    return eval(ex, new HashMap<>());
  }

  public static class TestClass2 {
    public void addEqualAuthorizationConstraint(Foo foo, Bar bar, Integer i) {
    }
  }

  public static class B {
    private int value;

    public B() {
    }

    public B(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }

    @Override
    public boolean equals(Object other) {
      return other != null && other instanceof B && value == ((B) other).value;
    }

    @Override
    public int hashCode() {
      return value;
    }
  }

  @Test
  public void testTestIntToLong() {
    String s = "1+(long)a";

    TestCompilerContext pc = new TestCompilerContext();
    pc.addInput("a", Integer.class);

    ExpressionCompiler compiler = new ExpressionCompiler(s, pc);
    Serializable expr = compiler.compile();

    Map vars = new HashMap();
    vars.put("a", 1);

    Object r = executeExpression(expr, vars);
    assertEquals(new Long(2), r);
  }

  @Test
  @KnownCompatibility("string-coercion")
  public void testTypeCast() {
    assertEquals("10",
        test("(String) 10"));
  }

  // Category not isolated: block-syntax and single-quote string literal are also present.
  @Test
  @KnownCompatibility("block-syntax")
  public void testTypeCast2() {
    String ex = "map = new java.util.HashMap(); map.put('doggie', new java.util.ArrayList());" +
        " ((java.util.ArrayList) map['doggie']).size()";

    Map map = createTestMap();

    assertEquals(0, eval(ex, map));

    assertEquals(0,
        test(ex));
  }

  @Test
  @KnownCompatibility("void-method-expression")
  public void testJIRA93() {
    Map testMap = createTestMap();
    testMap.put("testClass2",
        new TestClass2());

    Serializable s = compileExpression("testClass2.addEqualAuthorizationConstraint(foo, foo.bar, 5)");

    executeExpression(s,
        testMap);
  }

  @Test
  @KnownCompatibility("single-quote-string")
  public void testStringToArrayCast() {
    Object o = test("(char[]) 'abcd'");

    assertTrue(o instanceof char[]);
  }

  // Category not isolated: single-quote string, cast in assignment, and block-syntax are present.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testStringToArrayCast2() {
    assertTrue((Boolean) test("_xyxy = (char[]) 'abcd'; _xyxy[0] == 'a'"));
  }

  @Test
  @KnownCompatibility("block-syntax")
  public void testStaticallyTypedArrayVar() {
    String ex = "char[] _c___ = new char[10]; _c___ instanceof char[]";
    assertTrue((Boolean) test(ex));
  }

  @Test
  public void testJIRA100() {
    assertEquals(new BigDecimal(20),
        test("java.math.BigDecimal axx = new java.math.BigDecimal( 10.0 ); java.math.BigDecimal bxx = " +
            "new java.math.BigDecimal( 10.0 ); java.math.BigDecimal cxx = axx + bxx; return cxx; "));
  }

  @Test
  public void testJIRA100b() {
    Serializable s = compileExpression("java.math.BigDecimal axx = new java.math.BigDecimal( 10.0 ); java.math.BigDecimal bxx = " +
        "new java.math.BigDecimal( 10.0 ); java.math.BigDecimal cxx = axx + bxx; return cxx; ");

    assertEquals(new BigDecimal(20), executeExpression(s, new HashMap()));
  }

  @Test
  public void testStaticallyTypedLong() {
    assertEquals(10l,
        test("10l"));
  }

  @Test
  @KnownCompatibility("block-syntax")
  public void testDecl() {
    assertEquals((char) 100,
        test("char chr; chr = 100; chr"));
  }

  @Test
  @KnownCompatibility("inline-import")
  public void testBigDecimalOutput() {
    String str = "import java.math.BigDecimal; BigDecimal test = new BigDecimal(\"50000\"); System.out.println(test / new BigDecimal(\"1.13\"));";
    TestCompilerContext pctx = new TestCompilerContext();
    Serializable stmt = compileExpression(str, pctx);
    executeExpression(stmt, new HashMap());
  }

  @Test
  @KnownCompatibility("set-expression-api")
  public void testBindingNullToPrimitiveTypes() {
    Map<String, Object> vars = createTestMap();
    ((Foo) vars.get("foo")).setCountTest(10);

    Serializable s = compileSetExpression("foo.countTest");
    executeSetExpression(s, vars, null);

    assertEquals(((Foo) vars.get("foo")).getCountTest(), 0);

    s = compileSetExpression("foo.countTest");
    executeSetExpression(s, vars, null);

    assertEquals(((Foo) vars.get("foo")).getCountTest(), 0);

    executeSetExpression(s, vars, null);

    assertEquals(((Foo) vars.get("foo")).getCountTest(), 0);
  }

  @Test
  @KnownCompatibility("set-expression-api")
  public void testBindingNullToPrimitiveTypes2() {
    Map<String, Object> vars = createTestMap();
    ((Foo) vars.get("foo")).setCountTest(10);

    Serializable s = compileSetExpression("foo.boolTest");
    executeSetExpression(s, vars, null);

    assertFalse(((Foo) vars.get("foo")).isBoolTest());

    s = compileSetExpression("foo.boolTest");
    executeSetExpression(s, vars, null);

    assertFalse(((Foo) vars.get("foo")).isBoolTest());

    executeSetExpression(s, vars, null);

    assertFalse(((Foo) vars.get("foo")).isBoolTest());
  }

  @Test
  @KnownCompatibility("set-expression-api")
  public void testBindingNullToPrimitiveTypes3() {
    Map<String, Object> vars = createTestMap();
    ((Foo) vars.get("foo")).setCharTest('a');

    Serializable s = compileSetExpression("foo.charTest");
    executeSetExpression(s, vars, null);

    assertEquals(((Foo) vars.get("foo")).getCharTest(), 0);

    s = compileSetExpression("foo.charTest");
    executeSetExpression(s, vars, null);

    assertEquals(((Foo) vars.get("foo")).getCharTest(), 0);

    executeSetExpression(s, vars, null);

    assertEquals(((Foo) vars.get("foo")).getCharTest(), 0);
  }

  @Test
  @KnownCompatibility("set-expression-api")
  public void testBindingNullToPrimitiveTypes4() {
    Map<String, Object> vars = createTestMap();
    ((Foo) vars.get("foo")).charTestFld = 'a';

    Serializable s = compileSetExpression("foo.charTestFld");
    executeSetExpression(s, vars, null);

    assertEquals(((Foo) vars.get("foo")).charTestFld, 0);

    s = compileSetExpression("foo.charTestFld");
    executeSetExpression(s, vars, null);

    assertEquals(((Foo) vars.get("foo")).charTestFld, 0);

    executeSetExpression(s, vars, null);

    assertEquals(((Foo) vars.get("foo")).charTestFld, 0);
  }

  @Test
  @KnownCompatibility("single-quote-string")
  public void testNarrowToWideCompare() {
    Serializable s = compileExpression("new String('foo') == new Object()",
        TestCompilerContext.create());

    assertFalse((Boolean) executeExpression(s));
  }

  @Test
  @KnownCompatibility("contains-operator")
  public void testPrimitiveArray() {
    Map vars = new HashMap();
    vars.put("array", new boolean[]{true, false});
    String expression = "a = true; array contains a";
    Serializable compiled = compileExpression(expression);
    boolean result = (Boolean) executeExpression(compiled, vars);
    assertEquals(true, result);

    vars = new HashMap();
    vars.put("array", new int[]{1, 3, 4});
    expression = "a = 2; array contains a";
    compiled = compileExpression(expression);
    result = (Boolean) executeExpression(compiled, vars);
    assertEquals(false, result);

    expression = "a = 3; array contains a";
    compiled = compileExpression(expression);
    result = (Boolean) executeExpression(compiled, vars);
    assertEquals(true, result);

    expression = "a = false; array contains a";
    compiled = compileExpression(expression);
    result = (Boolean) executeExpression(compiled, vars);
    assertEquals(false, result);
  }

  @Test
  @KnownCompatibility("contains-operator")
  public void testPrimitiveArrayWithStrongTyping() {
    String expression = "a = true; new boolean[] { true, false } contains a";
    boolean result = (Boolean) compileAndExecuteWithStrongTyping(expression);
    assertEquals(true, result);

    expression = "a = 2; new int[] { 1, 3 } contains a";
    result = (Boolean) compileAndExecuteWithStrongTyping(expression);
    assertEquals(false, result);

    expression = "a = true; array = new boolean[] { true, false }; array contains a";
    result = (Boolean) compileAndExecuteWithStrongTyping(expression);
    assertEquals(true, result);
  }

  @Test
  @KnownCompatibility("inline-array-literal")
  public void testArrayCreation() {
    assertTrue(Arrays.deepEquals(new Object[0], (Object[]) compileAndExecuteWithStrongTyping("{}")));
    assertTrue(Arrays.deepEquals(new String[0], (String[]) compileAndExecuteWithStrongTyping("new String[] {}")));
    assertTrue(Arrays.deepEquals(new String[]{"xyz"}, (String[]) compileAndExecuteWithStrongTyping("new String[] { \"xyz\" }")));
    assertTrue(Arrays.deepEquals(new String[]{"xyz"}, (String[]) compileAndExecuteWithStrongTyping("new String[] { new String(\"xyz\") }")));
    assertTrue(Arrays.deepEquals(new String[]{"xyz", "abc"}, (String[]) compileAndExecuteWithStrongTyping("new String[] { new String(\"xyz\"), new String(\"abc\") }")));
    assertTrue(Arrays.deepEquals(new B[0], (B[]) compileAndExecuteWithStrongTyping("import org.mvel3.compat.mvel2.CoreTypeCoercionTests.B;\nnew B[] { }")));
    assertTrue(Arrays.deepEquals(new B[]{new B(5)}, (B[]) compileAndExecuteWithStrongTyping("import org.mvel3.compat.mvel2.CoreTypeCoercionTests.B;\nnew B[] { new B(5) }")));
    assertTrue(Arrays.deepEquals(new B[]{new B(), new B(), new B()}, (B[]) compileAndExecuteWithStrongTyping("import org.mvel3.compat.mvel2.CoreTypeCoercionTests.B;\nnew B[] {new B(),new B(),new B()}")));
  }

  @Test
  public void testExponentialNotation() {
    assertEquals(110.0, runSingleTest("10 + 1.0E+2"));
    assertEquals(90.0, runSingleTest("1.0E+2 - 10"));
    assertEquals(10.01, runSingleTest("10 + 1.0E-2"));
  }

  @Test
  public void testPrimitiveNumberCoercion() {
    final TestCompilerContext parserContext = new TestCompilerContext();
    parserContext.addInput("a", int.class);
    parserContext.addInput("b", double.class);
    Serializable compiled = compileExpression("a > b", parserContext);
    Map<String, Object> vars = new HashMap<>();
    vars.put("a", 1);
    vars.put("b", 2.0);
    assertEquals(false, executeExpression(compiled, vars));
  }

  @Test
  @KnownCompatibility("class-literal")
  public void testClassLiteral() {
    final TestCompilerContext parserContext = new TestCompilerContext();
    parserContext.addInput("a", Class.class);
    Serializable compiled = compileExpression("a == String", parserContext);
    Map<String, Object> vars = new HashMap<>();
    vars.put("a", String.class);
    assertEquals(true, executeExpression(compiled, vars));
  }

  @Test
  @KnownCompatibility("missing-auto-import")
  public void testBigIntegerWithZeroValue() {
    final TestCompilerContext parserContext = new TestCompilerContext();
    parserContext.addInput("a", BigInteger.class);
    Serializable expression = compileExpression("a == 0I", parserContext);
    boolean result = (Boolean) executeExpression(expression, new HashMap() {{
      put("a", new BigInteger("0"));
    }});
    assertTrue(result);
  }

  @Test
  @KnownCompatibility("missing-auto-import")
  public void testBigDecimalWithZeroValue() {
    final TestCompilerContext parserContext = new TestCompilerContext();
    parserContext.addInput("a", BigDecimal.class);
    Serializable expression = compileExpression("a == 0B", parserContext);
    boolean result = (Boolean) executeExpression(expression, new HashMap() {{
      put("a", new BigDecimal("0.0"));
    }});
    assertTrue(result);
  }

  @Test
  @KnownCompatibility("missing-auto-import")
  public void testDivisionType() {
    final TestCompilerContext parserContext = new TestCompilerContext();
    parserContext.addInput("a", BigDecimal.class);
    Serializable expression = compileExpression("(a / 3).setScale(2, java.math.BigDecimal.ROUND_HALF_UP)", parserContext);
    Object result = executeExpression(expression, new HashMap() {{
      put("a", new BigDecimal("3.0"));
    }});
    System.out.println(result);
  }

  @Test
  public void testPrimitiveNumberCoercionDuringDivisionShouldWorkOnBothSide() {
    final TestCompilerContext parserContext = new TestCompilerContext();
    parserContext.addInput("a", int.class);
    parserContext.addInput("b", int.class);

    Serializable constantDoubleLeft = compileExpression("0.99 >= a / b", parserContext);
    Object resultLeft = executeExpression(constantDoubleLeft, new HashMap() {{
      put("a", 1);
      put("b", 2);
    }});
    assertEquals(true, resultLeft);

    Serializable constantDoubleRight = compileExpression("a / b < 0.99", parserContext);
    Object resultRight = executeExpression(constantDoubleRight, new HashMap() {{
      put("a", 1);
      put("b", 2);
    }});
    assertEquals(true, resultRight);

    TestCompilerContext parserContext2 = new TestCompilerContext();
    parserContext2.addInput("c", double.class);
    parserContext2.addInput("d", double.class);
    Serializable constantIntRight = compileExpression("c / d > 0", parserContext2);
    Object resultRightInt = executeExpression(constantIntRight, new HashMap() {{
      put("c", 1.0);
      put("d", 2.0);
    }});
    assertEquals(true, resultRightInt);
  }

  // Category not isolated: Math.round(BigDecimal) argument type resolution requires runtime coercion from BigDecimal to double/float.
  @Test
  @KnownCompatibility("runtime-type-method-resolution")
  public void testNumberCoercion() {
    final TestCompilerContext parserContext = new TestCompilerContext();
    parserContext.addInput("a", int.class);

    // Long / Integer to Long / Long
    Serializable longDivByInt = compileExpression("15 * Math.round( new java.math.BigDecimal(\"49.4\") ) / 100.0", parserContext);
    Object resultLongDivByInt = executeExpression(longDivByInt, new HashMap());
    assertEquals(7.35, resultLongDivByInt);

    // Don't convert BigDecimal to int
    Serializable intDivByBigDecimal = compileExpression("a / new java.math.BigDecimal(\"0.5\")", parserContext);
    Object resultIntDivByBigDecimal = executeExpression(intDivByBigDecimal, new HashMap() {{
        put("a", 10);
    }});
    assertEquals(20, ((BigDecimal) resultIntDivByBigDecimal).intValue());

    // Don't convert Double to int
    Serializable intDivByDouble = compileExpression("a / 0.5", parserContext);
    Object resultIntDivByDouble = executeExpression(intDivByDouble, new HashMap() {{
        put("a", 10);
    }});
    assertEquals(20, ((Double) resultIntDivByDouble).intValue());
  }

  // Category not isolated: regex operator ~= is used and root 'this' context variable bar is referenced.
  @Test
  @KnownCompatibility("regex-operator")
  public void testRegExWithCast() {
    final TestCompilerContext parserContext = new TestCompilerContext();
    parserContext.addInput("this", Foo.class);
    Serializable compiled = compileExpression("(String)bar.name ~= '[a-z].+'", parserContext);
    assertEquals(true, executeExpression(compiled, new Foo()));
  }

  @Test
  public void testMultiplyIntByDouble() {
    TestCompilerContext pctx = new TestCompilerContext();
    pctx.addInput("i", Integer.class);
    pctx.addInput("d", Double.class);
    Map vars = new HashMap() {{ put("i", 10); put("d", 0.3); }};
    assertEquals(3.0, executeExpression(compileExpression("i*d", pctx), vars));
    assertEquals(3.0, executeExpression(compileExpression("i*0.3", pctx), vars));
  }

  @Test
  public void testCharToStringCoercionForComparison() {
    TestCompilerContext pctx = new TestCompilerContext();
    pctx.addInput("ch", Character.class);
    Map vars = new HashMap() {{ put("ch", 'a'); }};
    assertEquals(true, executeExpression(compileExpression("ch == \"a\"", pctx), vars));
    assertEquals(false, executeExpression(compileExpression("ch == \"b\"", pctx), vars));
  }

  @Test
  public void testNullBigDecimal() {
    analyzeBigDecimalOperation("a + b", new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3"));
    analyzeBigDecimalOperation("a + b", new BigDecimal("1"), null, null);
    analyzeBigDecimalOperation("a + b", null, new BigDecimal("2"), null);
    analyzeBigDecimalOperation("a + b", null, null, null);

    analyzeBigDecimalOperation("a - b", new BigDecimal("5"), new BigDecimal("3"), new BigDecimal("2"));
    analyzeBigDecimalOperation("a - b", new BigDecimal("5"), null, null);
    analyzeBigDecimalOperation("a - b", null, new BigDecimal("3"), null);
    analyzeBigDecimalOperation("a - b", null, null, null);

    analyzeBigDecimalOperation("a * b", new BigDecimal("2"), new BigDecimal("3"), new BigDecimal("6"));
    analyzeBigDecimalOperation("a * b", new BigDecimal("2"), null, null);
    analyzeBigDecimalOperation("a * b", null, new BigDecimal("3"), null);
    analyzeBigDecimalOperation("a * b", null, null, null);

    analyzeBigDecimalOperation("a / b", new BigDecimal("6"), new BigDecimal("3"), new BigDecimal("2"));
    analyzeBigDecimalOperation("a / b", new BigDecimal("6"), null, null);
    analyzeBigDecimalOperation("a / b", null, new BigDecimal("3"), null);
    analyzeBigDecimalOperation("a / b", null, null, null);
  }

  private void analyzeBigDecimalOperation(String expression, BigDecimal a, BigDecimal b, BigDecimal expected) {
    TestCompilerContext pctx = new TestCompilerContext();
    pctx.addInput("a", BigDecimal.class);
    pctx.addInput("b", BigDecimal.class);

    Serializable compiledExpr = compileExpression(expression, pctx);

    Map<String, Object> factory = new HashMap<>();
    factory.put("a", a);
    factory.put("b", b);

    if (a == null || b == null) {
      try {
        executeExpression(compiledExpr, factory);
        fail("should throw a NPE");
      } catch (NullPointerException npe) {
        // expected
      }
    } else {
      Object result = executeExpression(compiledExpr, factory);
      assertEquals(expected, result);
    }
  }
}
