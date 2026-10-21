package org.mvel3.compat.mvel2;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mvel3.compat.mvel2.res.Column;
import org.mvel3.compat.mvel2.res.PojoStatic;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.ExpressionCompiler;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.eval;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.evalToBoolean;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;

/**
 * Imported from the operators and expressions tests of org.mvel2.tests.core.CoreConfidenceTests (mvel2 branch):
 * arithmetic precedence, unary operators, logical/boolean operators, empty/is/contains operators,
 * and shift operators.
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged where MVEL3 has an equivalent. Failures are intentionally visible;
 * known behavioural differences are annotated with {@link KnownCompatibility}.
 *
 * <p>Not imported, because they use MVEL2-only compiler analysis or internal APIs:
 * {@code testEvaluationRegression}, {@code testIncrementAndAssignWithInputs}
 * (asserts on {@code ExpressionCompiler.getParserContextState().getInputs()});
 * {@code testRandomExpression4}, {@code testRandomExpression5}, {@code testRandomExpression6}
 * (call {@code org.mvel2.MVEL.*} dynamically inside expression strings);
 * {@code testStrDoubleEqualsEquals} (commented out in MVEL2).
 *
 * <p>Adaptations: {@code ParserContext}/{@code ParserConfiguration} become {@link TestCompilerContext};
 * optimizer selection and strong typing flags are removed.
 */
@Tag("mvel2-compatibility")
public class CoreOperatorsTests extends AbstractTest {

  protected TestCompilerContext context = new TestCompilerContext();

  protected static Object runSingleTest(final String ex) {
    return eval(ex, new HashMap<>());
  }

  public static class BeanB {
    private boolean successful;

    public BeanB(boolean successful) {
      this.successful = successful;
    }

    public boolean isSuccessful() {
      return successful;
    }

    public boolean isFailed() {
      return !successful;
    }
  }

  public static class Triangle {
    public static enum Foo {
      INCOMPLETE, UNCLASSIFIED,
      EQUILATERAL, ISOSCELES, RECTANGLED, ISOSCELES_RECTANGLED, ACUTE, OBTUSE;
    }

    private List<Map<String, Object>> deliveries = new ArrayList<Map<String, Object>>();

    public List<Map<String, Object>> getDeliveries() {
      return deliveries;
    }

    private Object objLabel = "Triangle";
    private String strLabel = "Triangle";
    private Double doubleVal = 29.0;

    public Object getObjLabel() {
      return objLabel;
    }

    public void setObjLabel(Object objLabel) {
      this.objLabel = objLabel;
    }

    public String getStrLabel() {
      return strLabel;
    }

    public void setStrLabel(String strLabel) {
      this.strLabel = strLabel;
    }

    public Double getDoubleVal() {
      return doubleVal;
    }

    public void setDoubleVal(Double doubleVal) {
      this.doubleVal = doubleVal;
    }
  }

  @Test
  public void testBooleanModeOnly4() {
    assertEquals(true, test("hour == (hour + 0)"));
  }

  @Test
  public void testEvalToBoolean() {
    assertEquals(true,
        (boolean) evalToBoolean("true ",
            "true"));
    assertEquals(true,
        (boolean) evalToBoolean("true ",
            "true"));
  }

  @Test
  public void testSubtractNoSpace1() {
    assertEquals(59,
        test("hour-1"));
  }

  @Test
  public void testEqualityRegression() {
    ExpressionCompiler compiler = new ExpressionCompiler("price == (new Integer( 5 ) + 5 ) ");
    compiler.compile();
  }

  @Test
  public void testAssignmentRegression() {
    ExpressionCompiler compiler = new ExpressionCompiler("total = total + $cheese.price");
    compiler.compile();
  }

  @Test
  public void testPrecedenceOrder() {
    assertTrue((Boolean) test("5 > 6 && 2 < 1 || 10 > 9"));
  }

  // Category not isolated: block syntax missing semicolon and return statement.
  @Test
  @KnownCompatibility("block-syntax")
  public void testRandomExpression1() {
    assertEquals("HelloWorld",
        test("if ((x15 = foo.bar) == foo.bar && x15 == foo.bar) { return 'HelloWorld'; } " +
            "else { return 'GoodbyeWorld' } "));
  }

  // Category not isolated: assignment expression causes JavaParser null node NPE in transpiler.
  @Test
  @KnownCompatibility("javaparser-null-node")
  public void testRandomExpression9() {
    assertEquals(false,
        test("_abc = 'someString'.hashCode(); _xyz = _abc + 1; _abc == _xyz"));
  }

  // Category not isolated: nested assignment expression causes JavaParser null node NPE in transpiler.
  @Test
  @KnownCompatibility("javaparser-null-node")
  public void testRandomExpression10() {
    assertEquals(false,
        test("(_abc = (_xyz = 'someString'.hashCode()) + 1); _abc == _xyz"));
  }

  @Test
  public void testBooleanEvaluation() {
    assertEquals(true,
        test("true||false||false"));
  }

  // Category not isolated: equalityCheck is a method on the root object Base.
  @Test
  @KnownCompatibility("this-reference")
  public void testBooleanEvaluation2() {
    assertEquals(true,
        test("equalityCheck(1,1)||fun||ackbar"));
  }

  // Category not isolated: value is a property of root object PojoStatic, undeclared in compiler context.
  @Test
  @KnownCompatibility("undeclared-input-variable")
  public void testSimpleExpression() {
    PojoStatic pojo = new PojoStatic("10");
    eval("value!= null",
        pojo,
        new HashMap());
  }

  // Category not isolated: post-increment hour++ transpiled as pre-increment ++hour.
  @Test
  @KnownCompatibility("post-increment-expression")
  public void testIncrementInBooleanStatement() {
    assertEquals(true,
        test("hour++ < 61 && hour == 61"));
  }

  @Test
  public void testIncrementInBooleanStatement2() {
    assertEquals(true,
        test("++hour == 61"));
  }

  // Category not isolated: 'is' operator and block-syntax without semicolons.
  @Test
  @KnownCompatibility("is-operator")
  public void testIsUsedInIf() {
    assertEquals(true,
        test("c = 'str'; if (c is String) { true; } else { false; } "));
  }

  @Test
  public void testOperatorPrecedenceOrder() {
    Serializable compiled =
        compileExpression("bean1.successful && bean2.failed || bean1.failed && bean2.successful");
    Map context = new HashMap();

    BeanB bean1 = new BeanB(true);
    BeanB bean2 = new BeanB(false);

    context.put("bean1",
        bean1);
    context.put("bean2",
        bean2);

    assertEquals(bean1.isSuccessful() && bean2.isFailed() || bean1.isFailed() && bean2.isSuccessful(),
        (boolean) executeExpression(compiled,
            context,
            Boolean.class));
  }

  // Category not isolated: 'empty' operator and block syntax without semicolons.
  @Test
  @KnownCompatibility("empty-operator")
  public void testEmpty() {
    TestCompilerContext ctx = new TestCompilerContext();

    Serializable s = compileExpression("list = new java.util.ArrayList(); list == empty",
        ctx);

    Map vars = new HashMap();

    Boolean x = (Boolean) executeExpression(s,
        vars);

    assertNotNull(x);
    assertTrue(x.booleanValue());
  }

  // Category not isolated: missing semicolon before block end.
  @Test
  @KnownCompatibility("block-syntax")
  public void testReturnBoolean() {
    String ex = "list = new java.util.ArrayList(); return list != null";

    TestCompilerContext ctx = new TestCompilerContext();
    Serializable s = compileExpression(ex,
        ctx);

    assertEquals(true,
        executeExpression(s,
            new HashMap()));
  }

  @Test
  public void testUnaryOpNegation1() {
    assertEquals(false,
        test("!new Boolean(true)"));
  }

  // Category not isolated: isdef operator.
  @Test
  @KnownCompatibility("isdef-operator")
  public void testUnaryOpNegation2() {
    assertEquals(true,
        test("!isdef _foozy_"));
  }

  @Test
  public void testJIRA153() {
    assertEquals(false,
        eval("!(true)"));
    assertEquals(false,
        executeExpression(compileExpression("!(true)")));
  }

  @Test
  public void testMultipleNegations() {
    assertEquals(true,
        executeExpression(compileExpression("!false")));
    assertEquals(false,
        executeExpression(compileExpression("!!false")));
    assertEquals(true,
        executeExpression(compileExpression("!!!false")));
    assertEquals(false,
        executeExpression(compileExpression("!!!!false")));
  }

  // Category not isolated: contains operator.
  @Test
  @KnownCompatibility("contains-operator")
  public void testOperatorPrecedence() throws IOException {
    String script = "list = [1, 2, 3]; x = 10; list contains x || x == 20";
    Serializable expression = compileExpression(script);
    Object result = executeExpression(expression, new HashMap());

    assertEquals(Boolean.FALSE, result);
  }

  // Category not isolated: this-reference in strong typing comparison.
  @Test
  @KnownCompatibility("this-reference")
  public void testStrTriangleEqualsEquals() {
    TestCompilerContext pctx = new TestCompilerContext();
    pctx.addInput("this", Triangle.class);

    String str = "this.strLabel == this";

    try {
      Serializable stmt = compileExpression(str, pctx);
      fail("should have failed");
    }
    catch (Exception e) {
      return;
    }
  }

  @Test
  public void testShiftOperator() {
    String expression = "1 << 65536L";
    Serializable compiled = compileExpression(expression, context);
    Object result = executeExpression(compiled, new HashMap());
    assertEquals(1 << 65536L, result);

    expression = "one << shift";
    Map map = new HashMap() {{
      put("one", 1);
      put("shift", 65536L);
    }};
    compiled = compileExpression(expression, context);
    result = executeExpression(compiled, map);
    assertEquals(1 << 65536L, result);
  }

  // Category not isolated: $c is undeclared variable in compiler context for POJO root.
  @Test
  @KnownCompatibility("undeclared-input-variable")
  public void testMinusOperatorWithoutSpace() {
    String str = "length == $c.length -1";

    TestCompilerContext pctx = new TestCompilerContext();
    Serializable stmt = compileExpression(str, pctx);
    Column col1 = new Column("x", 0);
    Column col2 = new Column("x", 0);
    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("$c", col2);
    Boolean result = (Boolean) executeExpression(stmt, col1, vars);
    assertFalse(result);
  }

  @Test
  public void testUnaryNegative() {
    TestCompilerContext pctx = new TestCompilerContext();
    pctx.addInput("value", int.class);
    Map vars = new HashMap() {{ put("value", 42); }};
    assertEquals(-42, executeExpression(compileExpression("-value", pctx), vars));
  }

  @Test
  public void testUnaryNegativeWithSpace() {
    TestCompilerContext pctx = new TestCompilerContext();
    pctx.addInput("value", int.class);
    Map vars = new HashMap() {{ put("value", 42); }};
    assertEquals(-42, executeExpression(compileExpression("- value", pctx), vars));
  }

  @Test
  @KnownCompatibility("empty-operator")
  public void testEmptyOperatorOnStrings() {
    TestCompilerContext pctx = new TestCompilerContext();

    pctx.addInput("nullString", String.class);
    pctx.addInput("emptyString", String.class);
    pctx.addInput("blankString", String.class);
    pctx.addInput("nonEmptyString", String.class);
    Map vars = new HashMap() {{
        put("nullString", null);
        put("emptyString", "");
        put("blankString", "   ");
        put("nonEmptyString", "abc");
    }};

    assertEquals(true, executeExpression(compileExpression("nullString == empty", pctx), vars));
    assertEquals(true, executeExpression(compileExpression("emptyString == empty", pctx), vars));
    assertEquals(true, executeExpression(compileExpression("blankString == empty", pctx), vars));
    assertEquals(false, executeExpression(compileExpression("nonEmptyString == empty", pctx), vars));
  }

  @Test
  @KnownCompatibility("empty-operator")
  public void testEmptyOperatorOnList() {
    TestCompilerContext pctx = new TestCompilerContext();

    pctx.addInput("list", ArrayList.class);
    Map vars = new HashMap() {{
      put("list", new ArrayList<>());
    }};

    assertEquals(true, executeExpression(compileExpression("list == empty", pctx), vars));
  }

  @Test
  @KnownCompatibility("empty-operator")
  public void testEmptyOperatorOnMap() {
    TestCompilerContext pctx = new TestCompilerContext();

    pctx.addInput("map", HashMap.class);
    Map vars = new HashMap() {{
      put("map", new HashMap<>());
    }};

    assertEquals(true, executeExpression(compileExpression("map == empty", pctx), vars));
  }

  @Test
  @KnownCompatibility("empty-operator")
  public void testEmptyOperatorOnBoolean() {
    TestCompilerContext pctx = new TestCompilerContext();

    pctx.addInput("bNull", Boolean.class);
    pctx.addInput("bTrue", Boolean.class);
    pctx.addInput("bFalse", Boolean.class);
    Map vars = new HashMap() {{
        put("bNull", null);
        put("bTrue", true);
        put("bFalse", false);
    }};

    assertEquals(true, executeExpression(compileExpression("bNull == empty", pctx), vars));
    assertEquals(false, executeExpression(compileExpression("bTrue == empty", pctx), vars));
    assertEquals(true, executeExpression(compileExpression("bFalse == empty", pctx), vars));
  }

  @Test
  @KnownCompatibility("empty-operator")
  public void testEmptyOperatorOnInteger() {
    TestCompilerContext pctx = new TestCompilerContext();

    pctx.addInput("nullInt", Integer.class);
    pctx.addInput("zero", Integer.class);
    pctx.addInput("nonZero", Integer.class);
    Map vars = new HashMap() {{
        put("nullInt", null);
        put("zero", 0);
        put("nonZero", 42);
    }};

    assertEquals(true, executeExpression(compileExpression("nullInt == empty", pctx), vars));
    assertEquals(true, executeExpression(compileExpression("zero == empty", pctx), vars));
    assertEquals(false, executeExpression(compileExpression("nonZero == empty", pctx), vars));
  }

  @Test
  public void testUseVariableFactoryWithArithmeticOperation() {
    checkOperation("3 + 4 * i.get()", 43);
    checkOperation("2 * 3 + 4 * i.get()", 46);
    checkOperation("1 + 2 * 3 + i.get()", 17);
    checkOperation("2 * 3 + 4 * i.get()", 46);
    checkOperation("1 + 2 * 3 + 4 * i.get()", 47);
    checkOperation("1 + 2 * 3 + i.get() * 4", 47);
    checkOperation("1 + 2 * 3 + i.get() + 4", 21);
    checkOperation("4 * i.get() + 5", 45);
    checkOperation("3 + 4 * i.get() + 5", 48);
    checkOperation("2 * 3 + 4 * i.get() + 5", 51);
    checkOperation("1 + 2 * 3 + 4 * i.get() + 5", 52);
    checkOperation("1 + 2 * 3 + 4 * i.get() * 5", 207);
    checkOperation("i.get() + 1 + 2 * 3 + 4 * i.get()", 57);
  }

  private void checkOperation(String expression, int expectedResult) {
    AtomicInteger i = new AtomicInteger( 10 );
    Map<String, Object> vars = new HashMap<>();
    vars.put("i", i);

    TestCompilerContext pctx = new TestCompilerContext();
    pctx.addInput("i", AtomicInteger.class);

    Serializable compiledExpr = compileExpression(expression, pctx);
    int result = (Integer) executeExpression(compiledExpr, vars);
    assertEquals(expectedResult, result);
  }
}
